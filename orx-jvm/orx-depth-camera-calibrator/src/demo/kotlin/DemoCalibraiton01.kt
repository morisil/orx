import org.openrndr.application
import org.openrndr.draw.colorBuffer
import org.openrndr.extra.depth.camera.calibrator.Calibrator
import org.openrndr.extra.depth.camera.calibrator.ParametrizedColorBufferCalibration
import org.openrndr.extra.depth.camera.calibrator.image
import org.openrndr.extra.depth.camera.calibrator.isolatedWithCalibration

fun main() {
    application {
        program {
            val buffer = colorBuffer(640, 480)
            val calibration = ParametrizedColorBufferCalibration(buffer)
            val calibrator = extend(Calibrator(calibration))
            extend {
                drawer.isolatedWithCalibration(calibration) {
                    image(buffer, calibration)
                }
            }
            keyboard.keyDown.listen { event ->
                if (event.name == "k") {
                    calibrator.enabled = !calibrator.enabled
                }
            }
        }
    }
}
