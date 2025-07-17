package me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {

    // Try these method names in order - one of them should work for 1.21.6

    // Most likely candidates for 1.21.6:
    @Invoker("handleInputEvents")
    void invokeHandleInputEvents();

    // Alternative method names to try:
    // @Invoker("doAttack")
    // void invokeDoAttack();

    // @Invoker("attack")
    // void invokeAttack();

    // @Invoker("continueAttack")
    // void invokeContinueAttack();

    @Invoker("doItemUse")
    void invokeDoItemUse();
}