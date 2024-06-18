#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform ivec2 resolution;
uniform vec2 floatResolution;

uniform float fxD;
uniform float fyD;
uniform float cxD;
uniform float cyD;
uniform vec3 spaceShift;

layout(rgba8, binding = 0) uniform readonly image2D depthMap;
#ifdef COLORED
layout(rgba8, binding = 1) uniform readonly image2D colors;
#endif

uniform float heightScale;

struct Point {
    vec3 position;
    float size;
    #ifdef COLORED
    vec4 color;
    #endif
};

layout(binding = 2) buffer pointCloud {
    Point points[];
};

vec3 depthToWorld(in vec2 uv, in float distance) {
    return
        vec3(
            (uv.x - cxD) * distance * fxD,
            (uv.y - cyD) * distance * fyD,
            distance
        ) + spaceShift;
}

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if (coord.x >= resolution.x || coord.y >= resolution.y) {
        return;
    }

    float distance = imageLoad(depthMap, coord).r;
    vec2 floatCoord = vec2(coord);

    int index = coord.y * resolution.x + coord.x;
    points[index].position = depthToWorld(floatCoord, distance);
    points[index].size = distance;

    #ifdef COLORED
    points[index].color = imageLoad(colors, coord);
    #endif
}
