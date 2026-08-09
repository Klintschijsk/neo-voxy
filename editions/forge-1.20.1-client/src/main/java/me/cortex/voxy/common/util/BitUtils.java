package me.cortex.voxy.common.util;

/** Java 17 equivalents of the later JDK bit compress/expand helpers. */
public final class BitUtils {
    private BitUtils() {}

    public static int expand(int value, int mask) {
        int result = 0;
        for (int sourceBit = 1; mask != 0; sourceBit <<= 1) {
            int targetBit = mask & -mask;
            if ((value & sourceBit) != 0) result |= targetBit;
            mask ^= targetBit;
        }
        return result;
    }

    public static int compress(int value, int mask) {
        int result = 0;
        for (int targetBit = 1; mask != 0; targetBit <<= 1) {
            int sourceBit = mask & -mask;
            if ((value & sourceBit) != 0) result |= targetBit;
            mask ^= sourceBit;
        }
        return result;
    }

    public static long expand(long value, long mask) {
        long result = 0;
        for (long sourceBit = 1; mask != 0; sourceBit <<= 1) {
            long targetBit = mask & -mask;
            if ((value & sourceBit) != 0) result |= targetBit;
            mask ^= targetBit;
        }
        return result;
    }
}
