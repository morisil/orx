package org.openrndr.extra.depth.camera

import org.openrndr.draw.*
import org.openrndr.math.IntVector2
import org.openrndr.utils.buffer.MPPBuffer

class SpaceRenderers : AutoCloseable {

    private val depthImageRendererDelegate = lazy {  }
    val depthImageRenderer: Filter by depthImageRendererDelegate

    val pointCloudGenerator: ComputeShader by lazy {  }

    val spaceMeshGenerator: ComputeShader by lazy {  }

    override fun close() {
        if (depthImageRendererDelegate.isInitialized()) {
            depthImageRenderer.destroy()
        }
    }

}

class SpaceRenderChain(
    resolution: IntVector2,
    private val renderers: SpaceRenderers
) : AutoCloseable {

    val depthData: MPPBuffer get() = depthDataRef.get()

    private val depthImageDelegate = lazy { depthImageBuffer(resolution) }
    val depthImage: ColorBuffer by depthImageDelegate

    private val pointCloudDelegate = lazy { pointCloudBuffer(resolution) }
    val pointCloud: VertexBuffer by pointCloudDelegate

    private val spaceMeshDelegate = lazy { spaceMeshBuffer(resolution) }
    val spaceMesh: VertexBuffer by spaceMeshDelegate

    private val pointCloudGenerator = ComputeShader.fromCode("", "")

    private val spaceMeshGenerator = ComputeShader.fromCode("", "")

    fun render(depthImage: ColorBuffer) {
        //depthDataRef.set(byteBuffer)
        if (depthImageDelegate.isInitialized()) {
            depthImage.write(depthData)
            renderers.depthImageRenderer.apply()
        }
        if (pointCloudDelegate.isInitialized()) {
            renderers.depthImageRenderer.
            pointCloudGenerator.
        }
    }

    override fun close() {
        if (depthImageDelegate.isInitialized()) { depthImage.destroy() }
        if (pointCloudDelegate.isInitialized()) { pointCloud.destroy() }
        if (spaceMeshDelegate.isInitialized()) { spaceMesh.destroy() }
    }

}



fun depthImageBuffer(
    resolution: IntVector2
) = colorBuffer(
    width = resolution.x,
    height = resolution.y,
    type = ColorType.UINT16_INT
)


