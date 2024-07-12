package org.openrndr.extra.wireframes

import org.openrndr.draw.*
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3

/**
 * Transforms this point cloud [VertexBuffer] to wireframe [VertexBuffer] storing lines connecting points.
 *
 * @param resolution the resolution of organized point cloud kept in this [VertexBuffer].
 */
fun VertexBuffer.toWireframe(
    resolution: IntVector2
): VertexBuffer {
    val wireframe = wireframeVertexBuffer(resolution)
    PointCloudToWireframeGenerator().use {
        it.populate(pointCloud = this, wireframe, resolution)
    }
    return wireframe
}

/**
 * Transforms this colored point cloud [VertexBuffer] to wireframe [VertexBuffer] storing colored lines
 * connecting points.
 *
 * @param resolution the resolution of organized point cloud kept in this [VertexBuffer].
 */
fun VertexBuffer.toColoredWireframe(
    resolution: IntVector2
): VertexBuffer {
    val wireframe = coloredWireframeVertexBuffer(resolution)
    PointCloudToWireframeGenerator(colored = true).use {
        it.populate(pointCloud = this, wireframe, resolution)
    }
    return wireframe
}

/**
 * Generates wireframe out of supplied point cloud.
 *
 * @param colored `true` if the point cloud / wireframe vertices contain color attribute, `false` by default.
 */
class PointCloudToWireframeGenerator(
    colored: Boolean = false
) : AutoCloseable {

    private val shader = ComputeShader.fromCode(
        code = wireframes_point_cloud_to_wireframe
            .replaceFirst(
                "//defines",
                if (colored) "#define COLORED" else ""
            ),
        name = "point-cloud-to-wireframe"
    )

    // should match the size defined in the shader
    private val workGroupSize = IntVector3(16, 16, 1)

    /**
     * Populates the [wireframe] based on the [pointCloud] data by connecting the points with the grid of lines.
     *
     * @param pointCloud the point cloud to read organized point cloud from.
     * @param wireframe the wireframe buffer to write lines to.
     * @param resolution the resolution of organized point cloud kept in the [pointCloud].
     */
    fun populate(
        pointCloud: VertexBuffer,
        wireframe: VertexBuffer,
        resolution: IntVector2
    ) {
        shader.run {
            uniform("resolution", resolution)
            uniform("resolutionMinus1", resolution - IntVector2.ONE)
            buffer("pointCloud", pointCloud)
            buffer("wireframe", wireframe)
            execute(
                computeShader2DExecuteSize(workGroupSize, resolution)
            )
        }
    }

    override fun close() {
        shader.destroy()
    }

}
