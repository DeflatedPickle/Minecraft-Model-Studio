package firemerald.mcms.api.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.joml.Matrix4d;

import firemerald.mcms.Main;
import firemerald.mcms.api.animation.Transformation;
import firemerald.mcms.api.data.AbstractElement;
import firemerald.mcms.api.model.effects.BoneEffect;
import firemerald.mcms.shader.Shader;
import firemerald.mcms.util.GuiUpdate;

public abstract class RenderBone<T extends RenderBone<T>> extends ObjectBone<T>
{
	@Override
	public Collection<? extends IModelEditable> getChildren()
	{
		return Stream.concat(effects.stream(), super.getChildren().stream()).collect(Collectors.toList());
	}

	@Override
	public boolean hasChildren()
	{
		return super.hasChildren() || !effects.isEmpty();
	}

	@Override
	public boolean canBeChild(IModelEditable candidate)
	{
		return super.canBeChild(candidate) || candidate instanceof BoneEffect;
	}

	@Override
	public void addChild(IModelEditable child)
	{
		if (child instanceof BoneEffect && !this.effects.contains(child)) this.effects.add((BoneEffect) child);
		else super.addChild(child);
	}

	@Override
	public void addChildBefore(IModelEditable child, IModelEditable position)
	{
		if (child instanceof BoneEffect && !this.effects.contains(child))
		{
			int pos = this.effects.indexOf(position);
			if (pos < 0) pos = 0;
			this.effects.add(pos, (BoneEffect) child);
		}
		else super.addChildBefore(child, position);
	}

	@Override
	public void addChildAfter(IModelEditable child, IModelEditable position)
	{
		if (child instanceof BoneEffect && !this.effects.contains(child))
		{
			int pos = this.effects.indexOf(position) + 1;
			if (pos <= 0) pos = this.effects.size();
			this.effects.add(pos, (BoneEffect) child);
		}
		else super.addChildAfter(child, position);
	}

	@Override
	public void removeChild(IModelEditable child)
	{
		if (child instanceof BoneEffect) this.effects.remove(child);
		else super.removeChild(child);
	}
	
	@Override
	public int getChildIndex(IModelEditable child)
	{
		if (child instanceof BoneEffect)
		{
			if (effects.contains(child)) return effects.indexOf(child);
			else return -1;
		}
		else return super.getChildIndex(child);
	}

	@Override
	public void addChildAt(IModelEditable child, int index)
	{
		if (child instanceof BoneEffect)
		{
			if (!effects.contains(child))
			{
				if (index <= 0) index = 0;
				this.effects.add(index, (BoneEffect) child);
			}
		}
		else super.addChildAt(child, index);
	}

	@Override
	public void copyChildren(T newParent, IRigged<?, ?> model)
	{
		super.copyChildren(newParent, model);
		effects.forEach(effect -> effect.copy(newParent, model));
	}
	
	@Override
	public T cloneObject(T clonedParent)
	{
		final T cloned = super.cloneObject(clonedParent);
		this.effects.forEach(component -> component.cloneObject(cloned));
		return cloned;
	}
	
	protected final List<BoneEffect> effects = new ArrayList<>();
	
	public RenderBone(String name, Transformation defaultTransform, @Nullable T parent)
	{
		super(name, defaultTransform, parent);
	}

	@Override
	public void onGuiUpdate(GuiUpdate reason)
	{
		super.onGuiUpdate(reason);
		this.effects.forEach(effect -> effect.onGuiUpdate(reason));
	}
	
	public void render(Map<String, Matrix4d> transformations, Runnable defaultTexture)
	{
		if (visible || childrenVisible)
		{
			Shader.MODEL.push();
			Shader.MODEL.matrix().mul(transformations.get(this.name));
			Main.instance.shader.updateModel();
			effects.forEach(effect -> effect.preRender(defaultTexture));
			if (visible) doRender(defaultTexture);
			effects.forEach(effect -> effect.postRenderBone(defaultTexture));
			if (childrenVisible) for (T child : children) child.render(transformations, defaultTexture);
			effects.forEach(effect -> effect.postRenderChildren(defaultTexture));
			Shader.MODEL.pop();
			Main.instance.shader.updateModel();
		}
	}

	@Override
	public void cleanUp()
	{
		super.cleanUp();
		effects.forEach(BoneEffect::doCleanUp);
	}
	
	public abstract void doRender(Runnable defaultTexture);
	// TODO effects raytrace
	/*
	public RaytraceResult raytraceLocal(float fx, float fy, float fz, float dx, float dy, float dz, Map<String, Matrix4d> transformations, Matrix4d transformation)
	{
		return null;
	}
	*/
	
	public void addEffect(BoneEffect effect)
	{
		this.effects.add(effect);
	}
	
	public void removeEffect(BoneEffect effect)
	{
		this.effects.remove(effect);
	}
	
	@Override
	public void addChildrenToXML(AbstractElement addTo, float scale)
	{
		super.addChildrenToXML(addTo, scale);
		this.effects.forEach(effect -> effect.addToXML(addTo, scale));
	}

	@Override
	public void loadChildrenFromXML(AbstractElement el, float scale)
	{
		effects.clear();
		super.loadChildrenFromXML(el, scale);
	}

	@Override
	public void tryLoadChild(AbstractElement el, float scale)
	{
		if (BoneEffect.constructIfRegistered(el.getName(), this, el, scale) == null) super.tryLoadChild(el, scale);
	}
	
	public List<BoneEffect> getEffects()
	{
		return effects;
	}
}