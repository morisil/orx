import org.openrndr.application
import org.openrndr.draw.createEquivalent
import org.openrndr.draw.loadImage
import org.openrndr.extra.fx.distort.Lenses

fun main() = application {
    configure {
        width = 640
        height = 480

    }
    program {
        val image = loadImage("demo-data/images/image-001.png")
        val lenses = Lenses()
        val edges = image.createEquivalent()
        extend {
            lenses.rotation = 0.0
            lenses.scale = 1.5

            lenses.apply(image, edges)
            drawer.image(edges)
        }
    }
}