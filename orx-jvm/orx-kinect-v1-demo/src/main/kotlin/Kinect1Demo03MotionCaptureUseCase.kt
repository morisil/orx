package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.depth.camera.DepthMeasurement
import org.openrndr.extra.kinect.RedToTurboColormap
import org.openrndr.extra.kinect.v1.Kinect1
import org.openrndr.launch

/**
 * A use case where "virtual walls" can be established within certain
 * depth ranges. Useful for actual installations, like interactive
 * projections in the form of a mirror. The measurement in meters helps
 * in calibration.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        camera.flipH = true // to make a mirror
        camera.depthMeasurement = DepthMeasurement.METERS
        val toTurbo = RedToTurboColormap().apply {
            minValue = 0.0
            maxValue = 1.0
        }
        val outputBuffer = colorBuffer(
            camera.resolution.x,
            camera.resolution.y,
            format = ColorFormat.RGB
        )
        device.depthCamera.enabled = true
        launch {
            device.depthCamera.frameFlow.collect { frame ->
                toTurbo.apply(frame, outputBuffer)
            }
        }
        extend {
            drawer.image(outputBuffer)
        }
    }
}
