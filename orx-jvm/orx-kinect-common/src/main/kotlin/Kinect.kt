package org.openrndr.extra.kinect

import org.openrndr.draw.*
import org.openrndr.extra.depth.camera.DepthCamera
import org.openrndr.math.IntVector2
import org.openrndr.resourceUrl
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Represents all the accessible kinects handled by a specific driver (V1, V2, etc.).
 */
interface Kinect {

    /**
     * Lists available kinect devices.
     */
    fun listDevices(): List<Device.Info>

    /**
     * Starts kinect device of a given index.
     *
     * @param index the kinect device index (starts with 0). If no value specified,
     *          it will default to 0.
     * @throws KinectException if device of such an index does not exist,
     *          or it was already started.
     * @see listDevices
     */
    fun openDevice(index: Int = 0): Device

    /**
     * Starts kinect device of a given serial number.
     *
     * @param serialNumber the kinect device serialNumber.
     * @throws KinectException if device of such a serial number does not exist
     *          , or it was already started.
     * @see listDevices
     */
    fun openDevice(serialNumber: String): Device

    /**
     * The list of kinect devices which are already opened and haven't been closed.
     */
    val activeDevices: List<Device>

    /**
     * Represents physical kinect device.
     */
    interface Device {

        /**
         * Provides information about kinect device.
         *
         * Note: in implementation it can be extended with any
         * additional information next to the serial number.
         */
        interface Info {
            val serialNumber: String
        }

        val info: Info

        val depthCamera: KinectDepthCamera

        fun close()

    }

}

/**
 * Generic interface for all the kinect cameras.
 */
interface KinectCamera {

    var enabled: Boolean

}

interface KinectDepthCamera : KinectCamera, DepthCamera {
    /* no special attributes at the moment */
}

open class KinectException(msg: String) : RuntimeException(msg)

fun kinectRawDepthByteBuffer(resolution: IntVector2): ByteBuffer =
    ByteBuffer.allocateDirect(
        resolution.x * resolution.y * 2
    ).also {
        it.order(ByteOrder.nativeOrder())
    }

class KinectDepthToRawNormalized : Filter(
    filterShaderFromUrl(
        resourceUrl(
            "kinect-depth-to-raw-normalized.frag",
            Kinect::class
        )
    )
) {

    /** 2047 for kinect 1, 4095 for kinect 2 */
    var maxDepthValue: Double by parameters

}

abstract class RedToColormap(shader: String) : Filter(
    filterShaderFromUrl(resourceUrl(shader, Kinect::class))
) {
    var minValue: Double by parameters
    var maxValue: Double by parameters
    var curve: Double by parameters
    init {
        minValue = 0.0
        maxValue = 1.0
        curve = 1.0
    }
}

/**
 * Maps depth values to grayscale.
 */
class RedToGrayscale : RedToColormap("red-to-grayscale.frag")

/**
 * Maps depth values to color map according to natural light dispersion as described
 * by Alan Zucconi in the
 * [Improving the Rainbow](https://www.alanzucconi.com/2017/07/15/improving-the-rainbow/)
 * article.
 */
class RedToSpectralZucconi : RedToColormap("red-to-spectral-zucconi.frag")

/**
 * Maps depth values to color map according to
 * [Turbo, An Improved Rainbow Colormap for Visualization](https://ai.googleblog.com/2019/08/turbo-improved-rainbow-colormap-for.html)
 * by Google.
 */
class RedToTurboColormap : RedToColormap("red-to-turbo-colormap.frag")
