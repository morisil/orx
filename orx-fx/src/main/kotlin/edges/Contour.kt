package org.openrndr.extra.vfx

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter
import org.openrndr.draw.Shader
import org.openrndr.draw.filterShaderFromUrl
import org.openrndr.extra.fx.filterFragmentCode
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extra.parameters.IntParameter
import org.openrndr.resourceUrl

@Description("Contour")
class Contour : Filter(Shader.createFromCode(Filter.filterVertexCode, filterFragmentCode("edges/contour.frag"))) {
    @DoubleParameter("levels", 1.0, 16.0)
    var levels: Double by parameters


    @DoubleParameter("contour width", 0.0, 4.0)
    var contourWidth: Double by parameters

    @DoubleParameter("contour opacity", 0.0, 1.0)
    var contourOpacity: Double by parameters

    @DoubleParameter("background opacity", 0.0, 1.0)
    var backgroundOpacity: Double by parameters


    @ColorParameter("contour color")
    var contourColor: ColorRGBa by parameters


    init {
        levels = 6.0
        contourWidth = 0.4
        contourColor = ColorRGBa.BLACK
        backgroundOpacity = 1.0
        contourOpacity = 1.0
    }
}
