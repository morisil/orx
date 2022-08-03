package org.openrndr.extra.kinect.v1.demo

import org.openrndr.application
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.kinect.*
import org.openrndr.extra.kinect.v1.Kinect1
import org.openrndr.launch

/**
 * Shows 4 different representations of the depth map:
 *
 * * the original depth map stored as RED channel values
 * * the same values expressed as gray tones
 * * zucconi6 color map according to natural light dispersion as described
 *   by Alan Zucconi in
 *   [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
 *   article
 * * turbo color map according to
 *   [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
 *   by Google.
 *
 * @see DepthToGrayscaleMapper
 * @see DepthToColorsZucconi6Mapper
 * @see DepthToColorsTurboMapper
 */
fun main() = application {
    configure {
        width =  2 * 640
        height = 2 * 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        device.depthCamera.enabled = true
        val camera = device.depthCamera
        val grayscaleFilter = DepthToGrayscaleMapper()
        val zucconiFilter = DepthToColorsZucconi6Mapper()
        val turboFilter = DepthToColorsTurboMapper()
        val grayscaleBuffer = colorBuffer(camera.resolution.x, camera.resolution.y, format = ColorFormat.RGB)
        val zucconiBuffer = colorBuffer(camera.resolution.x, camera.resolution.y, format = ColorFormat.RGB)
        val turboBuffer = colorBuffer(camera.resolution.x, camera.resolution.y, format = ColorFormat.RGB)

        /*
         * Note: the frameFlow will deliver new frames as soon as they
         * arrive from kinect, therefore some rendering can be already
         * performed when GPU is idle.
         *
         * Also the filters are being applied only if the actual new frame
         * from kinect was received. Kinect has different refresh rate (30 fps)
         * than usual display will have.
         *
         * We are dispatching coroutine here, for details check:
         * https://guide.openrndr.org/advancedDrawing/concurrencyAndMultithreading.html
         */
        launch {
            camera.frameFlow.collect { frame ->
                grayscaleFilter.apply(frame, grayscaleBuffer)
                zucconiFilter.apply(frame, zucconiBuffer)
                turboFilter.apply(frame, turboBuffer)
            }
        }
        extend {
            drawer.image(camera.currentFrame)
            drawer.image(grayscaleBuffer, camera.resolution.x.toDouble(), 0.0)
            drawer.image(turboBuffer, 0.0, camera.resolution.y.toDouble())
            drawer.image(zucconiBuffer, camera.resolution.vector2)
        }
    }
}
