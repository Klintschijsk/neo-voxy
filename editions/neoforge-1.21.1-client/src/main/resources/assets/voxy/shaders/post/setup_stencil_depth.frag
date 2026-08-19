#version 450 core

layout(binding = 0) uniform sampler2D depthTex;
layout(location = 1) uniform vec2 scaleFactor;
layout(location = 2) uniform mat4 inverseVanillaMvp;
layout(location = 6) uniform float lodBoundaryFadeStart;
layout(location = 7) uniform float lodBoundaryFadeEnd;
layout(location = 8) uniform ivec3 cameraBlockOrigin;
layout(location = 9) uniform vec3 cameraFraction;
layout(location = 10) uniform int boundaryGuardPass;
layout(location = 11) uniform mat4 voxyMvp;

#import <voxy:util/depthutils.glsl>

in vec2 UV;

float stableWorldDither(vec3 cameraRelativePosition) {
    ivec3 cell = cameraBlockOrigin * 8
            + ivec3(floor((cameraRelativePosition + cameraFraction) * 8.0));
    uint hash = uint(cell.x) * 0x8da6b343u;
    hash ^= uint(cell.y) * 0xd8163841u;
    hash ^= uint(cell.z) * 0xcb1ab31fu;
    hash ^= hash >> 16u;
    hash *= 0x7feb352du;
    hash ^= hash >> 15u;
    return (float(hash & 1023u) + 0.5) * (1.0 / 1024.0);
}

void main() {
    float vanillaDepth = texture(depthTex, UV * scaleFactor).r;
    if (vanillaDepth == FAR) {
        discard;
    }

    vec3 cameraRelativePosition = vec3(0.0);
    float horizontalDistance = 0.0;
    float lodCoverage = 0.0;
    float ditherValue = 1.0;
    if (lodBoundaryFadeEnd > lodBoundaryFadeStart) {
        vec4 cameraRelative = inverseVanillaMvp
                * vec4(SCREEN2NDC(vec3(UV, vanillaDepth)), 1.0);
        cameraRelative.xyz /= cameraRelative.w;
        cameraRelativePosition = cameraRelative.xyz;

        horizontalDistance = length(cameraRelativePosition.xyz);
        if (horizontalDistance > lodBoundaryFadeStart) {
            lodCoverage = smoothstep(
                    lodBoundaryFadeStart, lodBoundaryFadeEnd, horizontalDistance);
            lodCoverage *= lodCoverage;
            ditherValue = stableWorldDither(cameraRelativePosition);
        }
    }

    if (boundaryGuardPass != 0) {
        // Only the dither-selected part of the transition band receives a
        // conservative copy of the vanilla surface depth. This lets nearby LOD
        // replace it, while rejecting caves or missing LOD far behind it.
        if (horizontalDistance <= lodBoundaryFadeStart
                || horizontalDistance >= lodBoundaryFadeEnd
                || ditherValue >= lodCoverage) {
            discard;
        }

        float rayLength = max(length(cameraRelativePosition), 1.0);
        vec3 guardedPosition = cameraRelativePosition
                * (1.0 + min(2.0 / rayLength, 0.125));
        vec4 guardedClip = voxyMvp * vec4(guardedPosition, 1.0);
        float guardedDepth = NDC2SCREEN_DEPTH(guardedClip.z / guardedClip.w);
        gl_FragDepth = gl_DepthRange.diff * guardedDepth + gl_DepthRange.near;
        return;
    }

    if (lodBoundaryFadeEnd > lodBoundaryFadeStart) {
        if (horizontalDistance >= lodBoundaryFadeEnd
                || (horizontalDistance > lodBoundaryFadeStart
                && ditherValue < lodCoverage)) {
            // Leave stencil/depth cleared: LOD owns this pixel. A second guarded
            // pass restores only the transition pixels' conservative depth.
            discard;
        }
    }

    // Drawing replaces the cleared stencil with zero, keeping vanilla terrain.
    gl_FragDepth = NEAR;
}
