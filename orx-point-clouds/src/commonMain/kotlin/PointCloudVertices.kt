package org.openrndr.extra.pointclouds

import org.openrndr.draw.*
import org.openrndr.math.IntVector2

val pointCloudVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("size", VertexElementType.FLOAT32)
}

val coloredPointCloudVertexFormat: VertexFormat = vertexFormat {
    position(dimensions = 3)
    attribute("size", VertexElementType.FLOAT32)
    color(dimensions = 4)
}

fun pointCloudVertexBuffer(
    resolution: IntVector2
) = pointCloudVertexBuffer(size = resolution.x * resolution.y)

fun pointCloudVertexBuffer(
    size: Int
) = vertexBuffer(
    pointCloudVertexFormat,
    vertexCount = size
)

fun coloredPointCloudVertexBuffer(
    resolution: IntVector2
) = coloredPointCloudVertexBuffer(size = resolution.x * resolution.y)

fun coloredPointCloudVertexBuffer(
    size: Int
) = vertexBuffer(
    coloredPointCloudVertexFormat,
    vertexCount = size
)
