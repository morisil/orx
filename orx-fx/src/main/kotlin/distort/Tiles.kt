package org.openrndr.extra.fx.distort

import org.openrndr.draw.*
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter

@Description("Tiles")
class Tiles : Filter(Shader.createFromCode(filterVertexCode, filterFragmentCode("distort/tiles.frag"))) {

    @DoubleParameter("rotation", -180.0, 180.0, order = 2)
    var rotation: Double by parameters

    @IntParameter("x segments", 0, 256, order = 0)
    var xSegments: Int by parameters

    @IntParameter("y segments", 0, 256, order = 0)
    var ySegments: Int by parameters



    init {
        rotation = 0.0
        xSegments = 32
        ySegments = 32
    }

    var bicubicFiltering = false
    override fun apply(source: Array<ColorBuffer>, target: Array<ColorBuffer>) {
        if (bicubicFiltering && source.isNotEmpty()) {
            source[0].generateMipmaps()
            source[0].filter(MinifyingFilter.LINEAR_MIPMAP_LINEAR, MagnifyingFilter.LINEAR)
        }
        super.apply(source, target)
    }
}
