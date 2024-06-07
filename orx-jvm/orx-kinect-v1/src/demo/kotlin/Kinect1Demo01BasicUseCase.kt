import org.openrndr.application
import org.openrndr.extra.kinect.v1.Kinect1

/**
 * Basic kinect1 use case showing continuous stream from the depth camera.
 *
 * Note: kinect depth map is stored only on the RED color channel to save
 *       space. Therefore depth map is displayed only in the red tones.
 */
fun main() = application {
    configure { // default resolution of the Kinect v1 depth camera
        width = 640
        height = 480
    }
    program {
        val kinect = extend(Kinect1())
        val device = kinect.openDevice()
        val depthCamera = device.depthCamera
        depthCamera.flipH = true // to make a mirror
        depthCamera.enabled = true
        extend {
            drawer.image(depthCamera.render.depthImage)
        }
    }
}
