#version 430
//defines
layout(local_size_x = 16, local_size_y = 16) in;

uniform ivec2 resolution;
uniform ivec2 resolutionMinus1;

struct Point {
    vec3 position;
    float size;
    #ifdef COLORED
    vec4 color;
    #endif
};

struct Vertex {
    vec3 position;
    float weight;
    vec3 normal;
    float pad;
    #ifdef COLORED
    vec4 color;
    #endif
};

// input buffer for world positions
layout(std430, binding = 0) readonly restrict buffer pointCloud {
    Point points[];
};

// output buffer for vertex positions
layout(std430, binding = 1) writeonly restrict buffer mesh {
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
    int pointBase = coord.x + coord.y * resolution.x;

    Point p0 = points[pointBase];
    Point p1 = points[pointBase + 1];
    Point p2 = points[pointBase + 1 + resolution.x];
    Point p3 = points[pointBase + resolution.x];

    const int base = (coord.y * resolutionMinus1.x + coord.x) * 6;

    vec3 normal1 = calculateNormal(p0.position, p1.position, p3.position);

    vertices[base + 0].position = p0.position;
    vertices[base + 0].weight = p0.size;
    vertices[base + 0].normal = normal1;
    #ifdef COLORED
    vertices[base + 0].color = p0.color;
    #endif
    vertices[base + 1].position = p1.position;
    vertices[base + 1].weight = p1.size;
    vertices[base + 1].normal = normal1;
    #ifdef COLORED
    vertices[base + 1].color = p1.color;
    #endif
    vertices[base + 2].position = p3.position;
    vertices[base + 2].weight = p3.size;
    vertices[base + 2].normal = normal1;
    #ifdef COLORED
    vertices[base + 2].color = p3.color;
    #endif

    vec3 normal2 = calculateNormal(p1.position, p2.position, p3.position);

    vertices[base + 3].position = p1.position;
    vertices[base + 3].weight = p1.size;
    vertices[base + 3].normal = normal2;
    #ifdef COLORED
    vertices[base + 3].color = p1.color;
    #endif
    vertices[base + 4].position = p2.position;
    vertices[base + 4].weight = p2.size;
    vertices[base + 4].normal = normal2;
    #ifdef COLORED
    vertices[base + 4].color = p2.color;
    #endif
    vertices[base + 5].position = p3.position;
    vertices[base + 5].weight = p3.size;
    vertices[base + 5].normal = normal2;
    #ifdef COLORED
    vertices[base + 5].color = p3.color;
    #endif
}
