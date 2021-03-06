package org.openrndr.extra.compositor

import org.openrndr.Extension
import org.openrndr.Program
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.extra.parameters.BooleanParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.math.Matrix44

private val postBufferCache = mutableListOf<ColorBuffer>()

fun RenderTarget.deepDestroy() {
    val cbcopy = colorBuffers.map { it }
    val dbcopy = depthBuffer
    detachDepthBuffer()
    detachColorBuffers()
    cbcopy.forEach {
        it.destroy()
    }
    dbcopy?.destroy()
    destroy()
}

/**
 * A single layer representation
 */
@Description("Layer")
open class Layer internal constructor() {
    var drawFunc: () -> Unit = {}
    val children: MutableList<Layer> = mutableListOf()
    var blendFilter: Pair<Filter, Filter.() -> Unit>? = null
    val postFilters: MutableList<Pair<Filter, Filter.() -> Unit>> = mutableListOf()
    var colorType = ColorType.UINT8
    var accumulation: ColorBuffer? = null

    @BooleanParameter("enabled")
    var enabled = true
    var clearColor: ColorRGBa? = ColorRGBa.TRANSPARENT
    private var layerTarget: RenderTarget? = null

    /**
     * draw the layer
     */
    fun drawLayer(drawer: Drawer) {
        if (!enabled) {
            return
        }

        val activeRenderTarget = RenderTarget.active

        accumulation = if (activeRenderTarget !is ProgramRenderTarget) {
            activeRenderTarget.colorBuffer(0)
        } else {
          null
        }

        val localLayerTarget = layerTarget
        if (localLayerTarget == null || (localLayerTarget.width != activeRenderTarget.width || localLayerTarget.height != activeRenderTarget.height)) {
            layerTarget?.deepDestroy()
            layerTarget = renderTarget(activeRenderTarget.width, activeRenderTarget.height) {
                colorBuffer(type = colorType)
                depthBuffer()
            }
            layerTarget?.let {
                drawer.withTarget(it) {
                    drawer.background(ColorRGBa.TRANSPARENT)
                }
            }
        }

        layerTarget?.let { target ->
            drawer.isolatedWithTarget(target) {
                clearColor?.let {
                    drawer.background(it)
                }
                drawFunc()
                children.forEach {
                    it.drawLayer(drawer)
                }
            }

            if (postFilters.size > 0) {
                val sizeMismatch = if (postBufferCache.isNotEmpty()) {
                    postBufferCache[0].width != activeRenderTarget.width || postBufferCache[0].height != activeRenderTarget.height
                } else {
                    false
                }

                if (sizeMismatch) {
                    postBufferCache.forEach { it.destroy() }
                    postBufferCache.clear()
                }

                if (postBufferCache.isEmpty()) {
                    postBufferCache += persistent { colorBuffer(activeRenderTarget.width, activeRenderTarget.height, type = colorType) }
                    postBufferCache += persistent { colorBuffer(activeRenderTarget.width, activeRenderTarget.height, type = colorType) }
                }
            }

            val layerPost = postFilters.let { filters ->
                val targets = postBufferCache
                val result = filters.foldIndexed(target.colorBuffer(0)) { i, source, filter ->
                    val target = targets[i % targets.size]
                    filter.first.apply(filter.second)
                    filter.first.apply(source, target)
                    target
                }
                result
            }

            val localBlendFilter = blendFilter
            if (localBlendFilter == null) {
                drawer.isolatedWithTarget(activeRenderTarget) {
                    drawer.ortho()
                    drawer.view = Matrix44.IDENTITY
                    drawer.model = Matrix44.IDENTITY
                    drawer.image(layerPost, layerPost.bounds, drawer.bounds)
                }
            } else {
                localBlendFilter.first.apply(localBlendFilter.second)
                localBlendFilter.first.apply(arrayOf(activeRenderTarget.colorBuffer(0), layerPost), activeRenderTarget.colorBuffer(0))
            }
        }
    }
}

/**
 * create a layer within the composition
 */
fun Layer.layer(function: Layer.() -> Unit): Layer {
    val layer = Layer().apply { function() }
    children.add(layer)
    return layer
}

/**
 * set the draw contents of the layer
 */
fun Layer.draw(function: () -> Unit) {
    drawFunc = function
}

/**
 * add a post-processing filter to the layer
 */
fun <F : Filter> Layer.post(filter: F, configure: F.() -> Unit = {}): F {
    @Suppress("UNCHECKED_CAST")
    postFilters.add(Pair(filter as Filter, configure as Filter.() -> Unit))
    return filter
}

/**
 * add a blend filter to the layer
 */
fun <F : Filter> Layer.blend(filter: F, configure: F.() -> Unit = {}): F {
    @Suppress("UNCHECKED_CAST")
    blendFilter = Pair(filter as Filter, configure as Filter.() -> Unit)
    return filter
}

class Composite: Layer() {
    fun draw(drawer:Drawer) {
        drawLayer(drawer)
    }
}

/**
 * create a layered composition
 */
fun compose(function: Layer.() -> Unit): Composite {
    val root = Composite()
    root.function()
    return root
}

class Compositor : Extension {
    override var enabled: Boolean = true
    var composite = Composite()

    override fun afterDraw(drawer: Drawer, program: Program) {
        drawer.isolated {
            drawer.defaults()
            composite.drawLayer(drawer)
        }
    }
}