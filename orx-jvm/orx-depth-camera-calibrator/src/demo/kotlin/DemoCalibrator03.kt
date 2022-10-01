import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.loadImage
import org.openrndr.extra.depth.camera.calibrator.*
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.BooleanParameter

// multiple calibrations
fun main() {
    application {
        configure {
            width = 1600
            height = 900
        }
        program {
            val image1 = loadImage("demo-data/images/peopleCity01.jpg")
            val calibration1 = ParametrizedCalibration(
                inputResolution = resolution(image1.width, image1.height),
                outputResolution = resolution(width, height)
            ) { drawer, calibration ->
                drawer.fill = ColorRGBa.PINK.opacify(.2)
                drawer.rectangle(calibration)
            }
            val image2 = loadImage("demo-data/images/vw-beetle.jpg")
            val calibration2 = ParametrizedCalibration(
                inputResolution = resolution(image2.width, image2.height),
                outputResolution = resolution(width, height),
            ) { drawer, calibration ->
                drawer.fill = ColorRGBa.PINK.opacify(.2)
                drawer.rectangle(calibration)
            }
            val calibrator = extend(
                Calibrator(calibration1, calibration2)
            )
            val gui = GUI()
            gui.add(object {
                @BooleanParameter(label = "enabled")
                var enable: Boolean
                    get() = calibrator.enabled
                    set(value) { calibrator.enabled = value }
            }, "calibration")
            gui.add(calibration1, label = "image1 calibration")
            gui.add(calibration2, label = "image2 calibration")
            extend(gui)
            extend(calibrator) {
                enabled = true
            }
            extend {
                drawer.isolatedWithCalibration(calibration1) {
                    image(image1, calibration1)
                }
                drawer.isolatedWithCalibration(calibration2) {
                    image(image2, calibration2)
                }
            }
        }
    }
}
