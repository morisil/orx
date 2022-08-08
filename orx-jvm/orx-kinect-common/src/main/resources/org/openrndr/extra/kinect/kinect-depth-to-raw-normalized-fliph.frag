#version 330

uniform usampler2D  tex0;             // kinect raw
uniform float       maxDepthValue;
uniform int         resolutionXMinus1;
out     float       outDepth;         // measured in meters

void main() {
    ivec2 uv = ivec2(gl_FragCoord);
    uv = ivec2(resolutionXMinus1 - uv.x, uv.y);
    uint uintDepth = texelFetch(tex0, uv, 0).r;
    float depth = float(uintDepth);
    outDepth = float(uintDepth) / maxDepthValue;
}