import org.openrndr.application
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.extra.depth.camera.DefaultDepthCamera
import org.openrndr.math.IntVector2
import org.openrndr.utils.buffer.MPPBuffer

/**
 * Basic kinect1 use case showing continuous stream from the depth camera.
 *
 * Note: kinect depth map is stored only on the RED color channel to save
 *       space. Therefore depth map is displayed only in the red tones.
 */
fun main() = application {
    program {
        val depthImage = MPPBuffer(loadImage("").shadow.buffer)
        val depthCamera = DefaultDepthCamera(depthImage.resolution)
        extend {
            val render = depthCamera.render
            drawer.image(render.depthImage)
            // TODO set up orbital camera
            drawer.vertexBuffer(render.pointCloud, DrawPrimitive.POINTS)
            // TODO set up orbital camera
            drawer.vertexBuffer(render.spaceMesh, DrawPrimitive.TRIANGLES)
        }
    }
}

val ColorBuffer.resolution: IntVector2 get() = IntVector2(width, height)
