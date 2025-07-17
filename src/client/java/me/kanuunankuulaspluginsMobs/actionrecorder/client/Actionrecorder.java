package me.kanuunankuulaspluginsMobs.actionrecorder.client;

import me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin.MouseAccessor;
import me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin.MinecraftClientMixin;
import me.kanuunankuulaspluginsMobs.actionrecorder.client.mixin.ChatScreenAccessor;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import java.util.*;
import java.util.concurrent.*;

public class Actionrecorder {
    private boolean recordCommands = true;

    private final MinecraftClient client = MinecraftClient.getInstance();
    private final List<RecordedAction> recordedActions = new ArrayList<>();
    private final Queue<RecordedAction> playbackQueue = new ConcurrentLinkedQueue<>();

    private boolean isRecording = false;
    private volatile boolean isPlayingBack = false;
    private boolean isEnabled = true;
    private boolean loopPlayback = true;

    private long recordingStartTime = 0;
    private volatile long playbackStartTime = 0;
    private volatile int playbackIndex = 0;

    private final Map<KeyBinding, Boolean> previousKeyStates = new HashMap<>();
    private final Map<KeyBinding, Long> keyPressStartTimes = new HashMap<>();

    private ExecutorService playbackExecutor = null;
    private Future<?> playbackTask = null;

    private boolean recordMouseClicks = true;
    private boolean recordChatMessages = true;
    private List<RecordingProfile> savedProfiles = new ArrayList<>();
    private RecordingProfile currentProfile = null;

    private final Map<String, Boolean> previousMouseStates = new HashMap<>();

    private boolean waitingForChatMessage = false;
    private long chatKeyPressTime = 0;
    private String pendingChatMessage = null;

    private boolean inChatScreen = false;

    public void setRecordMouseClicks(boolean record) {
        this.recordMouseClicks = record;
    }

    public void setRecordChatMessages(boolean record) {
        this.recordChatMessages = record;
    }
    public void setRecordCommands(boolean record) {
        this.recordCommands = record;
    }
    public boolean isRecordCommands() {
        return recordCommands;
    }

    public boolean isRecordMouseClicks() {
        return recordMouseClicks;
    }

    public boolean isRecordChatMessages() {
        return recordChatMessages;
    }

    public void toggleRecording() {
        if (!isEnabled) return;

        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    public void togglePlayback() {
        if (!isEnabled) return;

        if (isPlayingBack) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    public void toggleEnabled() {
        isEnabled = !isEnabled;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Action Recorder: " + (isEnabled ? "Enabled" : "Disabled")), true);
        }

        if (!isEnabled) {
            stopRecording();
            stopPlayback();
        }
    }

    public void toggleLoopPlayback() {
        loopPlayback = !loopPlayback;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("Loop Playback: " + (loopPlayback ? "Enabled" : "Disabled")), true);
        }
    }

    private void startRecording() {
        if (isPlayingBack) {
            stopPlayback();
        }

        isRecording = true;
        recordingStartTime = System.currentTimeMillis();
        recordedActions.clear();
        keyPressStartTimes.clear();
        waitingForChatMessage = false;
        pendingChatMessage = null;
        initializeKeyStates();

        if (client.player != null) {
            client.player.sendMessage(Text.literal("Recording started"), true);
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        isRecording = false;
        waitingForChatMessage = false;
        pendingChatMessage = null;

        long currentTime = System.currentTimeMillis();
        long timeSinceStart = currentTime - recordingStartTime;

        for (Map.Entry<KeyBinding, Long> entry : keyPressStartTimes.entrySet()) {
            KeyBinding keyBinding = entry.getKey();
            long pressStartTime = entry.getValue();
            long holdDuration = timeSinceStart - pressStartTime;

            recordedActions.add(new RecordedAction(keyBinding, false, timeSinceStart, holdDuration));
        }

        keyPressStartTimes.clear();

        if (client.player != null) {
            client.player.sendMessage(Text.literal("Recording stopped. " + recordedActions.size() + " actions recorded"), true);
        }
    }

    private void startPlayback() {
        if (recordedActions.isEmpty()) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("No actions recorded!"), true);
            }
            return;
        }

        if (isRecording) {
            stopRecording();
        }

        stopPlayback();

        isPlayingBack = true;
        playbackStartTime = System.currentTimeMillis();
        playbackIndex = 0;

        playbackExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ActionRecorder-Playback");
            t.setDaemon(true);
            return t;
        });

        playbackTask = playbackExecutor.submit(this::playbackLoop);

        if (client.player != null) {
            client.player.sendMessage(Text.literal("Playback started" + (loopPlayback ? " (looping)" : "")), true);
        }
    }

    private void stopPlayback() {
        isPlayingBack = false;

        if (playbackTask != null && !playbackTask.isDone()) {
            playbackTask.cancel(true);
        }

        if (playbackExecutor != null && !playbackExecutor.isShutdown()) {
            playbackExecutor.shutdown();
            try {
                if (!playbackExecutor.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    playbackExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                playbackExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        MinecraftClient.getInstance().execute(() -> {
            for (RecordedAction action : recordedActions) {
                if (action.keyBinding != null) {
                    KeyBinding.setKeyPressed(action.keyBinding.getDefaultKey(), false);
                }
            }
        });

        if (client.player != null) {
            client.player.sendMessage(Text.literal("Playback stopped"), true);
        }
    }

    private void playbackLoop() {
        try {
            while (isPlayingBack && !Thread.currentThread().isInterrupted()) {
                if (playbackIndex >= recordedActions.size()) {
                    if (loopPlayback) {
                        playbackIndex = 0;
                        playbackStartTime = System.currentTimeMillis();
                        MinecraftClient.getInstance().execute(() -> {
                            if (client.player != null) {
                                client.player.sendMessage(Text.literal("Looping playback..."), true);
                            }
                        });
                        continue;
                    } else {
                        break;
                    }
                }

                long currentTime = System.currentTimeMillis();
                long timeSinceStart = currentTime - playbackStartTime;

                while (playbackIndex < recordedActions.size() && isPlayingBack) {
                    RecordedAction action = recordedActions.get(playbackIndex);

                    if (action.timestamp <= timeSinceStart) {
                        MinecraftClient.getInstance().execute(() -> executeAction(action));
                        playbackIndex++;
                    } else {
                        break;
                    }
                }

                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (isPlayingBack) {
                MinecraftClient.getInstance().execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("Playback completed"), true);
                    }
                });
                isPlayingBack = false;
            }
        }
    }

    public void tick() {
        inChatScreen = client.currentScreen instanceof ChatScreen;

        if (isRecording) {
            recordActions();
        }
    }

    private void recordActions() {
        if (!isRecording) return;

        long currentTime = System.currentTimeMillis();
        long timeSinceStart = currentTime - recordingStartTime;

        if (recordMouseClicks) {
            recordMouseActions(timeSinceStart);
        }

        if (inChatScreen) {
            return;
        }

        KeyBinding[] keyBindings = getKeyBindings();

        for (KeyBinding keyBinding : keyBindings) {
            boolean currentState = isKeyCurrentlyPressed(keyBinding);
            Boolean previousState = previousKeyStates.get(keyBinding);

            if (previousState == null || previousState != currentState) {
                if (currentState) {
                    keyPressStartTimes.put(keyBinding, timeSinceStart);
                    recordedActions.add(new RecordedAction(keyBinding, true, timeSinceStart, 0));
                } else {
                    Long pressStartTime = keyPressStartTimes.remove(keyBinding);
                    long holdDuration = (pressStartTime != null) ? timeSinceStart - pressStartTime : 0;
                    recordedActions.add(new RecordedAction(keyBinding, false, timeSinceStart, holdDuration));
                }
                previousKeyStates.put(keyBinding, currentState);
            }
        }
    }

    private boolean isKeyCurrentlyPressed(KeyBinding keyBinding) {
        if (client.getWindow() == null) return false;

        InputUtil.Key key = keyBinding.getDefaultKey();
        if (key == null) return false;

        int keyCode = key.getCode();
        if (keyCode == InputUtil.UNKNOWN_KEY.getCode()) return false;

        try {
            if (key.getCategory() == InputUtil.Type.MOUSE) {
                if (keyCode >= 0 && keyCode <= 8) {
                    return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
                }
                return false;
            }

            if (key.getCategory() == InputUtil.Type.KEYSYM) {
                if (keyCode >= 0 && keyCode <= 511) {
                    return GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }

    private void recordMouseActions(long timeSinceStart) {
        if (client.getWindow() == null) return;

        for (int button = 0; button < 3; button++) {
            try {
                boolean currentState = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), button) == GLFW.GLFW_PRESS;
                String buttonKey = "mouse_" + button;
                Boolean previousState = previousMouseStates.get(buttonKey);

                if (previousState == null || previousState != currentState) {
                    recordedActions.add(new RecordedAction(
                            0, 0, button, currentState, timeSinceStart
                    ));
                    previousMouseStates.put(buttonKey, currentState);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private void executeAction(RecordedAction action) {
        try {
            switch (action.actionType) {
                case KEY_PRESS:
                    if (action.keyBinding != null) {
                        KeyBinding.setKeyPressed(action.keyBinding.getDefaultKey(), action.pressed);
                        if (action.pressed) {
                            KeyBinding.onKeyPressed(action.keyBinding.getDefaultKey());
                        }
                    }
                    break;

                case MOUSE_CLICK:
                    executeMouseAction(action);
                    break;

                case CHAT_MESSAGE:
                    if (action.chatMessage != null && !action.chatMessage.trim().isEmpty()) {
                        executeChatMessage(action.chatMessage);
                    }
                    break;

                case COMMAND:
                    if (action.command != null && !action.command.trim().isEmpty()) {
                        executeCommand(action.command);
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error executing action: " + action.actionType + " - " + e.getMessage());
        }
    }

    public void onChatScreen(String message) {
        if (isRecording && recordChatMessages) {
            long currentTime = System.currentTimeMillis();
            long timeSinceStart = currentTime - recordingStartTime;

            RecordedAction chatAction = new RecordedAction(message, timeSinceStart);
            recordedActions.add(chatAction);

            if (client.player != null) {
                client.player.sendMessage(Text.literal("Recorded chat: " + message), true);
            }
        }
    }
    private void executeCommand(String command) {
        if (client.player == null || command == null || command.trim().isEmpty()) return;

        MinecraftClient.getInstance().execute(() -> {
            try {
                client.player.networkHandler.sendChatCommand(command);

                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Executed: /" + command), true);
                }
            } catch (Exception e) {
                System.err.println("Error executing command: " + e.getMessage());
            }
        });
    }
    private void executeChatMessage(String message) {
        if (client.player == null || message == null || message.trim().isEmpty()) return;

        MinecraftClient.getInstance().execute(() -> {
            try {
                client.player.networkHandler.sendChatMessage(message);

                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Sent: " + message), true);
                }
            } catch (Exception e) {
                System.err.println("Error sending chat message: " + e.getMessage());
            }
        });
    }

    private void executeMouseAction(RecordedAction action) {
        try {
            if (action.pressed) {
                switch (action.mouseButton) {
                    case 0:
                        client.interactionManager.attackEntity(client.player, client.targetedEntity);
                        client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                        break;

                    case 1:
                        if (client.crosshairTarget != null) {
                            client.interactionManager.interactBlock(client.player, net.minecraft.util.Hand.MAIN_HAND, (net.minecraft.util.hit.BlockHitResult) client.crosshairTarget);
                        } else {
                            client.interactionManager.interactItem(client.player, net.minecraft.util.Hand.MAIN_HAND);
                        }
                        break;

                    case 2:
                        break;
                }
            }
        } catch (Exception e) {
            fallbackToKeySimulation(action);
        }
    }

    private void fallbackToKeySimulation(RecordedAction action) {
        if (action.pressed) {
            switch (action.mouseButton) {
                case 0:
                    KeyBinding.setKeyPressed(client.options.attackKey.getDefaultKey(), true);
                    KeyBinding.onKeyPressed(client.options.attackKey.getDefaultKey());
                    break;
                case 1:
                    KeyBinding.setKeyPressed(client.options.useKey.getDefaultKey(), true);
                    KeyBinding.onKeyPressed(client.options.useKey.getDefaultKey());
                    break;
                case 2:
                    KeyBinding.setKeyPressed(client.options.pickItemKey.getDefaultKey(), true);
                    KeyBinding.onKeyPressed(client.options.pickItemKey.getDefaultKey());
                    break;
            }
        } else {
            switch (action.mouseButton) {
                case 0:
                    KeyBinding.setKeyPressed(client.options.attackKey.getDefaultKey(), false);
                    break;
                case 1:
                    KeyBinding.setKeyPressed(client.options.useKey.getDefaultKey(), false);
                    break;
                case 2:
                    KeyBinding.setKeyPressed(client.options.pickItemKey.getDefaultKey(), false);
                    break;
            }
        }
    }

    public void saveCurrentRecordingAsProfile(String name, String description) {
        if (recordedActions.isEmpty()) return;

        RecordingProfile profile = new RecordingProfile(name, recordedActions);
        profile.setDescription(description);

        if (profile.saveProfile()) {
            savedProfiles.add(profile);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Profile '" + name + "' saved successfully!"), true);
            }
        }
    }

    public void loadProfile(RecordingProfile profile) {
        if (profile == null) return;

        stopRecording();
        stopPlayback();

        currentProfile = profile;
        recordedActions.clear();
        recordedActions.addAll(profile.getActions());

        if (client.player != null) {
            client.player.sendMessage(Text.literal("Profile '" + profile.getName() + "' loaded with " +
                    recordedActions.size() + " actions"), true);
        }
    }

    public List<RecordingProfile> getSavedProfiles() {
        if (savedProfiles.isEmpty()) {
            savedProfiles = RecordingProfile.loadAllProfiles();
        }
        return new ArrayList<>(savedProfiles);
    }

    public void refreshProfiles() {
        savedProfiles = RecordingProfile.loadAllProfiles();
    }

    public void deleteProfile(RecordingProfile profile) {
        if (profile.deleteProfile()) {
            savedProfiles.remove(profile);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Profile '" + profile.getName() + "' deleted"), true);
            }
        }
    }

    public void onChatMessageSent(String message) {
        if (isRecording && recordChatMessages && message != null && !message.trim().isEmpty()) {
            long currentTime = System.currentTimeMillis();
            long timeSinceStart = currentTime - recordingStartTime;

            RecordedAction chatAction = new RecordedAction(message, timeSinceStart);
            recordedActions.add(chatAction);

            if (client.player != null) {
                client.player.sendMessage(Text.literal("Recorded chat: " + message), true);
            }
        }
    }

    private void initializeKeyStates() {
        previousKeyStates.clear();
        previousMouseStates.clear();
        KeyBinding[] keyBindings = getKeyBindings();

        for (KeyBinding keyBinding : keyBindings) {
            previousKeyStates.put(keyBinding, isKeyCurrentlyPressed(keyBinding));
        }
    }

    private KeyBinding[] getKeyBindings() {
        return new KeyBinding[] {
                client.options.forwardKey,
                client.options.backKey,
                client.options.leftKey,
                client.options.rightKey,
                client.options.jumpKey,
                client.options.sneakKey,
                client.options.sprintKey,
                client.options.attackKey,
                client.options.useKey,
                client.options.inventoryKey,
                client.options.chatKey,
                client.options.playerListKey,
                client.options.pickItemKey,
                client.options.commandKey,
                client.options.screenshotKey,
                client.options.togglePerspectiveKey,
                client.options.smoothCameraKey,
                client.options.fullscreenKey,
                client.options.spectatorOutlinesKey,
                client.options.swapHandsKey,
                client.options.saveToolbarActivatorKey,
                client.options.loadToolbarActivatorKey,
                client.options.hotbarKeys[0],
                client.options.hotbarKeys[1],
                client.options.hotbarKeys[2],
                client.options.hotbarKeys[3],
                client.options.hotbarKeys[4],
                client.options.hotbarKeys[5],
                client.options.hotbarKeys[6],
                client.options.hotbarKeys[7],
                client.options.hotbarKeys[8]
        };
    }

    public void cleanup() {
        stopPlayback();
        stopRecording();
    }

    public boolean isRecording() { return isRecording; }
    public boolean isPlayingBack() { return isPlayingBack; }
    public boolean isEnabled() { return isEnabled; }
    public boolean isLoopPlayback() { return loopPlayback; }
    public int getRecordedActionsCount() { return recordedActions.size(); }

    public void addManualAction(KeyBinding keyBinding, boolean pressed, long delay) {
        recordedActions.add(new RecordedAction(keyBinding, pressed, delay, 0));
    }

    public void clearRecording() {
        recordedActions.clear();
    }

    public List<RecordedAction> getRecordedActions() {
        return new ArrayList<>(recordedActions);
    }

    public void setRecordedActions(List<RecordedAction> actions) {
        recordedActions.clear();
        recordedActions.addAll(actions);
    }
    public void onCommandSent(String command) {
        if (isRecording && recordCommands && command != null && !command.trim().isEmpty()) {
            long currentTime = System.currentTimeMillis();
            long timeSinceStart = currentTime - recordingStartTime;

            RecordedAction commandAction = new RecordedAction(command, timeSinceStart, true);
            recordedActions.add(commandAction);

            if (client.player != null) {
                client.player.sendMessage(Text.literal("Recorded command: /" + command), true);
            }
        }
    }

}