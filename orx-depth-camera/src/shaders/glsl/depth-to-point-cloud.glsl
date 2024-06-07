#version 430

layout (local_size_x = 8, local_size_y = 8) in;

layout(r16ui) uniform uimage2D    rawDisparityImage;             // kinect raw
uniform int         resolutionX;
uniform int resolutionY;
//#ifdef KINECT_FLIPH
uniform int         resolutionXMinus1;
//#endif
uniform vec2 resolutionFloat;
uniform float maxDistance;

uniform float fxD;
uniform float fyD;
uniform float cxD;
uniform float cyD;

// output point cloud buffer
layout (std430, binding = 0) buffer pointCloud {
    vec4 points[];
};

const float minDistanceVector = -10.0;
const float scaleFactor = .0021;

const uint UINT_MAX_KINECT_DEPTH = 2047u;


// https://github.com/shiffman/OpenKinect-for-Processing/blob/master/OpenKinect-Processing/examples/Kinect_v1/PointCloud/PointCloud.pde

vec3 depthToWorld(in vec2 uv, in float distance) {
    return vec3(
      (uv.x - cxD) * distance * fxD,
      (uv.y - cyD) * distance * fyD,
      distance
    ) - vec3(0.0, .3, 0.0);
}

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    if (coord.x >= resolutionX || coord.y >= resolutionY) {
        return;
    }
    ivec2 imageCoord = ivec2(
       639 - coord.x,
//        639 - coord.x,
        479 - coord.y
    );
    #ifdef KINECT_FLIPH
    //coord = ivec2(resolutionXMinus1 - coord.x, coord.y);
    #endif
    uint uintDepth = imageLoad(rawDisparityImage, imageCoord).r;
    float rawDisparity = float(uintDepth);
    // maybe storing these values in some lookup table is faster than recalculating every time?
    float distance = 0.1236 * tan(rawDisparity / 2842.5 + 1.1863);
    //float distance = 1.0 / (rawDisparity * -0.0030711016 + 3.3309495161);
    float visible = 1.0;
    if (distance > maxDistance) {
        distance = maxDistance;
        visible = 0.0;
    } if (uintDepth >= UINT_MAX_KINECT_DEPTH) {
        distance = maxDistance;
        visible = 0.0;
    }
    int index = coord.y * resolutionX + coord.x;
    const vec3 position = depthToWorld(coord, distance);
    //points[base].position = position;
    points[index] = vec4(position, visible);
    //points[index] = vec4(float(coord.x), float(coord.y), 0.0, 1.0);
}
