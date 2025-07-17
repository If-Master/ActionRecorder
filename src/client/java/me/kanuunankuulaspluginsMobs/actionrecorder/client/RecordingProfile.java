package me.kanuunankuulaspluginsMobs.actionrecorder.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RecordingProfile {
    private String name;
    private List<SerializableAction> actions;
    private long createdTime;
    private String description;

    public RecordingProfile() {
        this.actions = new ArrayList<>();
        this.createdTime = System.currentTimeMillis();
        this.description = "";
    }

    public RecordingProfile(String name, List<RecordedAction> actions) {
        this.name = name;
        this.actions = convertToSerializable(actions);
        this.createdTime = System.currentTimeMillis();
        this.description = "";
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public long getCreatedTime() { return createdTime; }
    public int getActionCount() { return actions.size(); }

    private List<SerializableAction> convertToSerializable(List<RecordedAction> actions) {
        List<SerializableAction> serializable = new ArrayList<>();
        for (RecordedAction action : actions) {
            serializable.add(new SerializableAction(action));
        }
        return serializable;
    }

    public List<RecordedAction> getActions() {
        List<RecordedAction> recordedActions = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();

        for (SerializableAction action : actions) {
            RecordedAction recordedAction = action.toRecordedAction(client);
            if (recordedAction != null) {
                recordedActions.add(recordedAction);
            }
        }
        return recordedActions;
    }

    private static class SerializableAction {
        String keyBindingName;
        boolean pressed;
        long timestamp;
        long holdDuration;
        RecordedAction.ActionType actionType;
        String chatMessage;
        int mouseX, mouseY;
        int mouseButton;

        public SerializableAction(RecordedAction action) {
            this.keyBindingName = action.keyBinding != null ?
                    getKeyBindingName(action.keyBinding) : null;
            this.pressed = action.pressed;
            this.timestamp = action.timestamp;
            this.holdDuration = action.holdDuration;
            this.actionType = action.actionType;
            this.chatMessage = action.chatMessage;
            this.mouseX = action.mouseX;
            this.mouseY = action.mouseY;
            this.mouseButton = action.mouseButton;
        }

        private static String getKeyBindingName(KeyBinding keyBinding) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (keyBinding == client.options.forwardKey) return "forwardKey";
            if (keyBinding == client.options.backKey) return "backKey";
            if (keyBinding == client.options.leftKey) return "leftKey";
            if (keyBinding == client.options.rightKey) return "rightKey";
            if (keyBinding == client.options.jumpKey) return "jumpKey";
            if (keyBinding == client.options.sneakKey) return "sneakKey";
            if (keyBinding == client.options.sprintKey) return "sprintKey";
            if (keyBinding == client.options.attackKey) return "attackKey";
            if (keyBinding == client.options.useKey) return "useKey";
            if (keyBinding == client.options.chatKey) return "chatKey";
            return keyBinding.getTranslationKey();
        }

        public RecordedAction toRecordedAction(MinecraftClient client) {
            switch (actionType) {
                case CHAT_MESSAGE:
                    return new RecordedAction(chatMessage, timestamp);
                case MOUSE_CLICK:
                    return new RecordedAction(mouseX, mouseY, mouseButton, pressed, timestamp);
                case KEY_PRESS:
                default:
                    KeyBinding keyBinding = getKeyBindingFromName(keyBindingName, client);
                    if (keyBinding != null) {
                        return new RecordedAction(keyBinding, pressed, timestamp, holdDuration);
                    }
                    return null;
            }
        }

        private KeyBinding getKeyBindingFromName(String name, MinecraftClient client) {
            if (name == null) return null;

            switch (name) {
                case "forwardKey": return client.options.forwardKey;
                case "backKey": return client.options.backKey;
                case "leftKey": return client.options.leftKey;
                case "rightKey": return client.options.rightKey;
                case "jumpKey": return client.options.jumpKey;
                case "sneakKey": return client.options.sneakKey;
                case "sprintKey": return client.options.sprintKey;
                case "attackKey": return client.options.attackKey;
                case "useKey": return client.options.useKey;
                case "chatKey": return client.options.chatKey;
                default:
                    for (KeyBinding kb : client.options.allKeys) {
                        if (kb.getTranslationKey().equals(name)) {
                            return kb;
                        }
                    }
                    return null;
            }
        }
    }

    public static Path getProfilesDirectory() {
        Path configDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("config");
        return configDir.resolve("actionrecorder").resolve("profiles");
    }

    public static List<RecordingProfile> loadAllProfiles() {
        List<RecordingProfile> profiles = new ArrayList<>();
        Path profilesDir = getProfilesDirectory();

        if (!Files.exists(profilesDir)) {
            try {
                Files.createDirectories(profilesDir);
            } catch (IOException e) {
                e.printStackTrace();
                return profiles;
            }
        }

        try {
            Files.list(profilesDir)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            RecordingProfile profile = loadProfile(path);
                            if (profile != null) {
                                profiles.add(profile);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return profiles;
    }

    public static RecordingProfile loadProfile(Path path) {
        try {
            String json = Files.readString(path);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.fromJson(json, RecordingProfile.class);
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean saveProfile() {
        Path profilesDir = getProfilesDirectory();

        try {
            Files.createDirectories(profilesDir);
            Path profilePath = profilesDir.resolve(name + ".json");

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String json = gson.toJson(this);

            Files.writeString(profilePath, json);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteProfile() {
        Path profilesDir = getProfilesDirectory();
        Path profilePath = profilesDir.resolve(name + ".json");

        try {
            return Files.deleteIfExists(profilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}