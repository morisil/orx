package org.openrndr.extra.kinect.v1.demo

import org.openrndr.Fullscreen
import org.openrndr.application
import org.openrndr.draw.Filter
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.depth.camera.calibrator.DepthCameraCalibrator
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.kinect.v1.Kinect1
import org.openrndr.math.Vector2

/**
 * How to use [DepthCameraCalibrator] with [Kinect1]?
 */
fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        camera.depthMeasurement = DepthMeasurement.METERS
        val outputBuffer = colorBuffer(
            camera.resolution.x,
            camera.resolution.y
        )
        val spaceRangeExtractor = SpaceRangeExtractor()
        spaceRangeExtractor.resolution = Vector2(width.toDouble(), height.toDouble())
        val calibrator = extend(DepthCameraCalibrator(camera))
        calibrator.onCalibrationChange { camera, calibration ->
            spaceRangeExtractor.minDepth = calibration.minDepth
            spaceRangeExtractor.maxDepth = calibration.maxDepth
            camera.flipH = calibration.flipH
            camera.flipV = calibration.flipV
        }
        camera.onFrameReceived { frame ->
            spaceRangeExtractor.apply(frame, outputBuffer)
        }
        device.depthCamera.enabled = true
        val gui = GUI()
//        extend() {
//            visible = false
//            compartmentsCollapsedByDefault = false
//        }
        calibrator.addControlsTo(gui)
        extend {
            val calibration = calibrator.getCalibration(camera)
            drawer.image(
                colorBuffer = outputBuffer,
                position = calibration.position,
                width = calibration.width,
                height = calibration.height
            )
        }
        extend(gui)

        program.keyboard.keyDown.listen {
            if (it.name == "k") {
                calibrator.enabled = !calibrator.enabled
            }
        }

    }

}

/**
 * A visual effect applied to kinect data in this demonstration.
 * Everything is black, except for the white pixels within the range
 * of 2 virtual walls positioned at [minDepth] at front of the
 * viewer and [maxDepth] behind the viewer.
 */
class SpaceRangeExtractor : Filter(filterShaderFromCode("""
    uniform usampler2D  tex0;             // kinect raw
    uniform vec2        resolution;
    uniform float       minDepth;
    uniform float       maxDepth;
    out     vec4        o_color;
    void main() {
        vec2 uv = gl_FragCoord.xy / resolution;
        float depth = texture(tex0, uv).r;
        float luma = ((depth >= minDepth) && (depth <= maxDepth)) ? 1.0 : 0.0;
        o_color = vec4(vec3(luma), 1.0);
    }
    """.trimIndent(),
    "space range extractor"
)) {
    var resolution: Vector2 by parameters
    var minDepth: Double by parameters
    var maxDepth: Double by parameters
}
