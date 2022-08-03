package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * Stream from 2 kinects side by side.
 */
fun main() = application {
    configure {
        width  = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val camera = device.depthCamera
        camera.enabled = true
        extend {
            drawer.image(camera.currentFrame)
        }
        keyboard.keyDown.listen { keyEvent ->
            if (keyEvent.name == "e") {camera.enabled = !camera.enabled }
            if (keyEvent.name == "v") {camera.flipV = !camera.flipV }
            if (keyEvent.name == "h") {camera.flipH = !camera.flipH }
        }
    }
}
