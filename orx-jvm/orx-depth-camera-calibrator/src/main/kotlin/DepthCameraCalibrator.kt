package org.openrndr.extra.depth.camera.calibrator

import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.extra.depth.camera.DepthCamera
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.math.Vector2
import java.io.File

class DepthCameraCalibrator(vararg depthCameras: DepthCamera) : Extension {

    init {
        check(depthCameras.isNotEmpty()) {
            "depthCameras cannot be empty"
        }
        depthCameras.forEach {
            check(it.depthMeasurement == DepthMeasurement.METERS) {
                "depthMeasurement of each depth camera should be set to METERS"
            }
        }
    }

//    var colormap = Turbocolo
    private val depthCameraMap = depthCameras.associateBy { it.id }

//    val calibrationMap = mapOf<>()

    var keyCamera: CameraGui = CameraGui(depthCameras[0].id)

    private var mutableEnabled: Boolean = false
    override var enabled: Boolean
        get() = mutableEnabled
        set(value) {
            mutableEnabled = value

        }

    var saveFolder: File = File("depth-camera-calibrations")

    override fun setup(program: Program) {
        program.keyboard.keyDown.listen {
            if (enabled) {
                handleKeyDown(it)
            }
        }
    }

    override fun afterDraw(drawer: Drawer, program: Program) {
        if (!enabled) { return }
    }

    fun handleKeyDown(event: KeyEvent) {
        val shift = event.modifiers.contains(KeyModifier.SHIFT)
        val controlScale = if (shift) 10.0 else 1.0
        when(event.key) {
            KEY_ARROW_LEFT -> keyCamera.offset -= Vector2(1.0, 0.0) * controlScale
            KEY_ARROW_RIGHT -> keyCamera.offset += Vector2(1.0, 0.0) * controlScale
            KEY_ARROW_UP -> keyCamera.offset -= Vector2(0.0, 1.0) * controlScale
            KEY_ARROW_DOWN -> keyCamera.offset += Vector2(0.0, 1.0) * controlScale
        }
        when(event.name) {
            "-" -> keyCamera.scale -= .1
            "=" -> keyCamera.scale += .1
        }
    }

    fun add() {

    }

    class CalibrationGui(
        var commonMinDepth: Double = 0.1
        var commonMaxDepth: Double = 10.1
    )

    class CameraGui(id: String) {
        private var mutableTuneWithKeyboard = false
        var tuneWithKeyboard: Boolean
            get = mutableTuneWithKeyboard
            set(value) {
                mutableTuneWithKeyboard = value
                //depth
            }
        var offset: Vector2 = Vector2.ZERO
        var scale: Vector2 = Vector2.ONE
        var minDepth: Double = .1
        var maxDepth: Double = .1
    }

}
