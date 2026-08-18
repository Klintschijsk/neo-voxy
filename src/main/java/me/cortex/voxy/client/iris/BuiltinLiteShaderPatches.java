package me.cortex.voxy.client.iris;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class BuiltinLiteShaderPatches {
    record PatchPair(String opaque, String translucent, String packName, String testedVersions,
                     String transition) {
    }

    private static final String EMIT_SIGNATURE =
            "void voxy_emitFragment(VoxyFragmentParameters parameters)";
    private static final String ECLIPSE_OPAQUE = readResource(
            "/assets/voxy/shaders/iris/lite/eclipse_482/opaque_emit.glsl");
    private static final String ECLIPSE_TRANSLUCENT = readResource(
            "/assets/voxy/shaders/iris/lite/eclipse_482/translucent_emit.glsl");

    private BuiltinLiteShaderPatches() {
    }

    static PatchPair tryCreate(String opaque, String translucent) {
        if (opaque == null || translucent == null) {
            return null;
        }

        if (!isEclipse482(opaque, translucent)) {
            return null;
        }

        String translucentReplacement = eclipseTranslucentReplacement(translucent);
        if (translucentReplacement == null) {
            return null;
        }

        String liteOpaque = replaceEmitFunction(opaque, ECLIPSE_OPAQUE);
        String liteTranslucent = replaceEmitFunction(translucent, translucentReplacement);
        if (liteOpaque == null || liteTranslucent == null) {
            throw new IllegalStateException("Eclipse 482 Voxy program layout did not match the built-in Lite patch");
        }

        return new PatchPair(liteOpaque, liteTranslucent,
                "Eclipse Shader", "482 (built-in)", "lod-boundary-fade");
    }

    private static boolean isEclipse482(String opaque, String translucent) {
        // Iris strips version macros here, so match Eclipse 482 by a narrow source fingerprint.
        return opaque.contains("vec2 encodeNormal(vec3 n)")
                && opaque.contains("float encodeVec2(vec2 a)")
                && opaque.contains("SSSAMOUNT")
                && translucent.contains("float GGX(")
                && translucent.contains("vec3 rayTrace(")
                && translucent.contains("vec3 DH_toClipSpace3(")
                && translucent.contains("const float biasAmount = 0.00015;")
                && translucent.contains("float invLdFast(float linearDepth)")
                && translucent.contains("float DH_ld(float dist)")
                && opaque.indexOf(EMIT_SIGNATURE) == opaque.lastIndexOf(EMIT_SIGNATURE)
                && translucent.indexOf(EMIT_SIGNATURE) == translucent.lastIndexOf(EMIT_SIGNATURE);
    }

    private static String eclipseTranslucentReplacement(String source) {
        String dimensionLight;
        String skyReflection;
        if (source.contains("lightSourceColorSSBO/2400.0")) {
            dimensionLight = """
                    float lightSign = float(sunElevation > 1e-5) * 2.0 - 1.0;
                    vec3 worldLight = lightSign * normalize(mat3(vxModelViewInv) * sunPosition);
                    float nDotL = clamp(dot(normal, worldLight), 0.0, 1.0);
                    nDotL = clamp((-15.0 + nDotL * 255.0) / 240.0, 0.0, 1.0);
                    directLight = (lightSourceColorSSBO / 2400.0) * nDotL;

                    vec3 indirectNormal = normal / dot(abs(normal), vec3(1.0));
                    float skyDirection = clamp(indirectNormal.y * 0.7 + 0.3, 0.0, 1.0);
                    ambientLight = (averageSkyCol_CloudsSSBO / 900.0)
                            * mix(0.08, 1.0, skyDirection)
                            * mix(0.12, 1.0, lightmap.y);
                    """;
            skyReflection = "vec3 skyReflection = averageSkyCol_CloudsSSBO / 1200.0;";
        } else if (source.contains("vec3 lightPos = LightSourcePosition(")) {
            dimensionLight = "ambientLight = vec3(0.030, 0.035, 0.100) * mix(0.35, 1.0, lightmap.y);";
            skyReflection = "vec3 skyReflection = vec3(0.030, 0.035, 0.100);";
        } else if (source.contains("volumetricsFromTex(normal, colortex4")) {
            dimensionLight = "ambientLight = vec3(0.028, 0.022, 0.020) * mix(0.45, 1.0, lightmap.y);";
            skyReflection = "vec3 skyReflection = vec3(0.035, 0.025, 0.020);";
        } else {
            return null;
        }
        return ECLIPSE_TRANSLUCENT
                .replace("/* VOXY_LITE_DIMENSION_LIGHT */", dimensionLight.strip())
                .replace("/* VOXY_LITE_SKY_REFLECTION */", skyReflection);
    }

    static String replaceEmitFunction(String source, String replacement) {
        int functionStart = source.indexOf(EMIT_SIGNATURE);
        if (functionStart < 0 || functionStart != source.lastIndexOf(EMIT_SIGNATURE)) {
            return null;
        }
        int braceStart = source.indexOf('{', functionStart + EMIT_SIGNATURE.length());
        if (braceStart < 0) {
            return null;
        }

        int depth = 0;
        boolean lineComment = false;
        boolean blockComment = false;
        for (int i = braceStart; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';
            if (lineComment) {
                if (current == '\n') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (current == '/' && next == '/') {
                lineComment = true;
                i++;
                continue;
            }
            if (current == '/' && next == '*') {
                blockComment = true;
                i++;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return source.substring(0, functionStart)
                        + replacement.strip()
                        + source.substring(i + 1);
            }
        }
        return null;
    }

    private static String readResource(String path) {
        try (var input = BuiltinLiteShaderPatches.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing built-in Lite shader resource " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read built-in Lite shader resource " + path, e);
        }
    }
}
