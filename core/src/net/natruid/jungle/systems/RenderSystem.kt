package net.natruid.jungle.systems

import com.artemis.Aspect
import net.natruid.jungle.components.*
import net.natruid.jungle.core.Jungle
import net.natruid.jungle.systems.abstracts.AbstractRenderSystem

class RenderSystem : AbstractRenderSystem(
    Jungle.instance.camera,
    Aspect.all(TransformComponent::class.java).one(
        TextureComponent::class.java,
        LabelComponent::class.java,
        RectComponent::class.java,
        CircleComponent::class.java,
        RenderableComponent::class.java
    ).exclude(UIComponent::class.java)
)
