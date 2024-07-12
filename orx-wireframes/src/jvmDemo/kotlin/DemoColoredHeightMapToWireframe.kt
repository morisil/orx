import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.pointclouds.resolution
import org.openrndr.extra.pointclouds.toColoredHeightPointCloud
import org.openrndr.extra.wireframes.toColoredWireframe
import org.openrndr.math.Vector3

/**
 * Demonstrates a typical usage of converting an organized and colored point cloud to a wireframe.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val earth = loadImage("demo-data/images/nasa-blue-marble.png")
        val wireframe = heightMap
            .toColoredHeightPointCloud(
                colors = earth,
                heightScale = .05
            )
            .toColoredWireframe(heightMap.resolution)
        val style = shadeStyle {
            fragmentTransform = "x_fill.rgb = va_color.rgb;"
        }
        extend(Orbital()) {
            eye = Vector3(0.1, 0.05, .1)
            lookAt = Vector3.ZERO
            near = .001
            keySpeed = .01
        }
        extend {
            drawer.shadeStyle = style
            drawer.vertexBuffer(wireframe, DrawPrimitive.LINES)
        }
    }
}
