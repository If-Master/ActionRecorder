package me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {

    @Invoker("handleInputEvents")
    void invokeHandleInputEvents();


    @Invoker("doItemUse")
    void invokeDoItemUse();
}
