package org.openrndr.extra.pointclouds

import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.extra.computeshaders.appendAfterVersion
import org.openrndr.extra.computeshaders.resolution
import org.openrndr.extra.computeshaders.resolutionSpec
import org.openrndr.math.Vector3

/**
 * Generates an organized point cloud out of a depth map.
 * In an organized point cloud the layout of `XY` coordinates is preserved
 * in the layout of points in the cloud (no cross-sections).
 */
class DepthMapToPointCloudGenerator(
    private val intrinsics: DepthMapIntrinsicParameters
) {

    private val shader = ComputeShader.fromCode(
        code = pointclouds_depth_map_to_point_cloud,
        name = "height-map-to-point-cloud"
    ).apply {
        setIntrinsics(intrinsics)
    }

    /**
     * Populates [VertexBuffer] with organized point cloud data.
     *
     * Note: this function is intended for continuous writes of changing data to allocated point
     * cloud [VertexBuffer]. For one time generation shortcut see [generate].
     *
     * @param pointCloud the point cloud buffer to write to.
     * @param depthMap an image where the RED channel encodes depth.
     * @see generate
     * @see pointCloudVertexBuffer
     */
    fun populate(
        pointCloud: VertexBuffer,
        depthMap: ColorBuffer
    ) {
        shader.setUniforms(
            pointCloud,
            depthMap
        )
        shader.execute2D(depthMap.resolution)
    }

    /**
     * Generates ordered point cloud and returns corresponding [VertexBuffer].
     *
     * @param depthMap an image where the RED channel encodes depth.
     * @return the generated point cloud buffer.
     * @see populate
     * @see pointCloudVertexBuffer
     */
    fun generate(
        depthMap: ColorBuffer,
    ): VertexBuffer = pointCloudVertexBuffer(
        depthMap.resolution
    ).also {
        populate(it, depthMap)
    }

}

/**
 * Generates an organized point cloud out of a depth map and associated color information.
 * In an organized point cloud the layout of `XY` coordinates is preserved
 * in the layout of points in the cloud (no cross-sections).
 */
class ColoredDepthMapToPointCloudGenerator(
    private val intrinsics: DepthMapIntrinsicParameters
) {

    private val shader = ComputeShader.fromCode(
        code = pointclouds_depth_map_to_point_cloud
            .appendAfterVersion("#define COLORED"),
        name = "colored-height-map-to-point-cloud"
    ).apply {
        setIntrinsics(intrinsics)
    }

    /**
     * Populates [VertexBuffer] with organized point cloud data.
     *
     * Note: this function is intended for continuous writes of changing data to allocated point
     * cloud [VertexBuffer]. For one time generation shortcut see [generate].
     *
     * @param pointCloud the point cloud buffer to write to.
     * @param depthMap an image where the RED channel encodes depth.
     * @param colors an image of the same resolution as the [depthMap] holding color information.
     * @see generate
     * @see pointCloudVertexBuffer
     */
    fun populate(
        pointCloud: VertexBuffer,
        depthMap: ColorBuffer,
        colors: ColorBuffer
    ) {
        checkSizeMatch(depthMap, colors)
        doPopulate(pointCloud, depthMap, colors)
    }

    /**
     * Generates ordered point cloud and returns corresponding [VertexBuffer].
     *
     * @param depthMap an image where the RED channel encodes depth.
     * @param colors an image of the same resolution as the [depthMap] holding color information.
     * @return the generated point cloud buffer.
     * @see populate
     * @see pointCloudVertexBuffer
     */
    fun generate(
        depthMap: ColorBuffer,
        colors: ColorBuffer,
    ): VertexBuffer {
        checkSizeMatch(depthMap, colors)
        val pointCloud = coloredPointCloudVertexBuffer(
            depthMap.resolution
        )
        doPopulate(pointCloud, depthMap, colors)
        return pointCloud
    }

    private fun doPopulate(
        pointCloud: VertexBuffer,
        depthMap: ColorBuffer,
        colors: ColorBuffer
    ) {
        shader.setUniforms(
            pointCloud,
            depthMap
        )
        shader.image(
            "colors",
            1,
            colors.imageBinding(imageAccess = ImageAccess.READ)
        )
        shader.execute2D(depthMap.resolution)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun checkSizeMatch(
        depthMap: ColorBuffer,
        colors: ColorBuffer
    ) {
        check(depthMap.resolution == colors.resolution) {
            "Resolution mismatch between depthMap[${depthMap.resolutionSpec}] and colors[${colors.resolutionSpec}]"
        }
    }

}

data class DepthMapIntrinsicParameters(
    val fxD: Double,
    val fyD: Double,
    val cxD: Double,
    val cyD: Double,
    val spaceShift: Vector3
)

private fun ComputeShader.setUniforms(
    pointCloud: VertexBuffer,
    depthMap: ColorBuffer,
) {
    val resolution = depthMap.resolution
    val floatResolution = depthMap.resolution.vector2
    uniform("resolution", resolution)
    uniform("floatResolution", floatResolution)
    image(
        "depthMap",
        0,
        depthMap.imageBinding(imageAccess = BufferAccess.READ)
    )
    buffer("pointCloud", pointCloud)
}

private fun ComputeShader.setIntrinsics(
    intrinsics: DepthMapIntrinsicParameters
) {
    uniform("fxD", intrinsics.fxD)
    uniform("fyD", intrinsics.fyD)
    uniform("cxD", intrinsics.cxD)
    uniform("cyD", intrinsics.cyD)
}
