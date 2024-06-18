import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.pointclouds.DepthMapIntrinsicParameters
import org.openrndr.extra.pointclouds.DepthMapToPointCloudGenerator
import org.openrndr.math.Vector3

/**
 * Presents depth map image as an organized point cloud.
 */
fun main() = application {
    program {
        val depthMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val pointCloud = DepthMapToPointCloudGenerator(
            DepthMapIntrinsicParameters(
                fxD = 1.0,
                fyD = 2.0,
                cxD = 3.0,
                cyD = 4.0,
                spaceShift = Vector3.ZERO
            )
        ).generate(depthMap)
        extend(Orbital()) {
            eye = Vector3(0.03, 0.03, .3)
            lookAt = Vector3.ZERO
            near = 0.001
            keySpeed = .01
        }
        extend {
            drawer.vertexBuffer(pointCloud, DrawPrimitive.POINTS)
        }
    }
}
