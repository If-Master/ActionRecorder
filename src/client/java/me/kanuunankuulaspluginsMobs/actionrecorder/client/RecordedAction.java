package me.kanuunankuulaspluginsMobs.actionrecorder.client;

import net.minecraft.client.option.KeyBinding;

public class RecordedAction {
    public final KeyBinding keyBinding;
    public final boolean pressed;
    public final long timestamp;
    public final long holdDuration;

    public final ActionType actionType;
    public final String chatMessage;
    public final String command;
    public final int mouseX, mouseY;
    public final int mouseButton;

    public enum ActionType {
        KEY_PRESS,
        CHAT_MESSAGE,
        COMMAND,
        MOUSE_CLICK
    }

    public RecordedAction(KeyBinding keyBinding, boolean pressed, long timestamp, long holdDuration) {
        this.keyBinding = keyBinding;
        this.pressed = pressed;
        this.timestamp = timestamp;
        this.holdDuration = holdDuration;
        this.actionType = ActionType.KEY_PRESS;
        this.chatMessage = null;
        this.command = null;
        this.mouseX = this.mouseY = 0;
        this.mouseButton = -1;
    }

    public RecordedAction(String chatMessage, long timestamp) {
        this.actionType = ActionType.CHAT_MESSAGE;
        this.chatMessage = chatMessage;
        this.command = null;
        this.timestamp = timestamp;
        this.keyBinding = null;
        this.pressed = false;
        this.holdDuration = 0;
        this.mouseX = 0;
        this.mouseY = 0;
        this.mouseButton = 0;
    }

    public RecordedAction(String command, long timestamp, boolean isCommand) {
        this.actionType = ActionType.COMMAND;
        this.command = command;
        this.chatMessage = null;
        this.timestamp = timestamp;
        this.keyBinding = null;
        this.pressed = false;
        this.holdDuration = 0;
        this.mouseX = 0;
        this.mouseY = 0;
        this.mouseButton = 0;
    }

    public RecordedAction(int mouseX, int mouseY, int mouseButton, boolean pressed, long timestamp) {
        this.keyBinding = null;
        this.pressed = pressed;
        this.timestamp = timestamp;
        this.holdDuration = 0;
        this.actionType = ActionType.MOUSE_CLICK;
        this.chatMessage = null;
        this.command = null;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseButton = mouseButton;
    }

    public RecordedAction(KeyBinding keyBinding, boolean pressed, long timestamp) {
        this(keyBinding, pressed, timestamp, 0);
    }

    @Override
    public String toString() {
        switch (actionType) {
            case CHAT_MESSAGE:
                return "ChatAction{message='" + chatMessage + "', timestamp=" + timestamp + "}";
            case COMMAND:
                return "CommandAction{command='" + command + "', timestamp=" + timestamp + "}";
            case MOUSE_CLICK:
                return "MouseAction{x=" + mouseX + ", y=" + mouseY + ", button=" + mouseButton +
                        ", pressed=" + pressed + ", timestamp=" + timestamp + "}";
            default:
                return "KeyAction{key=" + (keyBinding != null ? keyBinding.getBoundKeyLocalizedText().getString() : "null") +
                        ", pressed=" + pressed + ", timestamp=" + timestamp + ", holdDuration=" + holdDuration + "}";
        }
    }
}