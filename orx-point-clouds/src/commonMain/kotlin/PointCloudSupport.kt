package org.openrndr.extra.pointclouds

import org.openrndr.draw.ColorBuffer
import org.openrndr.draw.ColorFormat
import org.openrndr.draw.ColorType
import org.openrndr.math.IntVector2

val ColorBuffer.resolution get() = IntVector2(width, height)

val IntVector2.resolutionSpec: String get() = "${x}x${y}"

val ColorBuffer.resolutionSpec: String get() = resolution.resolutionSpec

fun imageLayout(
    format: ColorFormat,
    type: ColorType
): String = imageLayout(format to type)

fun imageLayout(
    formatAndType: Pair<ColorFormat, ColorType>
): String {
    return imageLayoutMap[formatAndType] ?: error("unsupported layout: $formatAndType")
}

private val imageLayoutMap = mapOf(
    Pair(ColorFormat.R, ColorType.UINT8) to "r8",
    Pair(ColorFormat.R, ColorType.UINT8_INT) to "r8u",
    Pair(ColorFormat.R, ColorType.SINT8_INT) to "r8i",
    Pair(ColorFormat.R, ColorType.UINT16) to "r16",
    Pair(ColorFormat.R, ColorType.UINT16_INT) to "r16u",
    Pair(ColorFormat.R, ColorType.SINT16_INT) to "r16i",
    Pair(ColorFormat.R, ColorType.UINT32_INT) to "r32u",
    Pair(ColorFormat.R, ColorType.SINT32_INT) to "r32i",
    Pair(ColorFormat.R, ColorType.FLOAT16) to "r16f",
    Pair(ColorFormat.R, ColorType.FLOAT32) to "r32f",

    Pair(ColorFormat.RG, ColorType.UINT8) to "rg8",
    Pair(ColorFormat.RG, ColorType.UINT8_INT) to "rg8u",
    Pair(ColorFormat.RG, ColorType.SINT8_INT) to "rg8i",
    Pair(ColorFormat.RG, ColorType.UINT16) to "rg16",
    Pair(ColorFormat.RG, ColorType.UINT16_INT) to "rg16u",
    Pair(ColorFormat.RG, ColorType.SINT16_INT) to "rg16i",
    Pair(ColorFormat.RG, ColorType.FLOAT16) to "rg16f",
    Pair(ColorFormat.RG, ColorType.FLOAT32) to "rg32f",

    Pair(ColorFormat.RGBa, ColorType.UINT8) to "rgba8",
    Pair(ColorFormat.RGBa, ColorType.UINT8_INT) to "rgba8u",
    Pair(ColorFormat.RGBa, ColorType.SINT8_INT) to "rgba8i",
    Pair(ColorFormat.RGBa, ColorType.UINT16) to "rgba16",
    Pair(ColorFormat.RGBa, ColorType.UINT16_INT) to "rgba16u",
    Pair(ColorFormat.RGBa, ColorType.SINT16_INT) to "rgba16i",
    Pair(ColorFormat.RGBa, ColorType.FLOAT16) to "rgba16f",
    Pair(ColorFormat.RGBa, ColorType.FLOAT32) to "rgba32f"
)

@Suppress("NOTHING_TO_INLINE")
inline fun ColorBuffer.requireColorsResolutionMatch(
    colors: ColorBuffer,
    mapBufferName: String
) {
    require(resolution == colors.resolution) {
        "Resolution mismatch between $mapBufferName[${resolutionSpec}] and colors[${colors.resolutionSpec}]"
    }
}
