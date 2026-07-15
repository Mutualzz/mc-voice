package com.mutualzz.voice;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VoiceSettingsScreen extends Screen {
    private final Screen lastScreen;

    private PercentSlider outputSlider;
    private PercentSlider micSlider;
    private PercentSlider userVolumeSlider;
    private CycleButton<String> userPicker;
    private Button muteSelfButton;
    private Button deafenButton;
    private Button muteUserButton;

    public VoiceSettingsScreen(Screen lastScreen) {
        super(Component.literal("Mutualzz Voice Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        clearWidgets();
        VoiceSettings settings = VoiceSettings.get();
        int cx = width / 2;
        int y = 32;

        addRenderableWidget(Button.builder(title, b -> {
        }).bounds(cx - 140, y, 280, 20).build()).active = false;
        y += 28;

        outputSlider = new PercentSlider(
                cx - 140, y, 280, 20,
                "Output volume",
                0f, 2f,
                settings::outputVolume,
                settings::setOutputVolume
        );
        addRenderableWidget(outputSlider);
        y += 24;

        micSlider = new PercentSlider(
                cx - 140, y, 280, 20,
                "Mic sensitivity",
                0.5f, 2f,
                settings::micSensitivity,
                settings::setMicSensitivity
        );
        addRenderableWidget(micSlider);
        y += 24;

        addRenderableWidget(CycleButton.builder((VoiceSettings.InputMode mode) -> Component.literal(
                        mode == VoiceSettings.InputMode.PTT ? "Push to Talk" : "Voice Activity"))
                .withValues(VoiceSettings.InputMode.VOICE, VoiceSettings.InputMode.PTT)
                .withInitialValue(settings.inputMode())
                .create(cx - 140, y, 280, 20, Component.literal("Input mode"), (btn, value) -> {
                    settings.setInputMode(value);
                }));
        y += 24;

        addRenderableWidget(CycleButton.onOffBuilder(settings.speakingHudVisible())
                .create(cx - 140, y, 280, 20, Component.literal("Speaking list HUD"), (btn, value) -> {
                    settings.setSpeakingHudVisible(value);
                }));
        y += 24;

        if (RnnoiseDenoiser.get().isAvailable()) {
            addRenderableWidget(CycleButton.onOffBuilder(settings.noiseSuppression())
                    .create(cx - 140, y, 280, 20, Component.literal("Noise suppression"), (btn, value) -> {
                        settings.setNoiseSuppression(value);
                    }));
        }
        y += 28;

        muteSelfButton = addRenderableWidget(Button.builder(muteSelfLabel(), b -> {
            VoiceSession.setMuted(!VoiceSession.isMuted());
            refreshSelfButtons();
        }).bounds(cx - 140, y, 137, 20).build());
        deafenButton = addRenderableWidget(Button.builder(deafenLabel(), b -> {
            VoiceSession.setDeafened(!VoiceSession.isDeafened());
            refreshSelfButtons();
        }).bounds(cx + 3, y, 137, 20).build());
        y += 28;

        List<String> userIds = SpeakingTracker.get().otherRosterIds();
        List<String> pickerIds = new ArrayList<>();
        pickerIds.add("");
        pickerIds.addAll(userIds);

        String selected = settings.selectedUserId();
        if (!selected.isBlank() && !pickerIds.contains(selected)) {
            pickerIds.add(selected);
        }
        if (!pickerIds.contains(selected)) {
            selected = "";
            settings.setSelectedUserId("");
        }

        userPicker = addRenderableWidget(CycleButton.builder(
                        (String id) -> Component.literal(
                                id.isBlank()
                                        ? "(none)"
                                        : SpeakingTracker.get().displayName(id)
                                        + (VoiceSettings.get().isUserMuted(id) ? " [muted]" : "")
                        ))
                .withValues(pickerIds)
                .withInitialValue(selected)
                .create(cx - 140, y, 280, 20, Component.literal("User"), (btn, id) -> {
                    VoiceSettings.get().setSelectedUserId(id);
                    refreshUserControls();
                }));
        y += 24;

        userVolumeSlider = new PercentSlider(
                cx - 140, y, 280, 20,
                "User volume",
                0f, 2f,
                () -> {
                    String id = VoiceSettings.get().selectedUserId();
                    return id.isBlank() ? 1.0 : VoiceSettings.get().userVolume(id);
                },
                value -> {
                    String id = VoiceSettings.get().selectedUserId();
                    if (!id.isBlank()) {
                        VoiceSettings.get().setUserVolume(id, value);
                    }
                }
        );
        addRenderableWidget(userVolumeSlider);
        y += 24;

        muteUserButton = addRenderableWidget(Button.builder(muteUserLabel(), b -> {
            String id = VoiceSettings.get().selectedUserId();
            if (id.isBlank()) return;
            VoiceSettings.get().toggleUserMuted(id);
            refreshUserControls();
            if (userPicker != null) {
                userPicker.setValue(id);
            }
        }).bounds(cx - 140, y, 280, 20).build());
        y += 28;

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx - 100, Math.min(y, height - 28), 200, 20)
                .build());

        refreshUserControls();
    }


    @Override
    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, font, title, width / 2, 12, 0xFFFFFF);
    }


    @Override
    public void tick() {
        refreshSelfButtons();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    private void refreshSelfButtons() {
        if (muteSelfButton != null) muteSelfButton.setMessage(muteSelfLabel());
        if (deafenButton != null) deafenButton.setMessage(deafenLabel());
    }

    private void refreshUserControls() {
        String id = VoiceSettings.get().selectedUserId();
        boolean hasUser = !id.isBlank();
        if (userVolumeSlider != null) {
            userVolumeSlider.active = hasUser;
            userVolumeSlider.refreshFromSettings();
        }
        if (muteUserButton != null) {
            muteUserButton.active = hasUser;
            muteUserButton.setMessage(muteUserLabel());
        }
    }

    private static Component muteSelfLabel() {
        return Component.literal(VoiceSession.isMuted() ? "Unmute Mic" : "Mute Mic");
    }

    private static Component deafenLabel() {
        return Component.literal(VoiceSession.isDeafened() ? "Undeafen" : "Deafen");
    }

    private static Component muteUserLabel() {
        String id = VoiceSettings.get().selectedUserId();
        if (id.isBlank()) return Component.literal("Mute User Locally");
        boolean muted = VoiceSettings.get().isUserMuted(id);
        String name = SpeakingTracker.get().displayName(id);
        return Component.literal((muted ? "Unmute " : "Mute ") + name);
    }
}
