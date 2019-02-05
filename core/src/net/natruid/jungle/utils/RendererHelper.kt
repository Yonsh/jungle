package net.natruid.jungle.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable
import net.natruid.jungle.components.ShaderComponent

class RendererHelper : Disposable {
    enum class Type { NONE, SPRITE_BATCH, SHAPE_RENDERER }

    val batch = SpriteBatch(1000, ShaderComponent.defaultShader)
    val shapeRenderer = ShapeRenderer()

    private var current = Type.NONE
    private var shapeType = ShapeRenderer.ShapeType.Line

    fun begin(
            camera: OrthographicCamera,
            rendererType: Type,
            shapeType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line,
            shaderProgram: ShaderProgram = ShaderComponent.defaultShader
    ) {
        if (rendererType == Type.SPRITE_BATCH && batch.shader != shaderProgram) batch.shader = shaderProgram
        if (current == rendererType && (rendererType != Type.SHAPE_RENDERER || this.shapeType == shapeType)) return

        end()

        when (rendererType) {
            Type.SPRITE_BATCH -> {
                batch.color = Color.WHITE
                batch.projectionMatrix = camera.combined
                batch.enableBlending()
                batch.begin()
            }
            Type.SHAPE_RENDERER -> {
                shapeRenderer.projectionMatrix = camera.combined
                Gdx.gl.let {
                    it.glEnable(GL20.GL_BLEND)
                    it.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                }
                shapeRenderer.begin(shapeType)
                this.shapeType = shapeType
            }
            else -> {
                return
            }
        }

        current = rendererType
    }

    fun end() {
        when (current) {
            Type.SPRITE_BATCH -> {
                batch.end()
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
                batch.color = Color.WHITE
                if (batch.shader != ShaderComponent.defaultShader) batch.shader = ShaderComponent.defaultShader
            }
            Type.SHAPE_RENDERER -> {
                shapeRenderer.end()
                Gdx.gl.glDisable(GL20.GL_BLEND)
            }
            else -> {
                return
            }
        }

        current = Type.NONE
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
