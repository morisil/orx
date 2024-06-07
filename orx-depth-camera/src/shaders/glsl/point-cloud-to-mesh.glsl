#version 430

layout (local_size_x = 8, local_size_y = 8) in;

uniform int resolutionX;
uniform ivec2 resolutionMinus1;
uniform vec3 eye;

struct Vertex {
    vec4 position;
    vec3 normal;
    float pad;
};

// input buffer for world positions
layout (std430, binding = 0) buffer pointCloud {
    vec4 points[];
};

// output buffer for vertex positions
layout (std430, binding = 1) buffer mesh {
    Vertex vertices[];
};

vec3 calculateNormal(in vec3 p0, in vec3 p1, in vec3 p2) {
    vec3 edge1 = p1 - p0;
    vec3 edge2 = p2 - p0;
    return normalize(cross(edge1, edge2));
}

void main() {
    const ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if ((coord.x >= resolutionMinus1.x) || (coord.y >= resolutionMinus1.y)) {
        return;
    }
    int pointBase = coord.x + coord.y * resolutionX;

    vec4 p0 = points[pointBase];
    vec4 p1 = points[pointBase + 1];
    vec4 p2 = points[pointBase + 1 + resolutionX];
    vec4 p3 = points[pointBase + resolutionX];

//    int selector = int(p0.w) + int(p1.w) * 10 + int(p2.w) * 100 + int(p3.w) * 1000;
//
//    switch (selector) {
//            case 0000: break;
//            case 0001: p1 = p0; p2 = p0; p3 = p0; break;
//            case 0010: p0 = p1;
//            case 0011: p0 = p1;
//            case 0100: break;
//            case 0101: break;
//            case 0110: break;
//            case 0111: break;
//            case 1000: break;
//            case 1001: break;
//            case 1010: break;
//            case 1011: break;
//            case 1100: break;
//            case 1101: break;
//            case 1110: break;
//            case 1111: break;
//    }

    /*


10
00

11
11



01
11

p0 = p1

10
11

p1 = middle

11
01

p2 = middle

11
10



00
11

01
01


00
01

11
10

11
00

10
10

10
00

00
00

*/


//    if (p1.w == 0.0) {
//        p1 = vec4(0, 0, 4.0, 0.0);
//    }
//    if (p2.w == 0.0) {
//        p2 = vec4(0, 0, 4.0, 0.0);
//    }
//    if (p2.w == 0.0) {
//        p3 = vec4(0, 0, 4.0, 0.0);
//    }

    const int base = (coord.y * resolutionMinus1.x + coord.x) * 6;

    vec3 normal = calculateNormal(p0.xyz, p1.xyz, p3.xyz);

    vertices[base + 0].position = p0;
    vertices[base + 0].normal = normal;
    vertices[base + 1].position = p1;
    vertices[base + 1].normal = normal;
    vertices[base + 2].position = p3;
    vertices[base + 2].normal = normal;

    normal = calculateNormal(p1.xyz, p2.xyz, p3.xyz);

    vertices[base + 3].position = p1;
    vertices[base + 3].normal = normal;
    vertices[base + 4].position = p2;
    vertices[base + 4].normal = normal;
    vertices[base + 5].position = p3;
    vertices[base + 5].normal = normal;
}
