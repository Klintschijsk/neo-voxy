package me.cortex.voxy.commonImpl;

import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.config.Serialization;
import net.minecraftforge.fml.loading.FMLLoader;

public class VoxyCommon {
    public static final String MOD_VERSION;
    public static final boolean IS_DEDICATED_SERVER;
    public static final boolean IS_IN_MINECRAFT;

    static {
        var mod = FMLLoader.getLoadingModList().getModFileById("voxy");
        IS_IN_MINECRAFT = mod != null;
        MOD_VERSION = mod == null ? "0.3.3-1.20.1-alpha.1" : mod.versionString();
        IS_DEDICATED_SERVER = !FMLLoader.getDist().isClient();

        if (IS_IN_MINECRAFT) {
            Serialization.init();
        } else {
            Logger.error("Running Voxy outside Minecraft");
        }
    }

    public static boolean isVerificationFlagOn(String name) {
        return isVerificationFlagOn(name, false);
    }

    public static boolean isVerificationFlagOn(String name, boolean defaultOn) {
        return System.getProperty("voxy." + name, defaultOn ? "true" : "false").equals("true");
    }

    public interface IInstanceFactory { VoxyInstance create(); }

    private static VoxyInstance instance;
    private static IInstanceFactory factory;

    public static void setInstanceFactory(IInstanceFactory value) {
        if (factory != null) {
            throw new IllegalStateException("Cannot set instance factory more than once");
        }
        factory = value;
    }

    public static VoxyInstance getInstance() {
        return instance;
    }

    public static void shutdownInstance() {
        if (instance != null) {
            var old = instance;
            instance = null;
            old.shutdown();
        }
    }

    public static void createInstance() {
        if (factory == null) {
            return;
        }
        if (instance != null) {
            throw new IllegalStateException("Cannot create multiple instances");
        }
        instance = factory.create();
    }

    public static boolean isAvailable() {
        return factory != null;
    }

    public static final boolean IS_MINE_IN_ABYSS = false;
}
