package net.natruid.jungle.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.Disposable

class RendererHelper : Disposable {
    enum class Type { None, SpriteBatch, ShapeRenderer }

    val batch = SpriteBatch()
    val shapeRenderer = ShapeRenderer()

    private var current = Type.None
    private var shapeType = ShapeRenderer.ShapeType.Line

    fun begin(camera: OrthographicCamera, rendererType: Type, shapeType: ShapeRenderer.ShapeType = ShapeRenderer.ShapeType.Line) {
        if (current == rendererType && (rendererType != Type.ShapeRenderer || this.shapeType == shapeType)) return

        end()

        when (rendererType) {
            Type.SpriteBatch -> {
                batch.color = Color.WHITE
                batch.projectionMatrix = camera.combined
                batch.enableBlending()
                batch.begin()
            }
            Type.ShapeRenderer -> {
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
            Type.SpriteBatch -> batch.end()
            Type.ShapeRenderer -> {
                shapeRenderer.end()
                Gdx.gl.glDisable(GL20.GL_BLEND)
            }
            else -> {
                return
            }
        }

        current = Type.None
    }

    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
    }
}
