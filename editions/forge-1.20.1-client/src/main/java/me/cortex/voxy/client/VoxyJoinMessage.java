package me.cortex.voxy.client;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "voxy", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VoxyJoinMessage {
    private static int pending = -1;

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        pending = VoxyConfig.CONFIG.showJoinMessage ? 20 : -1;
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        pending = -1;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pending < 0 || pending-- > 0) return;
        var player = Minecraft.getInstance().player;
        if (player != null && VoxyConfig.CONFIG.showJoinMessage) {
            player.displayClientMessage(Component.literal("[Neo Voxy] " + version()).withStyle(ChatFormatting.AQUA), false);
            player.displayClientMessage(Component.translatable("voxy.join.disableHint").withStyle(ChatFormatting.DARK_GRAY), false);
        }
    }

    private static String version() {
        return ModList.get().getModContainerById("voxy")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse(VoxyCommon.MOD_VERSION);
    }
}
