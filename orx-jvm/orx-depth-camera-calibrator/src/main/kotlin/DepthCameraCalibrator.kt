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

fun parametrizedDepthCameraCalibration(
    camera: DepthCamera,
    outputResolution: Vector2
) = ParametrizedDepthCameraCalibration(
    inputResolution = camera.resolution.vector2,
    outputResolution = outputResolution,
    render = renderDepthCameraCalibration(camera) as (drawer: Drawer, calibration: Calibration) -> Unit
)

class ParametrizedDepthCameraCalibration(
    override val inputResolution: Vector2,
    override val outputResolution: Vector2,
    override val render: (drawer: Drawer, calibration: Calibration) -> Unit
) : DepthCameraCalibration, ParametrizedCalibration(
    inputResolution,
    outputResolution,
    render
) {

    @BooleanParameter(label = "include in tuning", order = 0)
    override var includeInTuning: Boolean = true

    @BooleanParameter(label = "flipH", order = 1)
    override var flipH: Boolean = false

    @BooleanParameter(label = "flipV", order = 2)
    override var flipV: Boolean = false

    @XYParameter(
        label = "offset [arrows]",
        minX = -1.0,
        minY = -1.0,
        maxX = 1.0,
        maxY = 1.0,
        invertY = true,
        order = 3
    )
    override var offset: Vector2 = Vector2.ZERO

    @DoubleParameter(
        label = "rotation [l/r]",
        low = -360.0,
        high = 360.0,
        order = 4
    )
    override var rotation: Double = 0.0

    @DoubleParameter(
        label = "scale [+/-]",
        low = 0.0,
        high = 10.0,
        order = 5
    )
    override var scale: Double = 1.0

    @ActionParameter(label = "reset")
    override fun reset() {
        super<ParametrizedCalibration>.reset()
    }

    @DoubleParameter(
        label = "min depth [1/2]",
        low = 0.0,
        high = 10.0,
        order = 6
    )
    override var minDepth = 0.2

    @DoubleParameter(
        label = "max depth [3/4]",
        low = 0.0,
        high = 10.0,
        order = 7
    )
    override var maxDepth = 10.0

}

val defaultDepthTuningKeys: (
    event: KeyEvent,
    calibration: DepthCameraCalibration,
) -> Unit = { event, calibration ->
    when (event.name) {
        "1" -> calibration.minDepth -= CENTIMETER
        "2" -> calibration.minDepth += CENTIMETER
        "3" -> calibration.maxDepth -= CENTIMETER
        "4" -> calibration.maxDepth += CENTIMETER
    }
}

val depthCameraTuningKeys: (
    event: KeyEvent,
    calibration: Calibration,
) -> Unit = { event, calibration ->
    defaultTuningKeys(event, calibration)
    if (calibration is DepthCameraCalibration) {
        defaultDepthTuningKeys(event, calibration)
    }
}

private const val CENTIMETER = .01

fun renderDepthCameraCalibration(
    camera: DepthCamera
) : (drawer: Drawer, calibration: DepthCameraCalibration) -> Unit {

    check(camera.depthMeasurement == DepthMeasurement.METERS) {
        "DepthCamera calibration requires depthMeasurement expressed in METERS"
    }

    val turboBuffer = colorBuffer(
        width = camera.resolution.x,
        height = camera.resolution.y
    )

    val colormap = TurboColormap()

    return { drawer, calibration ->
        colormap.minValue = calibration.minDepth
        colormap.maxValue = calibration.maxDepth
        colormap.apply(camera.currentFrame, turboBuffer)
        drawer.image(turboBuffer, calibration)
    }

}
