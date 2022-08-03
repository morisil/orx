#version 330

uniform usampler2D  tex0;             // kinect raw
uniform int         resolutionXMinus1;
out     float       outDepth;         // measured in meters

const uint  UINT_MAX_KINECT_DEPTH = 2047u;

void main() {
    ivec2 uv = ivec2(gl_FragCoord);
    uv = ivec2(resolutionXMinus1 - uv.x, uv.y);
    uint uintDepth = texelFetch(tex0, uv, 0).r;
    float depth = float(uintDepth);
    outDepth = (uintDepth < UINT_MAX_KINECT_DEPTH)
        ? 1.0 / (depth * -0.0030711016 + 3.3309495161)
        : 0.0;
}
