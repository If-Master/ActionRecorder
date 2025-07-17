package me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin;

import me.kanuunankuulaspluginsMobs.actionrecorder.client.ActionrecorderClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Shadow
    protected TextFieldWidget chatField;

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        // Check if Enter key was pressed (keyCode 257 is Enter)
        if (keyCode == 257) {
            String message = chatField.getText();
            if (message != null && !message.trim().isEmpty()) {
                // Capture the chat message
                ActionrecorderClient client = ActionrecorderClient.getInstance();
                if (client != null) {
                    client.onChatMessageSent(message);
                }
            }
        }
    }
}