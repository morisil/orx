package org.openrndr.extra.meshgenerators

import org.openrndr.draw.*
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3

/**
 * Creates mesh out of this [VertexBuffer] representing an organized point cloud.
 *
 * @param resolution the resolution of the organized point cloud in this buffer.
 */
fun VertexBuffer.toMesh(
    resolution: IntVector2
): VertexBuffer {
    val mesh = meshVertexBuffer(resolution)
    PointCloudToMeshGenerator().use {
        it.populate(pointCloud = this, mesh, resolution)
    }
    return mesh
}

/**
 * Creates colored mesh out of this [VertexBuffer] representing an organized point cloud.
 *
 * @param resolution the resolution of the organized point cloud in this buffer.
 */
fun VertexBuffer.toColoredMesh(
    resolution: IntVector2
): VertexBuffer {
    val mesh = coloredMeshVertexBuffer(resolution)
    PointCloudToMeshGenerator(colored = true).use {
        it.populate(pointCloud = this, mesh, resolution)
    }
    return mesh
}

/**
 * Calculates a mesh out of the supplied organized point cloud.
 *
 * @param colored `true` if the color information should be applied to vertices,
 *      `false` otherwise (default).
 */
class PointCloudToMeshGenerator(
    colored: Boolean = false
) : AutoCloseable {

    private val shader = ComputeShader.fromCode(
        code = meshgenerators_point_cloud_to_mesh
            .replaceFirst(
                "//defines",
                if (colored) "#define COLORED" else ""
            ),
        name = "point-cloud-to-mesh"
    )

    // should match the size defined in the shader
    private val workGroupSize = IntVector3(16, 16, 1)

    /**
     * Calculates the [mesh] out of the organized [pointCloud].
     *
     * @param pointCloud the point cloud buffer.
     * @param mesh the mesh buffer.
     * @param resolution the resolution of the organized point cloud data.
     */
    fun populate(
        pointCloud: VertexBuffer,
        mesh: VertexBuffer,
        resolution: IntVector2
    ) {
        shader.run {
            uniform("resolution", resolution)
            uniform("resolutionMinus1", resolution - IntVector2.ONE)
            buffer("pointCloud", pointCloud)
            buffer("mesh", mesh)
            execute(
                computeShader2DExecuteSize(workGroupSize, resolution)
            )
        }
    }

    override fun close() {
        shader.destroy()
    }

}

val meshVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("weight", VertexElementType.FLOAT32)
    normal(dimensions = 3)
    padding(4)
}

val coloredMeshVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("weight", VertexElementType.FLOAT32)
    normal(dimensions = 3)
    padding(4)
    color(dimensions = 4)
}

fun meshVertexBuffer(
    resolution: IntVector2
): VertexBuffer = vertexBuffer(
    meshVertexFormat,
    vertexCount = (resolution.x - 1) * (resolution.y - 1) * 6
)

fun coloredMeshVertexBuffer(
    resolution: IntVector2
): VertexBuffer = vertexBuffer(
    coloredMeshVertexFormat,
    vertexCount = (resolution.x - 1) * (resolution.y - 1) * 6
)
