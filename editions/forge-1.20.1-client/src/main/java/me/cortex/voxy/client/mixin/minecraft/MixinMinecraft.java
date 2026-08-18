package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.ClientSessionEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "disconnect", at = @At("TAIL"), require = 0)
    private void voxy$injectDisconnect(CallbackInfo ci) {
        voxy$closeSession();
    }

    @Inject(method = "clearLevel", at = @At("TAIL"), require = 0)
    private void voxy$injectClearLevel(CallbackInfo ci) {
        voxy$closeSession();
    }

    private static void voxy$closeSession() {
        if (ClientSessionEvents.inSession) {
            ClientSessionEvents.sessionEnd();
        }
    }
}
