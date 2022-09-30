package org.openrndr.extra.depth.camera.calibrator

import org.openrndr.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.parameters.*
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

interface Calibration {
    val resolution: Vector2
    var includeInTuning: Boolean
    var flipH: Boolean
    var flipV: Boolean
    var offset: Vector2
    var rotation: Double
    var scale: Double

    val width: Double get() = with (resolution) {
        x * y / y
    }

    val height: Double get() = resolution.y

    val position: Vector2 get() =
        (resolution - Vector2(resolution.x - width, 0.0)) / -2.0

    /**
     * Note this function will be called within [isolatedWithCalibration].
     */
    fun draw(drawer: Drawer)

}

abstract class ParametrizedCalibration : Calibration {

    @BooleanParameter(label = "include in tuning", order = 0)
    override var includeInTuning: Boolean = false

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

    @DoubleParameter(label = "rotation [l/r]", low = -360.0, high = 360.0, order = 4)
    override var rotation: Double = 0.0

    @DoubleParameter(label = "scale [+/-]", low = 0.0, high = 10.0, order = 5)
    override var scale: Double = 1.1

}

class ParametrizedColorBufferCalibration(
    private val colorBuffer: ColorBuffer
) : ParametrizedCalibration() {

    override val resolution
        get() = IntVector2(colorBuffer.width, colorBuffer.height).vector2

    override fun draw(drawer: Drawer) {
        drawer.image(colorBuffer, this)
    }

}

/**
 * Calibrator extension which can help with interactive positioning of
 * [ColorBuffer]s and depth cameras.
 *
 * @param calibrations the calibrations to manage with this calibrator.
 */
class Calibrator(
    vararg calibrations: Calibration
) : Extension {

    init {
        require(calibrations.isNotEmpty()) {
            "calibrations cannot be empty"
        }
    }

    override var enabled: Boolean = false

    private val calibrations = calibrations.toList()

    override fun afterDraw(drawer: Drawer, program: Program) {
        // will be called only if enabled
        calibrations.forEach { calibration ->
            drawer.isolatedWithCalibration(calibration) {
                calibration.draw(this)
            }
        }
    }

}

fun List<Calibration>.tuneWithKeyboard(keyboard: Keyboard) {
    keyboard.keyDown.listen { event ->
        filter { it.includeInTuning }
        .forEach {
            when(event.key) {
                KEY_ARROW_LEFT -> it.offset += left * OFFSET_CHANGE_SCALE
                KEY_ARROW_RIGHT -> it.offset += right * OFFSET_CHANGE_SCALE
                KEY_ARROW_UP -> it.offset += up * OFFSET_CHANGE_SCALE
                KEY_ARROW_DOWN -> it.offset += down * OFFSET_CHANGE_SCALE
            }
            when(event.name) {
                "-" -> it.scale -= SCALE_CHANGE
                "=" -> it.scale += SCALE_CHANGE
                "l" -> it.rotation -= ROTATION_CHANGE
                "r" -> it.rotation += ROTATION_CHANGE
            }
        }
    }
}

fun Drawer.isolatedWithCalibration(
    calibration: Calibration,
    block: Drawer.() -> Unit
) {
    isolated {
        translate(
            IntVector2(width, height).vector2 / 2.0
                    + calibration.offset * Vector2(1.0, -1.0) * height.toDouble()
        )
        rotate(calibration.rotation)
        scale(calibration.scale)
        block()
    }
}

fun Drawer.image(colorBuffer: ColorBuffer, calibration: Calibration) {
    image(
        colorBuffer = colorBuffer,
        position = calibration.position,
        width = calibration.width,
        height = calibration.height
    )
}

private const val OFFSET_CHANGE_SCALE = .001
private const val ROTATION_CHANGE = .1
private const val SCALE_CHANGE = .001

val left = Vector2(-1.0, 0.0)
val right = Vector2(1.0, 0.0)
val up = Vector2(0.0, 1.0)
val down = Vector2(0.0, -1.0)
