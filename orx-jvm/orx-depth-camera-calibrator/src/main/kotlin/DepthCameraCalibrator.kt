package org.openrndr.extra.depth.camera.calibrator

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.depth.camera.DepthCamera
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.fx.colormap.TurboColormap
import org.openrndr.extra.parameters.*
import org.openrndr.math.Vector2

interface DepthCameraCalibration : Calibration {
    var minDepth: Double
    var maxDepth: Double
}

class ParametrizedDepthCameraCalibration(
    private val camera: DepthCamera
) : ParametrizedCalibration(), DepthCameraCalibration {

    init {
        check(camera.depthMeasurement == DepthMeasurement.METERS) {
            "DepthCamera calibration requires depthMeasurement expressed in METERS"
        }
    }

    private val turboBuffer = colorBuffer(
        width = camera.resolution.x,
        height = camera.resolution.y
    )

    private val colormap = TurboColormap()

    override val resolution: Vector2
        get() = camera.resolution.vector2



    @DoubleParameter(label = "min depth [a/s]", low = 0.0, high = 10.0, order = 6)
    override var minDepth: Double = 0.2

    @DoubleParameter(label = "max depth [d/f]", low = 0.0, high = 10.0, order = 7)
    override var maxDepth: Double = 10.0

    override fun draw(drawer: Drawer) {
        colormap.minValue = minDepth
        colormap.maxValue = maxDepth
        colormap.apply(camera.currentFrame, turboBuffer)
        drawer.image(turboBuffer)
    }

}

fun List<DepthCameraCalibration>.tuneDepthWithKeyboard(keyboard: Keyboard) {
    keyboard.keyDown.listen { event ->
        filter { it.includeInTuning }
            .forEach {
                when(event.name) {
                    "a" -> it.minDepth -= CENTIMETER
                    "s" -> it.minDepth += CENTIMETER
                    "d" -> it.maxDepth -= CENTIMETER
                    "f" -> it.maxDepth += CENTIMETER
                }
            }
    }
}

private const val CENTIMETER = .01
