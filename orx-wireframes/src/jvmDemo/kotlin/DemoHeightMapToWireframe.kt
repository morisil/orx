import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.BlendMode
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.pointclouds.resolution
import org.openrndr.extra.pointclouds.toHeightPointCloud
import org.openrndr.extra.wireframes.toWireframe
import org.openrndr.math.Vector3

/**
 * Demonstrates a typical usage of converting an organized point cloud to a wireframe.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val wireframe = heightMap
            .toHeightPointCloud(
                heightScale = .5
            )
            .toWireframe(heightMap.resolution)
        extend(Orbital()) {
            eye = Vector3(0.03, 0.03, .3)
            lookAt = Vector3.ZERO
            near = .001
            keySpeed = .01
        }
        extend {
            drawer.fill = ColorRGBa.WHITE.opacify(.1)
            drawer.depthWrite = false
            drawer.drawStyle.blendMode = BlendMode.ADD
            drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
        }
    }
}
