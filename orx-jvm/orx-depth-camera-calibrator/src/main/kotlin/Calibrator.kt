package org.openrndr.extra.depth.camera.calibrator

import org.openrndr.*
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.Drawer
import org.openrndr.draw.isolated
import org.openrndr.extra.parameters.*
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2

interface Calibration {

    /**
     * Renders calibration view of the input.
     *
     * Note: This function will be called from within [isolatedWithCalibration].
     * Therefore, for the simplest scenario, it is enough to implement:
     * ```
     * drawer.image(colorBuffer, calibration)
     * ```
     *
     * @see image
     */
    val render: (drawer: Drawer, calibration: Calibration) -> Unit
    val inputResolution: Vector2
    val outputResolution: Vector2
    var includeInTuning: Boolean
    var flipH: Boolean
    var flipV: Boolean
    var offset: Vector2
    var rotation: Double
    var scale: Double

    val width: Double get() =
        inputResolution.x * outputResolution.y / inputResolution.y

    val height: Double get() = outputResolution.y

    val position: Vector2 get() =
        (outputResolution - Vector2(outputResolution.x - width, 0.0)) / -2.0

    fun reset() {
        flipH = false
        flipV = false
        offset = Vector2.ZERO
        rotation = 0.0
        scale = 1.0
    }

}

typealias TuningKeys = (event: KeyEvent, calibration: Calibration) -> Unit

val defaultTuningKeys: TuningKeys = { event, calibration ->
    when (event.key) {
        KEY_ARROW_LEFT -> calibration.offset += left * OFFSET_CHANGE_SCALE
        KEY_ARROW_RIGHT -> calibration.offset += right * OFFSET_CHANGE_SCALE
        KEY_ARROW_UP -> calibration.offset += up * OFFSET_CHANGE_SCALE
        KEY_ARROW_DOWN -> calibration.offset += down * OFFSET_CHANGE_SCALE
    }
    when (event.name) {
        "-" -> calibration.scale -= SCALE_CHANGE
        "=" -> calibration.scale += SCALE_CHANGE
        "l" -> calibration.rotation -= ROTATION_CHANGE
        "r" -> calibration.rotation += ROTATION_CHANGE
    }
}

/**
 * Calibrator extension which can help with interactive positioning of
 * [ColorBuffer]s and depth cameras.
 *
 * @param calibrations the calibrations to manage with this calibrator.
 */
class Calibrator<C : Calibration>(
    vararg calibrations: C
) : Extension {

    init {
        require(calibrations.isNotEmpty()) {
            "calibrations cannot be empty"
        }
    }

    private lateinit var program: Program

    var tuningKeys: TuningKeys = defaultTuningKeys

    private val keyboardListener: (KeyEvent) -> kotlin.Unit = { event ->
        calibrations
            .filter { it.includeInTuning }
            .forEach { calibration ->
                tuningKeys(event, calibration)
            }
    }

    private fun maybeListenToKeyboard() {
        if (this::program.isInitialized) {
            if (enabled && tuneWithKeyboard) {
                program.keyboard.keyDown.listen(keyboardListener)
            } else {
                program.keyboard.keyDown.cancel(keyboardListener)
            }
        }
    }

    override var enabled: Boolean = false
        set(value) {
            if (value != field) {
                field = value
                maybeListenToKeyboard()
            }
        }

    var tuneWithKeyboard: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                maybeListenToKeyboard()
            }
        }

    val calibrations = calibrations.toList()

    override fun setup(program: Program) {
        this.program = program
        maybeListenToKeyboard()
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        // will be called only if enabled
        calibrations.forEach { calibration ->
            drawer.isolatedWithCalibration(calibration) {
                calibration.render(this, calibration)
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

fun Drawer.rectangle(calibration: Calibration) {
    rectangle(
        corner = calibration.position,
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

open class ParametrizedCalibration(
    override val inputResolution: Vector2,
    override val outputResolution: Vector2,
    override val render: (drawer: Drawer, calibration: Calibration) -> Unit
) : Calibration {

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
        super.reset()
    }

}

fun resolution(width: Int, height: Int): Vector2 = Vector2(
    width.toDouble(), height.toDouble()
)
