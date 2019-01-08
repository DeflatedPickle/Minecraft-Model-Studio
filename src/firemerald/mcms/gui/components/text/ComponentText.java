package firemerald.mcms.gui.components.text;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;

import firemerald.mcms.Main;
import firemerald.mcms.gui.components.Component;
import firemerald.mcms.model.Mesh;
import firemerald.mcms.shader.Shader;
import firemerald.mcms.texture.Color;
import firemerald.mcms.theme.BoxFormat;
import firemerald.mcms.util.ClipboardUtil;
import firemerald.mcms.util.Cursors;
import firemerald.mcms.util.FontRenderer;
import firemerald.mcms.util.TextureManager;

public class ComponentText extends Component
{
	protected String text = "";
	public int pos = 0, clickPos = 0;
	public int selStart = 0, selEnd = 0;
	public FontRenderer font;
	protected float cursorPos = 0;
	protected float selX1, selX2;
	public boolean hasChanged;
	public float cursorTime = 0;
	public float clickTime = 0;
	public int clickNum = 0;
	private final Consumer<String> onTextChange;
	protected BoxFormat rectangle;
	protected final Mesh mesh;
	
	public ComponentText(float x1, float y1, float x2, float y2, FontRenderer font, String text, Consumer<String> onTextChange)
	{
		this(x1, y1, x2, y2, font, onTextChange);
		setText(text);
	}
	
	public ComponentText(float x1, float y1, float x2, float y2, FontRenderer font, Consumer<String> onTextChange)
	{
		super(x1, y1, x2, y2);
		this.font = font;
		this.onTextChange = onTextChange;
		rectangle = new BoxFormat((int) (x2 - x1), (int) (y2 - y1), 1);
		mesh = new Mesh(x1, y1, x2, y2, 0, 0, 0, 1, 1);
	}
	
	@Override
	public void setSize(float x1, float y1, float x2, float y2)
	{
		super.setSize(x1, y1, x2, y2);
		updatePos2();
		rectangle = new BoxFormat((int) (x2 - x1), (int) (y2 - y1), 1);
		mesh.setMesh(x1, y1, x2, y2, 0, 0, 0, 1, 1);
	}
	
	public void setText(String text)
	{
		this.text = text;
		selStart = selEnd = pos = text.length();
		onTextUpdate();
	}
	
	public String getText()
	{
		return text;
	}
	
	protected void updatePos()
	{
		cursorPos = x1 + 2 + font.getStringWidth(text.substring(0, pos));
		selX1 = x1 + 2 + font.getStringWidth(text.substring(0, selStart));
		selX2 = x1 + 2 + font.getStringWidth(text.substring(0, selEnd));
		cursorTime = 0;
		clickNum = 0;
		clickTime = 0;
	}
	
	protected void updatePos2()
	{
		cursorPos = x1 + 2 + font.getStringWidth(text.substring(0, pos));
		selX1 = x1 + 2 + font.getStringWidth(text.substring(0, selStart));
		selX2 = x1 + 2 + font.getStringWidth(text.substring(0, selEnd));
		cursorTime = 0;
	}

	@Override
	public void onMousePressed(float mx, float my, int button, int mods)
	{
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT)
		{
			int clicked = font.getSelectedPos(text, (int) (mx - (x1 + 2)));
			if (clicked == clickPos && clickTime > 0)
			{
				if (clickNum == 0)
				{
					selStart = getBeforePreviousSymbol(clicked);
					selEnd = pos = getNextSymbol(clicked);
					updatePos2();
					clickNum = 1;
				}
				else if (clickNum == 1)
				{
					selStart = 0;
					selEnd = pos = text.length();
					updatePos2();
					clickNum = 2;
				}
				else
				{
					clickPos = selStart = selEnd = pos = clicked;
					updatePos();
				}
			}
			else
			{
				clickPos = selStart = selEnd = pos = clicked;
				updatePos();
			}
			clickTime = .5f;
		}
	}
	
	public int getBeforePreviousSymbol(int pos)
	{
		if (pos <= 0) return 0;
		else if (pos >= text.length()) return text.length();
		else
		{
			for (int i = pos - 1; i >= 0; i--)
			{
				char c = text.charAt(i);
				if (!(Character.isLetterOrDigit(c) || c == '_')) return i + 1;
			}
			return 0;
		}
	}
	
	public int getNextSymbol(int pos)
	{
		if (pos >= text.length()) return text.length();
		else
		{
			for (int i = pos; i < text.length(); i++)
			{
				char c = text.charAt(i);
				if (!(Character.isLetterOrDigit(c) || c == '_')) return i;
			}
			return text.length();
		}
	}

	@Override
	public void onDrag(float mx, float my)
	{
		if (isTextFocused())
		{
			pos = font.getSelectedPos(text, (int) (mx - (x1 + 2)));
			if (pos == clickPos) selStart = selEnd = pos;
			else if (pos < clickPos)
			{
				selStart = pos;
				selEnd = clickPos;
				clickTime = 0;
			}
			else if (pos > clickPos)
			{
				selStart = clickPos;
				selEnd = pos;
				clickTime = 0;
			}
			updatePos();
		}
	}
	
	public void onTextUpdate()
	{
		if (onTextChange != null) onTextChange.accept(text);
		hasChanged = true;
	}

	@Override
	public void tick(float mx, float my, float deltaTime)
	{
		cursorTime = (cursorTime + deltaTime) % 1;
		if (clickTime > 0 && (clickTime -= deltaTime) < 0) clickTime = 0;
	}

	@Override
	public void render(float mx, float my, boolean canHover)
	{
		Shader s = Main.instance.shader;
		getTheme().bindTextBox(rectangle);
		mesh.render();
		TextureManager texs = Main.instance.textureManager;
		font.drawTextLine(text, x1 + 2, y2 - (5 + font.height), getTextColor(), false, false, false);
		texs.unbindTexture();
		if (selStart != selEnd)
		{
			s.setColor(0, 0, 1, .5f);
			Main.MODMESH.setMesh(selX1, y1 + 2, selX2, y2 - 2, 0, 0, 0, 1, 1);
			Main.MODMESH.render();
		}
		if (this.isTextFocused() && cursorTime < .5f)
		{
			s.setColor(getTheme().getTextColor()); //TODO
			Main.MODMESH.setMesh(cursorPos, y1 + 2, cursorPos + 1, y2 - 2, 0, 0, 0, 1, 1);
			Main.MODMESH.render();
		}
		s.setColor(1, 1, 1, 1);
	}
	
	public Color getTextColor()
	{
		return getTheme().getTextColor();
	}

	@Override
	public void onCharTyped(char chr)
	{
		if (isTextFocused()) if (chr != '\n')
		{
			int pos1, pos2;
			if (selStart != selEnd)
			{
				pos1 = selStart;
				pos2 = selEnd;
				pos = 1 + (selEnd = selStart);
			}
			else
			{
				pos1 = pos2 = pos;
				pos++;
			}
			text = text.substring(0, pos1) + chr + text.substring(pos2);
			selStart = selEnd = pos;
			updatePos();
			onTextUpdate();
		}
	}

	@Override
	public void onKeyPressed(int key, int scancode, int mods)
	{
		if (isTextFocused()) onKey(key, scancode, mods);
	}

	@Override
	public void onKeyReleased(int key, int scancode, int mods) {}

	@Override
	public void onKeyRepeat(int key, int scancode, int mods)
	{
		if (isTextFocused()) onKey(key, scancode, mods);
	}
	
	public void onKey(int key, int scancode, int mods)
	{
		switch (key)
		{
		case GLFW.GLFW_KEY_BACKSPACE:
			onBackspace(mods);
			break;
		case GLFW.GLFW_KEY_DELETE:
			onDelete(mods);
			break;
		case GLFW.GLFW_KEY_LEFT:
			onLeft(mods);
			break;
		case GLFW.GLFW_KEY_RIGHT:
			onRight(mods);
			break;
		case GLFW.GLFW_KEY_HOME:
			onHome(mods);
			break;
		case GLFW.GLFW_KEY_END:
			onEnd(mods);
			break;
		case GLFW.GLFW_KEY_C:
			if ((mods & GLFW.GLFW_MOD_CONTROL) > 0) onCopy();
			break;
		case GLFW.GLFW_KEY_X:
			if ((mods & GLFW.GLFW_MOD_CONTROL) > 0) onCut();
			break;
		case GLFW.GLFW_KEY_V:
			if ((mods & GLFW.GLFW_MOD_CONTROL) > 0) onPaste();
			break;
		}
	}
	
	public void onBackspace(int mods)
	{
		if (selEnd > 0)
		{
			int pos2;
			if (selStart != selEnd)
			{
				pos2 = selEnd;
				pos = selEnd = selStart;
			}
			else
			{
				pos2 = pos;
				pos--;
			}
			text = text.substring(0, pos) + text.substring(pos2);
			selStart = selEnd = pos;
			updatePos();
			onTextUpdate();
		}
	}
	
	public void onDelete(int mods)
	{
		if (selStart < text.length())
		{
			int pos2;
			if (selStart != selEnd)
			{
				pos2 = selEnd;
				pos = selEnd = selStart;
			}
			else
			{
				pos2 = pos + 1;
			}
			text = text.substring(0, pos) + text.substring(pos2);
			selStart = selEnd = pos;
			updatePos();
			onTextUpdate();
		}
	}
	
	/* breaks:
	 * whitespace|non-whitespace
	 * alphanumeric|non-whitespace and non-alphanumeric and not '_'
	 * non-uppercase|uppercase
	 * non-whitespace and non-alphanumeric and not '_'|alphanumeric
	 * '_'|uppercase
	 */
	
	public boolean isBreak(int pos)
	{
		char c1 = text.charAt(pos - 1);
		char c2 = text.charAt(pos);
		return 
				(Character.isWhitespace(c1) && !Character.isWhitespace(c2)) || 
				(!Character.isUpperCase(c1) && Character.isUpperCase(c2)) || 
				(Character.isLetterOrDigit(c1) && !Character.isWhitespace(c2) && !Character.isLetterOrDigit(c2) && c2 != '_') ||
				(!Character.isWhitespace(c1) && !Character.isLetterOrDigit(c1) && c1 != '_' && Character.isLetterOrDigit(c2)) ||
				(c1 == '_' && Character.isUpperCase(c2));
	}
	
	public int getPrevPos(int pos)
	{
		if (pos <= 1) return 0;
		else
		{
			for (int i = pos - 1; i > 0; i--) if (isBreak(i)) return i;
			return 0;
		}
	}
	
	public int getNextPos(int pos)
	{
		if (pos >= text.length() - 1) return text.length();
		else
		{
			for (int i = pos + 1; i < text.length(); i++) if (isBreak(i)) return i;
			return text.length();
		}
	}
	
	public void onLeft(int mods)
	{
		boolean isShift = (mods & GLFW.GLFW_MOD_SHIFT) > 0;
		if (pos > 0)
		{
			int newPos = (mods & GLFW.GLFW_MOD_CONTROL) > 0 ? getPrevPos(pos) : pos - 1;
			if (isShift)
			{
				if (pos <= selStart) selStart = pos = newPos; //<-|   |
				else if (newPos > selStart) selEnd = pos = newPos; //|   <-|
				else //<|-|
				{
					selEnd = selStart;
					selStart = pos = newPos;
				}
			}
			else selStart = selEnd = pos = newPos;
			updatePos();
		}
		else if (!isShift && selStart != selEnd)
		{
			selStart = selEnd = 0;
			updatePos();
		}
	}
	
	public void onRight(int mods)
	{
		boolean isShift = (mods & GLFW.GLFW_MOD_SHIFT) > 0;
		if (pos < text.length())
		{
			int newPos = (mods & GLFW.GLFW_MOD_CONTROL) > 0 ? getNextPos(pos) : pos + 1;
			if (isShift)
			{
				if (pos >= selEnd) selEnd = pos = newPos; //|   |->
				else if (newPos < selEnd) selStart = pos = newPos; //|->   |
				else //|-|>
				{
					selStart = selEnd;
					selEnd = pos = newPos;
				}
			}
			else selStart = selEnd = pos = newPos;
			updatePos();
		}
		else if (!isShift && selStart != selEnd)
		{
			selStart = selEnd = text.length();
			updatePos();
		}
	}
	
	public void onHome(int mods)
	{
		boolean isShift = (mods & GLFW.GLFW_MOD_SHIFT) > 0;
		if (pos > 0)
		{
			if ((mods  & GLFW.GLFW_MOD_SHIFT) > 0)
			{
				if (pos == selEnd) selEnd = selStart;
				selStart = pos = 0;
			}
			else selStart = selEnd = pos = 0;
			updatePos();
		}
		else if (!isShift && selStart != selEnd)
		{
			selStart = selEnd = 0;
			updatePos();
		}
	}
	
	public void onEnd(int mods)
	{
		boolean isShift = (mods & GLFW.GLFW_MOD_SHIFT) > 0;
		if (pos < text.length())
		{
			if ((mods  & GLFW.GLFW_MOD_SHIFT) > 0)
			{
				if (pos == selStart) selStart = selEnd;
				selEnd = pos = text.length();
			}
			else selStart = selEnd = pos = text.length();
			updatePos();
		}
		else if (!isShift && selStart != selEnd)
		{
			selStart = selEnd = text.length();
			updatePos();
		}
	}
	
	public void onCopy()
	{
		if (selStart != selEnd) ClipboardUtil.setString(text.substring(selStart, selEnd));
	}
	
	public void onCut()
	{
		if (selStart != selEnd)
		{
			ClipboardUtil.setString(text.substring(selStart, selEnd));
			text = text.substring(0, selStart) + text.substring(selEnd);
			pos = selEnd = selStart;
			updatePos();
			onTextUpdate();
		}
	}
	
	public void onPaste()
	{
		String str = ClipboardUtil.getString();
		if (str != null)
		{
			int ind = str.indexOf('\n');
			if (ind >= 0) str = str.substring(0, ind);
			if (str.length() != 0)
			{
				int pos1, pos2;
				if (selStart != selEnd)
				{
					pos1 = selStart;
					pos2 = selEnd;
				}
				else pos1 = pos2 = pos;
				text = text.substring(0, pos1) + str + text.substring(pos2);
				selStart = selEnd = pos = pos1 + str.length();
				updatePos();
				onTextUpdate();
			}
		}
	}
	
	@Override
	public void onUnfocus()
	{
		super.onUnfocus();
		selStart = selEnd = pos = 0;
	}
	
	@Override
	public long getCursor(float mx, float my)
	{
		return Cursors.text;
	}
	
	public boolean isTextFocused()
	{
		return focused;
	}
}