package org.openrndr.extra.computeshaders

import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.openrndr.math.IntVector2
import org.openrndr.math.IntVector3
import kotlin.test.Test

class TestComputeShaders {

    @Test
    fun testComputeShaderExecuteDimensions() {

        computeShaderExecuteDimensions(
            resolution = IntVector2(639, 480),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBeEqualTo IntVector3(80, 60, 1)

        computeShaderExecuteDimensions(
            resolution = IntVector2(640, 480),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBeEqualTo IntVector3(80, 60, 1)

        computeShaderExecuteDimensions(
            resolution = IntVector2(641, 480),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBeEqualTo IntVector3(81, 60, 1)

        computeShaderExecuteDimensions(
            resolution = IntVector2(641, 481),
            localSizeX = 8,
            localSizeY = 8
        ) shouldBeEqualTo IntVector3(81, 61, 1)

    }

    @Test
    fun replaceVersion() {
        "foo".appendAfterVersion("bar") shouldBe "foo"
        "#version 430".appendAfterVersion("#define FOO") shouldBeEqualTo """
            #version 430
            #define FOO
            
            """.trimIndent()
    }

    @Test
    fun testIntVector2ResolutionSpec() {
        IntVector2(640, 480).resolutionSpec shouldBeEqualTo "640x480"
    }

}
