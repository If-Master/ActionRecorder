package me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {

    @Invoker("setText")
    void invokeSetText(String text);
}