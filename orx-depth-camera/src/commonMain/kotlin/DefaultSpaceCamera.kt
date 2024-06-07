package org.openrndr.extra.depth.camera

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.openrndr.draw.*
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.utils.buffer.MPPBuffer

class DefaultDepthCamera(
    override val resolution: IntVector2,
    override var depthMeasurement: DepthMeasurement,
    override var flipH: Boolean = false,
    override var flipV: Boolean = false,
    override var minDepth: Double = 0.0,
    override var maxDepth: Double = 5.0,
    override var minX: Double = -1000.0,
    override var maxX: Double = 1000.0,
    override var minY: Double = -1000.0,
    override var maxY: Double = 1000.0,
    override var minZ: Double = -1000.0,
    override var maxZ: Double = 1000.0,
    override var transformationMatrix: Matrix44,
    private val rawDepthImageRenderer: Filter // this filter will depend on the depth camera
) : DepthCamera {

    private val rawDepthImage: ColorBuffer

    private val _rawDepthData = MutableSharedFlow<MPPBuffer>()
    override val rawDepthData: Flow<MPPBuffer> = _rawDepthData

    private val _renders = MutableSharedFlow<SpaceRender>()
    override val renders: Flow<SpaceRender> = _renders

    private val _front = DefaultSpaceRender()

    private val _back = DefaultSpaceRender()

    suspend fun handleRawDepth(buffer: MPPBuffer) {
        _rawDepthData.emit(buffer)
        rawDepthImageRenderer.apply()
        _back.render()
    }

    inner class DefaultSpaceRender(
        private val renderers: SpaceRenderers,
        private val depthMeasurement: DepthMeasurement
    ) : SpaceRender {

        private val depthImageDelegate = lazy { depthImageBuffer(resolution) }
        override val depthImage: ColorBuffer by depthImageDelegate

        private val pointCloudDelegate = lazy { pointCloudBuffer(resolution) }
        override val pointCloud: VertexBuffer by pointCloudDelegate

        private val spaceMeshDelegate = lazy { spaceMeshBuffer(resolution) }
        override val spaceMesh: VertexBuffer by spaceMeshDelegate

        suspend fun render(rawDepthImage: ColorBuffer) {
            if (depthImageDelegate.isInitialized()) {
                when (depthMeasurement) {
                    DepthMeasurement.METERS -> metricDepthImage.copyTo(depthImage)
                }
                renderers.depthImageRenderer.apply(metricDepthImage, depthImage)
            }
            if (pointCloudDelegate.isInitialized()) {
                renderers.pointCloudGenerator.run {
                    image()
                    execute()
                }
            }
            _renders.emit(_back)
            // swap here
        }

    }

}

