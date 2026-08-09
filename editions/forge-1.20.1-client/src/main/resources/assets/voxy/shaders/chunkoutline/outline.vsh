#version 460

layout(binding = 0, std140) uniform SceneUniform {
    mat4 MVP;
    ivec4 cameraBlockPos;
    vec4 cameraRemainder;
};

layout(binding = 1, std430) restrict readonly buffer SectionPosBuffer {
    ivec2[] sectionPos;
};

ivec3 unpackPos(ivec2 pos) {
    return ivec3(pos.y >> 10, (pos.x << 12) >> 12, ((pos.y << 22) | int(uint(pos.x) >> 10)) >> 10);
}

#ifdef TAA
vec2 getTAA();
#endif

void main() {
    uint id = (gl_InstanceID << 5) + gl_BaseInstance + (gl_VertexID >> 3);
    ivec3 origin = unpackPos(sectionPos[id]) * 16 - cameraBlockPos.xyz;
    ivec3 corner = ivec3(gl_VertexID & 1, (gl_VertexID >> 2) & 1, (gl_VertexID >> 1) & 1) * 16;

    gl_Position = MVP * vec4(vec3(corner + origin), 1.0);
    gl_Position.z += 0.00005;

#ifdef TAA
    gl_Position.xy += getTAA() * gl_Position.w;
#endif
}
