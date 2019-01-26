package net.natruid.jungle.views

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.czyzby.lml.parser.impl.AbstractLmlView
import net.natruid.jungle.core.Jungle

abstract class AbstractView : AbstractLmlView(Stage(ScreenViewport(), Jungle.instance.renderer.batch)) {
    private val renderer = Jungle.instance.renderer

    override fun render(delta: Float) {
        renderer.end()
        super.render(delta)
    }

    override fun render() {
        renderer.end()
        super.render()
    }

    abstract override fun getTemplateFile(): FileHandle

    companion object {
        inline fun <reified Type : AbstractView> createView() = createView(Type::class.java)

        fun <Type : AbstractView> createView(type: Class<Type>): Type {
            val view = type.getDeclaredConstructor().newInstance()
            Jungle.lmlParser.createView(view, view.templateFile)
            return view
        }
    }
}
