import org.openrndr.application
import org.openrndr.extra.color.colormaps.ColormapPhraseBook
import org.openrndr.extra.color.colormaps.spectralZucconi6
import org.openrndr.extra.noise.fastFloor
import kotlin.math.sin

fun main() = application {
    program {
        ColormapPhraseBook.register()
        extend {
            drawer.stroke = null
            val stripeCount = 32 + (sin(seconds) * 16.0).fastFloor()
            repeat(stripeCount) { i ->
                drawer.fill = spectralZucconi6(i / stripeCount.toDouble())
                drawer.rectangle(
                    x = i * width / stripeCount.toDouble(),
                    y = 0.0,
                    width = width / stripeCount.toDouble(),
                    height = height.toDouble(),
                )
            }
        }
    }
}
