package firemerald.mcms.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4d;

import firemerald.mcms.Main;
import firemerald.mcms.api.animation.Transformation;
import firemerald.mcms.api.data.AbstractElement;
import firemerald.mcms.api.math.EulerZYXRotation;
import firemerald.mcms.api.math.IRotation;
import firemerald.mcms.api.math.QuaternionRotation;
import firemerald.mcms.api.model.IRaytraceTarget;
import firemerald.mcms.api.model.IRigged;
import firemerald.mcms.api.model.ObjData;
import firemerald.mcms.api.util.RaytraceResult;
import firemerald.mcms.gui.GuiElementContainer;
import firemerald.mcms.gui.components.ComponentFloatingLabel;
import firemerald.mcms.gui.components.SelectorButton;
import firemerald.mcms.gui.components.text.ComponentIncrementFloat;
import firemerald.mcms.gui.components.text.ComponentText;
import firemerald.mcms.gui.components.text.ComponentTextFloat;
import firemerald.mcms.shader.Shader;
import firemerald.mcms.util.ObjUtil;
import firemerald.mcms.util.mesh.Mesh;

/**
 * @author test
 *
 */
public abstract class ModelComponent implements IRaytraceTarget, IComponentParent //TODO fix offset calculation TODO add registry
{
	public boolean visible = true, childrenVisible = true;
	protected String name;
	public IComponentParent parent;
	public final List<ModelComponent> children = new ArrayList<>();
	protected final Transformation position = new Transformation();
	protected final Vector3f offset = new Vector3f();
	protected float texU = 0, texV = 0;
	protected Matrix4d transformation = new Matrix4d();
	
	public ModelComponent(String name)
	{
		this(null, name);
	}
	
	public ModelComponent(IComponentParent parent, String name)
	{
		this.name = name;
		if ((this.parent = parent) != null) parent.getChildrenComponents().add(this);
	}
	
	public ModelComponent(IComponentParent parent, ModelComponent from)
	{
		this(parent, from.name);
		this.visible = from.visible;
		this.childrenVisible = from.childrenVisible;
		this.position.set(from.position);
		this.offset.set(from.offset);
		this.texU = from.texU;
		this.texV = from.texV;
		this.transformation = new Matrix4d(from.transformation);
	}
	
	public Matrix4d transformation()
	{
		return transformation;
	}
	
	public void updateTransform()
	{
		transformation.identity();
		transformation.mul(position.getTransformation());
		//transformation.translate(offset);
	}
	
	public void updateTransformVals()
	{
		if (position.rotation == IRotation.NONE) position.rotation = new QuaternionRotation();
		position.setFromMatrix(transformation);
	}
	
	public abstract void onTexSizeChange();

	public float posX()
	{
		return position.translation.x();
	}

	public void posX(float posX)
	{
		Main.instance.project.onAction();
		position.translation.x = posX;
		updateTransform();
	}

	public float posY()
	{
		return position.translation.y();
	}

	public void posY(float posY)
	{
		Main.instance.project.onAction();
		position.translation.y = posY;
		updateTransform();
	}

	public float posZ()
	{
		return position.translation.z();
	}

	public void posZ(float posZ)
	{
		Main.instance.project.onAction();
		position.translation.z = posZ;
		updateTransform();
	}

	public float offX()
	{
		return offset.x();
	}

	public void offX(float offX)
	{
		Main.instance.project.onAction();
		offset.x = offX;
		updateTransform();
	}

	public float offY()
	{
		return offset.y();
	}

	public void offY(float offY)
	{
		Main.instance.project.onAction();
		offset.y = offY;
		updateTransform();
	}

	public float offZ()
	{
		return offset.z();
	}

	public void offZ(float offZ)
	{
		Main.instance.project.onAction();
		offset.z = offZ;
		updateTransform();
	}

	public IRotation rotation()
	{
		return position.rotation;
	}

	public void rotation(IRotation rotation)
	{
		Main.instance.project.onAction();
		position.rotation = rotation;
		updateTransform();
	}

	public float texU()
	{
		return texU;
	}

	public void texU(float texU)
	{
		Main.instance.project.onAction();
		this.texU = texU;
	}

	public float texV()
	{
		return texV;
	}

	public void texV(float texV)
	{
		Main.instance.project.onAction();
		this.texV = texV;
	}
	
	public abstract void setTexs();
	
	public void render(Runnable defaultTexture)
	{
		Shader.MODEL.push();
		Shader.MODEL.matrix().mul(transformation);
		Shader.MODEL.push();
		Shader.MODEL.matrix().translate(offset);
		Main.instance.shader.updateModel();
		if (visible) this.doRender(defaultTexture);
		Shader.MODEL.pop();
		Main.instance.shader.updateModel();
		if (childrenVisible) for (ModelComponent c : children) c.render(defaultTexture);
		Shader.MODEL.pop();
		Main.instance.shader.updateModel();
	}
	
	public abstract void doRender(Runnable defaultTexture);
	
	public void cleanUp()
	{
		this.doCleanUp();
		for (ModelComponent c : children) c.cleanUp();
	}
	
	public abstract void doCleanUp();
	
	public void addToObjModel(Matrix4d transformation, ObjData obj, List<int[][]> mesh)
	{
		float scale = Main.instance.project.getScale();
		Matrix4d trans2 = transformation.mul(this.transformation, new Matrix4d());
		Matrix3d norm = trans2.transpose3x3(new Matrix3d()).invert();
		Matrix4d trans = trans2.translate(offset, new Matrix4d());
		float[][][] m = generateMesh();
		for (float[][] fData : m)
		{
			int[][] vInd = new int[fData.length][3];
			for (int i = 0; i < fData.length; i++)
			{
				float[] vData = fData[i];
				vInd[i][0] = ObjUtil.getSetIndex(obj.vertices, to3(trans.transform(new Vector4d(vData[0], vData[1], vData[2], 1))).mul(scale));
				vInd[i][1] = ObjUtil.getSetIndex(obj.textureCoordinates, new Vector2f(vData[3], 1 - vData[4]));
				vInd[i][2] = ObjUtil.getSetIndex(obj.vertexNormals, norm.transform(new Vector3f(vData[5], vData[6], vData[7])));
			}
			mesh.add(vInd);
		}
		for (ModelComponent component : children) component.addToObjModel(trans2, obj, mesh);
	}
	
	public static Vector3f to3(Vector4d vec)
	{
		return new Vector3f((float) vec.x, (float) vec.y, (float) vec.z);
	}
	
	/** float[face][vertex][{x,y,z,u,v,nx,ny,nz}] **/
	public abstract float[][][] generateMesh();
	
	public RaytraceResult raytrace(float fx, float fy, float fz, float dx, float dy, float dz, Matrix4d transformation)
	{
		transformation = transformation.mul(this.transformation, new Matrix4d()).translate(offset);
		RaytraceResult result = visible ? doRaytrace(fx, fy, fz, dx, dy, dz, transformation) : null;
		transformation.translate(offset.negate(new Vector3f()));
		if (childrenVisible) for (ModelComponent child : this.children)
		{
			RaytraceResult res = child.raytrace(fx, fy, fz, dx, dy, dz, transformation);
			if (res != null && (result == null || res.m < result.m)) result = res;
		}
		return result;
	}
	
	protected abstract RaytraceResult doRaytrace(float fx, float fy, float fz, float dx, float dy, float dz, Matrix4d transformation);

	private ComponentFloatingLabel labelName;
	private ComponentText textName;
	private ComponentFloatingLabel labelPos;
	private ComponentTextFloat posXT;
	private ComponentIncrementFloat posXP, posXS;
	private ComponentTextFloat posYT;
	private ComponentIncrementFloat posYP, posYS;
	private ComponentTextFloat posZT;
	private ComponentIncrementFloat posZP, posZS;
	private SelectorButton rotMode;
	private ComponentFloatingLabel labelOff;
	private ComponentTextFloat offXT;
	private ComponentIncrementFloat offXP, offXS;
	private ComponentTextFloat offYT;
	private ComponentIncrementFloat offYP, offYS;
	private ComponentTextFloat offZT;
	private ComponentIncrementFloat offZP, offZS;
	private ComponentFloatingLabel labelTexPos;
	private ComponentTextFloat texOU;
	private ComponentIncrementFloat texOUP, texOUS;
	private ComponentTextFloat texOV;
	private ComponentIncrementFloat texOVP, texOVS;
	
	@Override
	public int onSelect(EditorPanes editorPanes, int editorY)
	{
		final int origY = editorY;
		editorPanes.addBox.setParent(this);
		editorPanes.addMesh.setParent(this);
		editorPanes.copy.setEditable(parent, this);
		editorPanes.remove.setEditable(parent, this);

		GuiElementContainer editor = editorPanes.editor.container;
		int editorX = editorPanes.editor.minX;
		editor.addElement(labelName = new ComponentFloatingLabel( editorX      , editorY, editorX + 300, editorY + 20, Main.instance.fontMsg, "Component name"));
		editorY += 20;
		editor.addElement(textName  = new ComponentText(          editorX      , editorY, editorX + 300, editorY + 20, Main.instance.fontMsg, getName(), this::setName));
		editorY += 20;
		editor.addElement(labelPos  = new ComponentFloatingLabel( editorX      , editorY, editorX + 300, editorY + 20, Main.instance.fontMsg, "Position"));
		editorY += 20;
		editor.addElement(posXT     = new ComponentTextFloat(     editorX      , editorY, editorX + 90 , editorY + 20, Main.instance.fontMsg, posX(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::posX));
		editor.addElement(posXP     = new ComponentIncrementFloat(editorX + 90 , editorY                             , posXT, 1));
		editor.addElement(posXS     = new ComponentIncrementFloat(editorX + 90 , editorY + 10                        , posXT, -1));
		editor.addElement(posYT     = new ComponentTextFloat(     editorX + 100, editorY, editorX + 190, editorY + 20, Main.instance.fontMsg, posY(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::posY));
		editor.addElement(posYP     = new ComponentIncrementFloat(editorX + 190, editorY                             , posYT, 1));
		editor.addElement(posYS     = new ComponentIncrementFloat(editorX + 190, editorY + 10                        , posYT, -1));
		editor.addElement(posZT     = new ComponentTextFloat(     editorX + 200, editorY, editorX + 290, editorY + 20 , Main.instance.fontMsg, posZ(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::posZ));
		editor.addElement(posZP     = new ComponentIncrementFloat(editorX + 290, editorY                              , posZT, 1));
		editor.addElement(posZS     = new ComponentIncrementFloat(editorX + 290, editorY + 10                         , posZT, -1));
		editorY += 20;

		String[] names = new String[] {
				"No rotation",
				"Euler ZYX rotation",
				//"Euler XYZ rotation",
				"Quaternion rotation"
		};
		editor.addElement(rotMode = new SelectorButton(editorX, editorY, editorX + 300, editorY + 20, 
				this.position.rotation == IRotation.NONE ? names[0] :  
				this.position.rotation instanceof EulerZYXRotation ? names[1] :  
				//this.defaultTransform.rotation instanceof EulerXYZRotation ? names[2] :  
				this.position.rotation instanceof QuaternionRotation ? names[2] : "Unknown rotation"
				, names, (ind, str) -> {
					this.onDeselect(editorPanes);
					IRotation old = position.rotation;
					switch (ind)
					{
					case 0:
						Main.instance.project.onAction();
						position.rotation = IRotation.NONE;
						break;
					case 1:
						Main.instance.project.onAction();
						(position.rotation = new EulerZYXRotation()).setFromQuaternion(old.getQuaternion());
						break;
					//case 2:
					//	(defaultTransform.rotation = new EulerXYZRotation()).setFromQuaternion(old.getQuaternion());
					//	break;
					case 2:
						Main.instance.project.onAction();
						(position.rotation = new QuaternionRotation()).setFromQuaternion(old.getQuaternion());
						break;
					}
					this.onSelect(editorPanes, origY);
					this.updateTransform();
				}));
		editorY += 20;
		editorY = this.position.rotation.onSelect(editorPanes, editorY, Main.instance.project::onAction, this::updateTransform);
		
		editor.addElement(labelOff     = new ComponentFloatingLabel( editorX      , editorY, editorX + 300, editorY + 20, Main.instance.fontMsg, "Offset"));
		editorY += 20;
		editor.addElement(offXT        = new ComponentTextFloat(     editorX      , editorY, editorX + 90 , editorY + 20, Main.instance.fontMsg, offX(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::offX));
		editor.addElement(offXP        = new ComponentIncrementFloat(editorX + 90 , editorY                             , offXT, 1));
		editor.addElement(offXS        = new ComponentIncrementFloat(editorX + 90 , editorY + 10                        , offXT, -1));
		editor.addElement(offYT        = new ComponentTextFloat(     editorX + 100, editorY, editorX + 190, editorY + 20, Main.instance.fontMsg, offY(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::offY));
		editor.addElement(offYP        = new ComponentIncrementFloat(editorX + 190, editorY                             , offYT, 1));
		editor.addElement(offYS        = new ComponentIncrementFloat(editorX + 190, editorY + 10                        , offYT, -1));
		editor.addElement(offZT        = new ComponentTextFloat(     editorX + 200, editorY, editorX + 290, editorY + 20, Main.instance.fontMsg, offZ(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::offZ));
		editor.addElement(offZP        = new ComponentIncrementFloat(editorX + 290, editorY                             , offZT, 1));
		editor.addElement(offZS        = new ComponentIncrementFloat(editorX + 290, editorY + 10                        , offZT, -1));
		editorY += 20;
		editor.addElement(labelTexPos  = new ComponentFloatingLabel( editorX      , editorY, editorX + 300, editorY + 20, Main.instance.fontMsg, "Texture Offset"));
		editorY += 20;
		editor.addElement(texOU        = new ComponentTextFloat(     editorX      , editorY, editorX + 140, editorY + 20, Main.instance.fontMsg, texU(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::texU));
		editor.addElement(texOUP       = new ComponentIncrementFloat(editorX + 140, editorY                             , texOU, 1));
		editor.addElement(texOUS       = new ComponentIncrementFloat(editorX + 140, editorY + 10                        , texOU, -1));
		editor.addElement(texOV        = new ComponentTextFloat(     editorX + 150, editorY, editorX + 290, editorY + 20, Main.instance.fontMsg, texV(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, this::texV));
		editor.addElement(texOVP       = new ComponentIncrementFloat(editorX + 290, editorY                             , texOV, 1));
		editor.addElement(texOVS       = new ComponentIncrementFloat(editorX + 290, editorY + 10                        , texOV, -1));
		editorY += 20;
		return editorY;
	}

	@Override
	public void onDeselect(EditorPanes editorPanes)
	{
		editorPanes.addBox.setParent(null);
		editorPanes.addMesh.setParent(null);
		editorPanes.copy.setEditable(null, null);
		editorPanes.remove.setEditable(null, null);
		GuiElementContainer editor = editorPanes.editor.container;
		editor.removeElement(labelName);
		editor.removeElement(textName);
		editor.removeElement(labelPos);
		editor.removeElement(posXT);
		editor.removeElement(posXP);
		editor.removeElement(posXS);
		editor.removeElement(posYT);
		editor.removeElement(posYP);
		editor.removeElement(posYS);
		editor.removeElement(posZT);
		editor.removeElement(posZP);
		editor.removeElement(posZS);
		editor.removeElement(rotMode);
		editor.removeElement(labelOff);
		editor.removeElement(offXT);
		editor.removeElement(offXP);
		editor.removeElement(offXS);
		editor.removeElement(offYT);
		editor.removeElement(offYP);
		editor.removeElement(offYS);
		editor.removeElement(offZT);
		editor.removeElement(offZP);
		editor.removeElement(offZS);
		editor.removeElement(labelTexPos);
		editor.removeElement(texOU);
		editor.removeElement(texOUP);
		editor.removeElement(texOUS);
		editor.removeElement(texOV);
		editor.removeElement(texOVP);
		editor.removeElement(texOVS);
		labelName = null;
		labelPos  = null;
		posXT     = null;
		posXP     = null;
		posXS     = null;
		posYT     = null;
		posYP     = null;
		posYS     = null;
		posZT     = null;
		posZP     = null;
		posZS     = null;
		rotMode   = null;
		labelOff  = null;
		offXT     = null;
		offXP     = null;
		offXS     = null;
		offYT     = null;
		offYP     = null;
		offYS     = null;
		offZT     = null;
		offZP     = null;
		offZS     = null;
		labelTexPos  = null;
		texOU     = null;
		texOUP    = null;
		texOUS    = null;
		texOV     = null;
		texOVP    = null;
		texOVS    = null;
		this.position.rotation.onDeselect(editorPanes);
	}

	@Override
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		Main.instance.project.onAction();
		this.name = name;
	}

	@Override
	public Collection<? extends IModelEditable> getChildren()
	{
		return children;
	}

	@Override
	public boolean hasChildren()
	{
		return !children.isEmpty();
	}

	@Override
	public boolean canBeChild(IModelEditable candidate)
	{
		return candidate instanceof ModelComponent;
	}

	@Override
	public void addChild(IModelEditable child)
	{
		if (child instanceof ModelComponent) this.children.add((ModelComponent) child);
	}

	@Override
	public void addChildBefore(IModelEditable child, IModelEditable position)
	{
		if (child instanceof ModelComponent)
		{
			if (!children.contains(child))
			{
				int pos = this.children.indexOf(position);
				if (pos < 0) pos = 0;
				this.children.add(pos, (ModelComponent) child);
			}
		}
	}

	@Override
	public void addChildAfter(IModelEditable child, IModelEditable position)
	{
		if (child instanceof ModelComponent)
		{
			if (!children.contains(child))
			{
				int pos = this.children.indexOf(position) + 1;
				if (pos <= 0) pos = this.children.size();
				this.children.add(pos, (ModelComponent) child);
			}
		}
	}

	@Override
	public void removeChild(IModelEditable child)
	{
		if (child instanceof ModelComponent) this.children.remove(child);
	}

	@Override
	public void movedTo(IEditableParent oldParent, IEditableParent newParent)
	{
		this.parent = oldParent instanceof IComponentParent ? (IComponentParent) oldParent : null;
		Matrix4d targetTransform = getTransformation();
		this.parent = newParent instanceof IComponentParent ? (IComponentParent) newParent : null;
		Matrix4d parentTransform = parent == null ? new Matrix4d() : parent.getTransformation();
		this.transformation = parentTransform.invert().mul(targetTransform);
		this.updateTransformVals();
	}

	@Override
	public Matrix4d getTransformation()
	{
		Matrix4d mat = new Matrix4d(this.transformation);
		if (parent != null) parent.getTransformation().mul(mat, mat);
		return mat;
	}

	@Override
	public List<ModelComponent> getChildrenComponents()
	{
		return children;
	}
	
	@Override
	public boolean isVisible()
	{
		return visible;
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		childrenVisible = this.visible = visible;
	}
	
	public void addToXML(AbstractElement addTo)
	{
		AbstractElement el = addTo.addChild(getXMLName());
		addData(el);
		addChildrenToXML(el);
	}
	
	public abstract String getXMLName();
	
	public void addData(AbstractElement el)
	{
		el.setString("name", name);
		position.save(el, 1);
		if (offset.x() != 0) el.setFloat("offX", offset.x());
		if (offset.y() != 0) el.setFloat("offY", offset.y());
		if (offset.z() != 0) el.setFloat("offZ", offset.z());
		if (texU != 0) el.setFloat("texU", texU);
		if (texV != 0) el.setFloat("texV", texV);
	}
	
	public void addChildrenToXML(AbstractElement addTo)
	{
		for (ModelComponent child : this.children) child.addToXML(addTo);
	}
	
	public void loadFromXML(AbstractElement el)
	{
		name = el.getString("name", "null");
		position.load(el, 1);
		offset.set(el.getFloat("offX", 0), el.getFloat("offY", 0), el.getFloat("offZ", 0));
		updateTransform();
		texU = el.getFloat("texU", 0);
		texV = el.getFloat("texV", 0);
		onTexSizeChange();
		loadChildrenFromXML(el);
	}
	
	public void loadChildrenFromXML(AbstractElement el)
	{
		children.clear();
		for (AbstractElement child : el.getChildren()) tryLoadChild(child);
	}

	public void tryLoadChild(AbstractElement el)
	{
		ModelComponent.loadComponent(this, el);
	}
	
	public void copyChildren(ModelComponent newParent, IRigged<?> model)
	{
		for (ModelComponent child : children) child.copy(newParent, model);
	}
	
	public static boolean loadComponent(IComponentParent parent, AbstractElement el)
	{
		switch (el.getName())
		{
		case "box":
		{
			ComponentBox box = new ComponentBox(parent, el.getString("name", "unnamed box"));
			box.loadFromXML(el);
			return true;
		}
		case "mesh":
		{
			ComponentMeshTrue mesh = new ComponentMeshTrue(new Mesh(), parent, el.getString("name", "unnamed mesh"));
			mesh.loadFromXML(el);
			return true;
		}
		default: return false;
		}
	}
	
	@Override
	public void updateTex()
	{
		this.setTexs();
		getChildrenComponents().forEach(component -> component.updateTex());
	}

	@Override
	public Transformation getDefaultTransformation()
	{
		return this.position;
	}
	
	public ModelComponent cloneObject(IComponentParent clonedParent)
	{
		ModelComponent newComponent = this.cloneSelf(clonedParent);
		this.children.forEach(child -> child.cloneObject(newComponent));
		return newComponent;
	}
	
	public abstract ModelComponent cloneSelf(IComponentParent clonedParent);
}