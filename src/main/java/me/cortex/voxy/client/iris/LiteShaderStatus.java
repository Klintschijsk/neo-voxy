package me.cortex.voxy.client.iris;

/**
 * Cold-path status written while Iris builds a shader pipeline and read by the config tooltip.
 * It is deliberately only one volatile snapshot: no polling, tick hook, or render-loop work.
 */
public final class LiteShaderStatus {
    public enum Code {
        DISABLED,
        REQUESTED,
        ACTIVE,
        NO_VOXY_PATCH,
        MISSING_PROGRAMS,
        MISSING_CONTRACT,
        CONTRACT_MISMATCH,
        API_MISMATCH,
        TRANSITION_REQUIRED,
        ERROR
    }

    public record Snapshot(Code code, String packName, String testedVersions, String detail) {
        private static String clean(String value) {
            return value == null ? "" : value;
        }

        public Snapshot {
            packName = clean(packName);
            testedVersions = clean(testedVersions);
            detail = clean(detail);
        }
    }

    private static volatile Snapshot current = new Snapshot(Code.DISABLED, "", "", "");

    private LiteShaderStatus() {
    }

    public static Snapshot get() {
        return current;
    }

    public static void set(Code code) {
        current = new Snapshot(code, "", "", "");
    }

    public static void set(Code code, String detail) {
        current = new Snapshot(code, "", "", detail);
    }

    public static void active(String packName, String testedVersions, String transition) {
        current = new Snapshot(Code.ACTIVE, packName, testedVersions, transition);
    }
}
