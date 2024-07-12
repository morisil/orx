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
 * Renders rotating Earth as a sphere made out of a wireframe, with exaggerated elevation data.
 *
 * This demonstrates that `VertexBuffers`s containing points can be either drawn as individual points or
 * as a continuous line according to the order of points.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val earth = loadImage("demo-data/images/nasa-blue-marble.png")
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val wireframe = heightMap
            .toColoredHeightPointCloud(
                colors = earth,
                preserveProportions = false, // important to keep
                heightScale = .1
            )
            .toColoredWireframe(heightMap.resolution)
        val style = shadeStyle {
            vertexPreamble = "const float PI = 3.14159265359;"
            vertexTransform = """
                float phi = a_position.x * PI * 2.0;
                float theta = a_position.y * PI;
                x_position = vec3(
                    sin(theta) * cos(phi),
                    sin(theta) * sin(phi),
                    cos(theta)
                ) * (1.0 + a_position.z);
                """.trimIndent()
            fragmentTransform = "x_fill.rgb = va_color.rgb;"
        }
        extend(Orbital()) {
            eye = Vector3.UNIT_Y * 1.6
            lookAt = Vector3.ZERO
            near = .001
            keySpeed = .01
        }
        extend {
            drawer.run {
                shadeStyle = style
                rotate(Vector3.UNIT_Z, seconds * 5.0)
                vertexBuffer(wireframe, DrawPrimitive.LINES)
            }
        }
    }
}
