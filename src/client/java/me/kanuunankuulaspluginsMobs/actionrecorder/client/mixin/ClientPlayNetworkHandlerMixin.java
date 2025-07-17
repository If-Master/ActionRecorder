package me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin;

import me.kanuunankuulaspluginsMobs.actionrecorder.client.ActionrecorderClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"))
    private void onSendChatMessage(String message, CallbackInfo ci) {
        // Capture the chat message when it's sent via network
        ActionrecorderClient client = ActionrecorderClient.getInstance();
        if (client != null) {
            client.onChatMessageSent(message);
        }
    }
}