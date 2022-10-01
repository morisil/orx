import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.depth.camera.calibrator.*

// calibration with keyboard only
fun main() {
    application {
        configure {
            width = 1600
            height = 900
        }
        program {
            val image = loadImage("demo-data/images/image-001.png")
            val calibration = ParametrizedCalibration(
                inputResolution = resolution(image.width, image.height),
                outputResolution = resolution(width, height)
            ) { drawer, calibration ->
                // this code will be executed only if calibration is enabled
                // it can use the original image, filter it to add
                // markers, or like in this case, just put a calibration mask over it.
                // It is possible thanks to the fact that calibration overlay is also
                // immediately changing position of calibration image
                drawer.fill = ColorRGBa.PINK.opacify(.2)
                drawer.rectangle(calibration)
            }
            val calibrator = extend(Calibrator(calibration))
            extend {
                image.flipV = calibration.flipV
                // flipping horizontally would require custom
                // interpretation / implementation
                drawer.isolatedWithCalibration(calibration) {
                    image(image, calibration)
                }
            }
            // We need some signal to enable Calibrator extension.
            // It might be a mouse button or MIDI controller button.
            keyboard.keyDown.listen { event ->
                if (event.name == "c") {
                    calibrator.enabled = !calibrator.enabled
                }
            }
        }
    }
}
