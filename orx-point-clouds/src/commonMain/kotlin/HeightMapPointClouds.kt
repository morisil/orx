package org.openrndr.extra.pointclouds

import org.openrndr.draw.*
import org.openrndr.draw.font.BufferAccess
import org.openrndr.math.IntVector3
import org.openrndr.math.Vector2

/**
 * Transforms this [ColorBuffer] storing a height map in the RED channel to organized point cloud.
 *
 * In an organized point cloud the layout of `XY` coordinates
 * is preserved in the layout of points in the cloud, and the `Z` coordinate is extruded.
 *
 * @param preserveProportions If `true` (default) it will preserve the original proportions of
 *      the supplied height map image, centering the resulting point cloud
 *      in point `[0, 0, 0]` and normalizing the width in the `-1..1` range. When set to `false`,
 *      the `XY` coordinates of the resulting point cloud will be normalized in the `0..1` range.
 * @param heightScale How much height should be expressed on the `Z`-axis.
 * @return the point cloud.
 * @see HeightMapToPointCloudGenerator.populate
 * @see pointCloudVertexBuffer
 */
fun ColorBuffer.toHeightPointCloud(
    preserveProportions: Boolean = true,
    heightScale: Double = 1.0
): VertexBuffer {
    val pointCloud = pointCloudVertexBuffer(resolution)
    HeightMapToPointCloudGenerator(
        heightMapFormat = format,
        heightMapType = type,
        preserveProportions,
        heightScale
    ).use {
        it.populate(heightMap = this, pointCloud)
    }
    return pointCloud
}

/**
 * Generates an organized point cloud out of a height map.
 *
 * In an organized point cloud the layout of `XY` coordinates
 * is preserved in the layout of points in the cloud, and the `Z` coordinate is extruded.
 *
 * @param heightMapFormat the color format of the height map.
 * @param heightMapType the color type of the height map.
 * @param preserveProportions If `true` (default) it will preserve the original proportions of
 *      the supplied height map image, centering the resulting point cloud
 *      in point `[0, 0, 0]` and normalizing the width in the `-1..1` range. When set to `false`,
 *      the `XY` coordinates of the resulting point cloud will be normalized in the `0..1` range.
 * @param heightScale How much height should be expressed on the `Z`-axis.
 */
class HeightMapToPointCloudGenerator(
    heightMapFormat: ColorFormat,
    heightMapType: ColorType,
    private val preserveProportions: Boolean = true,
    var heightScale: Double = 1.0
) : AutoCloseable {

    private val shader = ComputeShader.fromCode(
        code = pointclouds_height_map_to_point_cloud
            .replaceFirst("//defines", if (preserveProportions) "#define PRESERVE_PROPORTIONS" else "")
            .replaceFirst("heightMapImageLayout", imageLayout(heightMapFormat, heightMapType)),
        name = "height-map-to-point-cloud"
    )

    // should match the size defined in the shader
    private val workGroupSize = IntVector3(16, 16, 1)

    /**
     * Populates the [pointCloud] with the organized point cloud data stored in the [heightMap].
     *
     * Note: this function is intended for continuous writes of changing data to allocated point
     * cloud [VertexBuffer]. For one time generation shortcut see [toHeightPointCloud].
     *
     * @param heightMap an image where the RED channel encodes height.
     * @param pointCloud the point cloud buffer to write to.
     * @see pointCloudVertexBuffer
     */
    fun populate(
        heightMap: ColorBuffer,
        pointCloud: VertexBuffer,
    ) {
        shader.setUniforms(
            pointCloud,
            heightMap,
            heightScale,
            preserveProportions
        )
        shader.execute(
            computeShader2DExecuteSize(workGroupSize, heightMap.resolution)
        )
    }

    override fun close() {
        shader.destroy()
    }

}

/**
 * Transforms this [ColorBuffer] storing a height map in the RED channel to organized point
 * cloud with corresponding [colors].
 *
 * In an organized point cloud the layout of `XY` coordinates
 * is preserved in the layout of points in the cloud, and the `Z` coordinate is extruded.
 *
 * @param colors the [ColorBuffer] of a matching resolution storing color information for each point.
 * @param preserveProportions If `true` (default) it will preserve the original proportions of
 *      the supplied height map image, centering the resulting point cloud
 *      in point `[0, 0, 0]` and normalizing the width in the `-1..1` range. When set to `false`,
 *      the `XY` coordinates of the resulting point cloud will be normalized in the `0..1` range.
 * @param heightScale How much height should be expressed on the `Z`-axis.
 * @return the point cloud.
 * @throws IllegalArgumentException if the resolution of this height map and [colors] differ.
 *
 * @see ColoredHeightMapToPointCloudGenerator.populate
 * @see coloredPointCloudVertexBuffer
 */
fun ColorBuffer.toColoredHeightPointCloud(
    colors: ColorBuffer,
    preserveProportions: Boolean = true,
    heightScale: Double = 1.0
): VertexBuffer {
    val pointCloud = coloredPointCloudVertexBuffer(resolution)
    ColoredHeightMapToPointCloudGenerator(
        heightMapFormat = format,
        heightMapType = type,
        colorsFormat = colors.format,
        colorsType = colors.type,
        preserveProportions,
        heightScale
    ).use {
        it.populate(heightMap = this, pointCloud, colors)
    }
    return pointCloud
}

/**
 * Generates an organized point cloud out of a height map and color map.
 *
 * In an organized point cloud the layout of `XY` coordinates
 * is preserved in the layout of points in the cloud, and the `Z` coordinate is extruded.
 *
 * @param heightMapFormat the color format of the height map.
 * @param heightMapType the color type of the height map.
 * @param colorsFormat the color format of the color map.
 * @param colorsType the color type of the color map.
 * @param preserveProportions If `true` (default) it will preserve the original proportions of
 *      the supplied height map image, centering the resulting point cloud
 *      in point `[0, 0, 0]` and normalizing the width in the `-1..1` range. When set to `false`,
 *      the `XY` coordinates of the resulting point cloud will be normalized in the `0..1` range.
 * @param heightScale How much height should be expressed on the `Z`-axis.
 */
class ColoredHeightMapToPointCloudGenerator(
    heightMapFormat: ColorFormat,
    heightMapType: ColorType,
    colorsFormat: ColorFormat,
    colorsType: ColorType,
    val preserveProportions: Boolean = true,
    var heightScale: Double = 1.0
) : AutoCloseable {

    private val shader = ComputeShader.fromCode(
        code = pointclouds_height_map_to_point_cloud.replaceFirst(
            "//defines",
            "#define COLORED\n" +
                    if (preserveProportions) "#define PRESERVE_PROPORTIONS" else ""
        ).replaceFirst(
            "heightMapImageLayout",
            imageLayout(heightMapFormat, heightMapType)
        ).replaceFirst(
            "colorsImageLayout",
            imageLayout(colorsFormat, colorsType)
        ),
        name = "colored-height-map-to-point-cloud"
    )

    // should match the size defined in the shader
    private val workGroupSize = IntVector3(16, 16, 1)

    /**
     * Populates the [pointCloud] with the organized point cloud data stored in the [heightMap] together with
     * corresponding [colors].
     *
     * Note: this function is intended for continuous writes of changing data to allocated point
     * cloud [VertexBuffer]. For one time generation shortcut see [toColoredHeightPointCloud].
     *
     * @param heightMap an image where the RED channel encodes height.
     * @param pointCloud the point cloud buffer to write to.
     * @param colors the colors to apply to corresponding points.
     * @throws IllegalArgumentException if the resolution of the [heightMap] and [colors] differ.
     *
     * @see coloredPointCloudVertexBuffer
     */
    fun populate(
        heightMap: ColorBuffer,
        pointCloud: VertexBuffer,
        colors: ColorBuffer
    ) {
        heightMap.requireColorsResolutionMatch(colors, "heightMap")
        shader.run {
            setUniforms(
                pointCloud,
                heightMap,
                heightScale,
                preserveProportions
            )
            image(
                "colors",
                1,
                colors.imageBinding(imageAccess = ImageAccess.READ)
            )
            execute(computeShader2DExecuteSize(workGroupSize, heightMap.resolution))
        }

    }

    override fun close() {
        shader.destroy()
    }

}

private fun ComputeShader.setUniforms(
    pointCloud: VertexBuffer,
    heightMap: ColorBuffer,
    heightScale: Double,
    preserveProportions: Boolean
) {
    val resolution = heightMap.resolution
    val floatResolution = heightMap.resolution.vector2

    uniform("resolution", resolution)
    uniform("floatResolution", floatResolution)
    uniform("heightScale", heightScale)

    if (preserveProportions) {
        val scale = Vector2(1.0, floatResolution.y / floatResolution.x)
        val offset = Vector2(-.5, -scale.y * .5)
        uniform("scale", scale)
        uniform("offset", offset)
    }

    image(
        "heightMap",
        0,
        heightMap.imageBinding(imageAccess = BufferAccess.READ)
    )

    buffer("pointCloud", pointCloud)
}
