package net.natruid.jungle.screens

import com.artemis.Aspect
import com.artemis.WorldConfigurationBuilder
import com.artemis.managers.TagManager
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import net.natruid.jungle.components.UnitComponent
import net.natruid.jungle.core.Jungle
import net.natruid.jungle.systems.*
import net.natruid.jungle.utils.extensions.forEach
import kotlin.random.Random

class FieldScreen : AbstractScreen(WorldConfigurationBuilder().with(
    TagManager(),
    TileSystem(),
    UnitManagementSystem(),
    IndicatorSystem(),
    PathfinderSystem(),
    PathFollowingSystem(),
    CameraMovementSystem(),
    RenderSystem()
).build()) {
    init {
        init()
    }

    private fun init(seed: Long = Random.nextLong()) {
        world.getSystem(TileSystem::class.java).create(20, 20, seed)
        world.getSystem(UnitManagementSystem::class.java)
            .addUnit(faction = UnitComponent.Faction.PLAYER, speed = 6f)
        world.getSystem(RenderSystem::class.java).sort()
    }

    override fun show() {
        super.show()
        val camera = Jungle.instance.camera
        camera.translate(400f, 300f)
        camera.update()
    }

    override fun keyUp(keycode: Int): Boolean {
        super.keyUp(keycode)
        if (keycode == Input.Keys.R) {
            world.getSystem(UnitManagementSystem::class.java).clean()
            world.aspectSubscriptionManager.get(Aspect.all()).entities.forEach {
                world.delete(it)
            }
            if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
                init(world.getSystem(TileSystem::class.java).seed)
            } else {
                init()
            }
            return true
        }

        return false
    }
}
