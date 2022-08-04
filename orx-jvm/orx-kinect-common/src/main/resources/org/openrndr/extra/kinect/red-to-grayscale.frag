#version 330

#ifdef OR_IN_OUT
in vec2 v_texCoord0;
#else
varying vec2 v_texCoord0;
#endif

uniform sampler2D   tex0;
uniform float       minValue;
uniform float       maxValue;

#ifndef OR_GL_FRAGCOLOR
out vec4 o_color;
#endif

void main() {
    #ifndef OR_GL_TEXTURE2D
    float red = texture(tex0, v_texCoord0).r;
    #else
    float red = texture2D(tex0, v_texCoord0).r;
    #endif
    float value = (red - minValue) / (maxValue - minValue);
    vec4 result = vec4(vec3(value), 1.);
    #ifdef OR_GL_FRAGCOLOR
    gl_FragColor = result;
    #else
    o_color = result;
    #endif
}
