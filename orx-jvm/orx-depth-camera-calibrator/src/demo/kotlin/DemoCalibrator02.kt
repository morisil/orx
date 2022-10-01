import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.depth.camera.calibrator.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.BooleanParameter

// calibration with keyboard and GUI
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
                drawer.fill = ColorRGBa.PINK.opacify(.2)
                drawer.rectangle(calibration)
            }
            val calibrator = Calibrator(calibration)
            val gui = GUI()
            gui.add(object {
                @BooleanParameter(label = "enabled")
                var enable: Boolean
                    get() = calibrator.enabled
                    set(value) { calibrator.enabled = value }
            }, "calibrator")
            gui.add(calibration, label = "image calibration")
            extend(gui)
            // remember to extend with calibrator after extending with gui
            // otherwise calibrator view will overlay gui
            extend(calibrator) {
                enabled = true
            }
            extend {
                image.flipV = calibration.flipV
                drawer.isolatedWithCalibration(calibration) {
                    image(image, calibration)
                }
            }
        }
    }
}
