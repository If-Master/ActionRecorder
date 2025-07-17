package me.kanuunankuulaspluginsMobs.actionrecorder.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ActionrecorderClient implements ClientModInitializer {

    private static ActionrecorderClient instance;
    public static Actionrecorder actionRecorder;

    private static KeyBinding recordingKeyBinding;
    private static KeyBinding playbackKeyBinding;
    private static KeyBinding toggleKeyBinding;
    private static KeyBinding loopKeyBinding;

    @Override
    public void onInitializeClient() {
        // Set the singleton instance
        instance = this;

        // Initialize the action recorder
        actionRecorder = new Actionrecorder();

        // Register key bindings
        recordingKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.actionrecorder.record",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "category.actionrecorder"
        ));

        playbackKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.actionrecorder.playback",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.actionrecorder"
        ));

        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.actionrecorder.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                "category.actionrecorder"
        ));

        loopKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.actionrecorder.loop",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                "category.actionrecorder"
        ));

        // Register chat message events
        registerChatEvents();

        // Register tick event
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (actionRecorder != null) {
                actionRecorder.tick();

                // Handle key presses
                while (recordingKeyBinding.wasPressed()) {
                    actionRecorder.toggleRecording();
                }

                while (playbackKeyBinding.wasPressed()) {
                    actionRecorder.togglePlayback();
                }

                while (toggleKeyBinding.wasPressed()) {
                    actionRecorder.toggleEnabled();
                }

                while (loopKeyBinding.wasPressed()) {
                    actionRecorder.toggleLoopPlayback();
                }
            }
        });
    }

    private void registerChatEvents() {
        // Capture outgoing chat messages (when player sends a message)
        ClientSendMessageEvents.ALLOW_CHAT.register((message) -> {
            if (actionRecorder != null) {
                actionRecorder.onChatMessageSent(message);
            }
            return true; // Allow the message to be sent
        });

        // Capture outgoing commands (when player kwsends a command)
        ClientSendMessageEvents.ALLOW_COMMAND.register((command) -> {
            if (actionRecorder != null) {
                actionRecorder.onCommandSent(command);
            }
            return true; // Allow the command to be sent
        });
    }

    // Add this method to ActionrecorderClient:
    public void onCommandSent(String command) {
        if (actionRecorder != null) {
            actionRecorder.onCommandSent(command);
        }
    }
    // Add the getInstance method
    public static ActionrecorderClient getInstance() {
        return instance;
    }

    public void onChatMessageSent(String chatText) {
        if (actionRecorder != null) {
            actionRecorder.onChatMessageSent(chatText);
        }
    }

    // Helper methods for manual chat handling if needed
    public void sendChatMessage(String message) {
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            net.minecraft.client.MinecraftClient.getInstance().player.networkHandler.sendChatMessage(message);
        }
    }

    public void sendCommand(String command) {
        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
            net.minecraft.client.MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);
        }
    }
}