import org.openrndr.WindowMultisample
import org.openrndr.application
import org.openrndr.draw.DrawPrimitive
import org.openrndr.draw.loadImage
import org.openrndr.draw.shadeStyle
import org.openrndr.extra.camera.Orbital
import org.openrndr.extra.meshgenerators.toColoredMesh
import org.openrndr.extra.pointclouds.resolution
import org.openrndr.extra.pointclouds.toColoredHeightPointCloud
import org.openrndr.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Presents organized and colored point cloud as a colored mesh.
 */
fun main() = application {
    configure {
        multisample = WindowMultisample.SampleCount(4) // makes lines smoother
    }
    program {
        val heightMap = loadImage("demo-data/images/nasa-blue-marble-height-map.png")
        val earth = loadImage("demo-data/images/nasa-blue-marble.png")
        val mesh = heightMap
            .toColoredHeightPointCloud(
                colors = earth,
                heightScale = .02
            )
            .toColoredMesh(heightMap.resolution)
        val style = shadeStyle {
            fragmentTransform = """
                vec3 lightDir = normalize(p_lightPosition);
                float luma = dot(va_normal, lightDir) * 0.6 + .4;
                x_fill.rgb = va_color.rgb * luma;
            """.trimIndent()
        }
        extend(Orbital()) {
            eye = Vector3(0.03, 0.03, .3)
            lookAt = Vector3.ZERO
            near = .001
            keySpeed = .01
        }
        extend {
            drawer.shadeStyle = style
            style.parameter("lightPosition", Vector3(
                sin(seconds) * 1.0,
                cos(seconds) * 1.0,
                1.0)
            )
            drawer.vertexBuffer(mesh, DrawPrimitive.TRIANGLES)
        }
    }
}
