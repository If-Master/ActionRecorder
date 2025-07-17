package me.kanuunankuulaspluginsMobs.actionrecorder.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import java.util.List;

public class ActionRecorderConfigScreen extends Screen {
    private final Screen parent;
    private Actionrecorder recorder;
    private ButtonWidget recordButton;
    private ButtonWidget playbackButton;
    private ButtonWidget enableButton;
    private ButtonWidget clearButton;
    private ButtonWidget saveProfileButton;
    private ButtonWidget manageProfilesButton;
    private ButtonWidget mouseRecordButton;
    private ButtonWidget chatRecordButton;

    private TextFieldWidget profileNameField;
    private TextFieldWidget profileDescField;
    private boolean showSaveDialog = false;
    private boolean showProfileManager = false;
    private ProfileManagerScreen profileManager;

    public ActionRecorderConfigScreen(Screen parent) {
        super(Text.literal("Action Recorder Configuration"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        try {
            this.recorder = ActionrecorderClient.actionRecorder;
        } catch (Exception e) {
            this.recorder = null;
        }

        if (this.recorder == null) {
            showErrorScreen();
            return;
        }

        if (showSaveDialog) {
            initSaveDialog();
        } else if (showProfileManager) {
            initProfileManager();
        } else {
            initMainScreen();
        }
    }

    private void initMainScreen() {
        int centerX = this.width / 2;
        int startY = this.height / 6;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        int currentY = startY;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Status: " + getStatusText()),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Recorded Actions: " + recorder.getRecordedActionsCount()),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing;

        recordButton = ButtonWidget.builder(
                Text.literal(recorder.isRecording() ? "Stop Recording" : "Start Recording"),
                button -> {
                    recorder.toggleRecording();
                    updateButtonTexts();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(recordButton);
        currentY += spacing;

        playbackButton = ButtonWidget.builder(
                Text.literal(recorder.isPlayingBack() ? "Stop Playback" : "Start Playback"),
                button -> {
                    recorder.togglePlayback();
                    updateButtonTexts();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(playbackButton);
        currentY += spacing;

        mouseRecordButton = ButtonWidget.builder(
                Text.literal("Mouse Recording: " + (recorder.isRecordMouseClicks() ? "ON" : "OFF")),
                button -> {
                    recorder.setRecordMouseClicks(!recorder.isRecordMouseClicks());
                    updateButtonTexts();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(mouseRecordButton);
        currentY += spacing;

        chatRecordButton = ButtonWidget.builder(
                Text.literal("Chat Recording: " + (recorder.isRecordChatMessages() ? "ON" : "OFF")),
                button -> {
                    recorder.setRecordChatMessages(!recorder.isRecordChatMessages());
                    updateButtonTexts();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(chatRecordButton);
        currentY += spacing;

        saveProfileButton = ButtonWidget.builder(
                Text.literal("Save as Profile"),
                button -> {
                    showSaveDialog = true;
                    this.clearAndInit();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(saveProfileButton);
        currentY += spacing;

        manageProfilesButton = ButtonWidget.builder(
                Text.literal("Manage Profiles"),
                button -> {
                    showProfileManager = true;
                    this.clearAndInit();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(manageProfilesButton);
        currentY += spacing;

        enableButton = ButtonWidget.builder(
                Text.literal(recorder.isEnabled() ? "Disable Recorder" : "Enable Recorder"),
                button -> {
                    recorder.toggleEnabled();
                    updateButtonTexts();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(enableButton);
        currentY += spacing;

        clearButton = ButtonWidget.builder(
                Text.literal("Clear Recording"),
                button -> {
                    recorder.clearRecording();
                    updateButtonTexts();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build();
        this.addDrawableChild(clearButton);
        currentY += spacing;

        this.addDrawableChild(ButtonWidget.builder(
                ScreenTexts.DONE,
                button -> this.close()
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
    }

    private void initSaveDialog() {
        int centerX = this.width / 2;
        int startY = this.height / 4;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        int currentY = startY;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save Recording Profile"),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing * 2;

        profileNameField = new TextFieldWidget(this.textRenderer,
                centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight,
                Text.literal("Profile Name"));
        profileNameField.setPlaceholder(Text.literal("Enter profile name"));
        this.addDrawableChild(profileNameField);
        currentY += spacing;

        profileDescField = new TextFieldWidget(this.textRenderer,
                centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight,
                Text.literal("Description"));
        profileDescField.setPlaceholder(Text.literal("Enter description (optional)"));
        this.addDrawableChild(profileDescField);
        currentY += spacing * 2;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Save Profile"),
                button -> {
                    String name = profileNameField.getText().trim();
                    String desc = profileDescField.getText().trim();

                    if (!name.isEmpty()) {
                        recorder.saveCurrentRecordingAsProfile(name, desc);
                        showSaveDialog = false;
                        this.clearAndInit();
                    }
                }
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth / 2 - 5, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Cancel"),
                button -> {
                    showSaveDialog = false;
                    this.clearAndInit();
                }
        ).dimensions(centerX + 5, currentY, buttonWidth / 2 - 5, buttonHeight).build());
    }

    private void initProfileManager() {
        if (profileManager == null) {
            profileManager = new ProfileManagerScreen(this, recorder);
        }

        int centerX = this.width / 2;
        int startY = this.height / 6;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        int currentY = startY;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Profile Manager"),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
        currentY += spacing * 2;

        List<RecordingProfile> profiles = recorder.getSavedProfiles();

        for (RecordingProfile profile : profiles) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(profile.getName() + " (" + profile.getActionCount() + " actions)"),
                    button -> {}
            ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth * 2 / 3, buttonHeight).build());

            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("Load"),
                    button -> {
                        recorder.loadProfile(profile);
                        showProfileManager = false;
                        this.clearAndInit();
                    }
            ).dimensions(centerX + buttonWidth / 2 - buttonWidth / 3 + 5, currentY,
                    buttonWidth / 3 - 10, buttonHeight).build());

            currentY += spacing;
        }

        if (profiles.isEmpty()) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal("No saved profiles"),
                    button -> {}
            ).dimensions(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight).build());
            currentY += spacing;
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Back"),
                button -> {
                    showProfileManager = false;
                    this.clearAndInit();
                }
        ).dimensions(centerX - buttonWidth / 2, currentY + spacing, buttonWidth, buttonHeight).build());
    }

    private void showErrorScreen() {
        int centerX = this.width / 2;
        int startY = this.height / 4;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Action Recorder not initialized!"),
                button -> {}
        ).dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        this.addDrawableChild(ButtonWidget.builder(
                ScreenTexts.DONE,
                button -> this.close()
        ).dimensions(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight).build());
    }

    private void updateButtonTexts() {
        if (recorder == null) return;

        try {
            if (recordButton != null) {
                recordButton.setMessage(Text.literal(recorder.isRecording() ? "Stop Recording" : "Start Recording"));
            }
            if (playbackButton != null) {
                playbackButton.setMessage(Text.literal(recorder.isPlayingBack() ? "Stop Playback" : "Start Playback"));
            }
            if (enableButton != null) {
                enableButton.setMessage(Text.literal(recorder.isEnabled() ? "Disable Recorder" : "Enable Recorder"));
            }
            if (mouseRecordButton != null) {
                mouseRecordButton.setMessage(Text.literal("Mouse Recording: " + (recorder.isRecordMouseClicks() ? "ON" : "OFF")));
            }
            if (chatRecordButton != null) {
                chatRecordButton.setMessage(Text.literal("Chat Recording: " + (recorder.isRecordChatMessages() ? "ON" : "OFF")));
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );

        if (showSaveDialog) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("Actions to save: " + recorder.getRecordedActionsCount()),
                    this.width / 2,
                    this.height / 4 + 40,
                    0xAAAAAA
            );
        }
    }

    private String getStatusText() {
        if (recorder == null) return "Not Available";

        try {
            if (recorder.isRecording()) return "Recording";
            if (recorder.isPlayingBack()) return "Playing Back";
            if (!recorder.isEnabled()) return "Disabled";
            return "Idle";
        } catch (Exception e) {
            return "Error";
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private static class ProfileManagerScreen {
        private final ActionRecorderConfigScreen parent;
        private final Actionrecorder recorder;
        private int selectedProfileIndex = -1;

        public ProfileManagerScreen(ActionRecorderConfigScreen parent, Actionrecorder recorder) {
            this.parent = parent;
            this.recorder = recorder;
        }

        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        }
    }
}