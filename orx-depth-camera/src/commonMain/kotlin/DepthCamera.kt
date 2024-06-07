package org.openrndr.extra.depth.camera

import kotlinx.coroutines.flow.Flow
import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.VertexBuffer
import org.openrndr.math.IntVector2
import org.openrndr.math.Matrix44
import org.openrndr.utils.buffer.MPPBuffer

/**
 * Defines how pixel values encoded in depth [ColorBuffer] will be interpreted.
 */
enum class DepthMeasurement {

    /**
     * Raw values, but normalized to the range 0-1.
     * Useful for debugging, because full range of captured values can be rendered
     * as a texture. Therefore, it's a default setting.
     */
    RAW_NORMALIZED,

    /**
     * Raw values, exactly as they are provided by the device.
     * Note: it might imply that [ColorBuffer] of the depth camera frame
     * is provided in integer-based format (for example in case of Kinect devices).
     */
    RAW,

    /**
     * Expressed in meters.
     * It is using floating point numbers.
     * Note: values above `1.0` will not be visible if displayed as a texture.
     */
    METERS

}

/**
 * General API of any depth camera.
 */
interface DepthCamera {

    /**
     * Current operating resolution.
     */
    val resolution: IntVector2

    /**
     * The units/mapping in which depth is expressed on [depthImage].
     */
    var depthMeasurement: DepthMeasurement

    /**
     * Flips source depth data image in horizontal axis (mirror).
     */
    var flipH: Boolean

    /**
     * Flips source depth data image in vertical axis (upside-down).
     */
    var flipV: Boolean

    /**
     * Min depth measured by
     */
    var minDepth: Double

    var maxDepth: Double

    var minX: Double

    var maxX: Double

    var minY: Double

    var maxY: Double

    var minZ: Double

    var maxZ: Double

    // TODO how to call it?
    var transformationMatrix: Matrix44

    val render: SpaceRender

    val renders: Flow<SpaceRender>

    /**
     * The flow of the most recent raw depth data received from the depth camera.
     *
     * This information can be used to stream the data, for with OSC protocol.
     */
    val rawDepthData: Flow<MPPBuffer>

}

interface SpaceRender {

    /**
     * The most recent depth image frame received from the [DepthCamera].
     */
    val depthImage: ColorBuffer

    /**
     * Point cloud vertex buffer.
     */
    val pointCloud: VertexBuffer

    val spaceMesh: VertexBuffer

}

