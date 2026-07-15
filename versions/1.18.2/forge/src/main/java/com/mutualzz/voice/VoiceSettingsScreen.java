package com.mutualzz.voice;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;

import java.util.List;

/** Simplified settings for 1.18.2 (no CycleButton / Button.builder). */
public final class VoiceSettingsScreen extends Screen {
    private final Screen lastScreen;
    private PercentSlider outputSlider;
    private PercentSlider micSlider;
    private Button muteSelfButton;
    private Button deafenButton;
    private Button inputModeButton;
    private Button hudButton;
    private Button noiseButton;

    public VoiceSettingsScreen(Screen lastScreen) {
        super(new TextComponent("Mutualzz Voice Settings"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        VoiceSettings settings = VoiceSettings.get();
        int cx = width / 2;
        int y = 32;

        Button titleBtn = new Button(cx - 140, y, 280, 20, title, b -> {
        });
        titleBtn.active = false;
        addRenderableWidget(titleBtn);
        y += 28;

        outputSlider = new PercentSlider(
                cx - 140, y, 280, 20, "Output volume", 0f, 2f,
                settings::outputVolume, settings::setOutputVolume
        );
        addRenderableWidget(outputSlider);
        y += 24;

        micSlider = new PercentSlider(
                cx - 140, y, 280, 20, "Mic sensitivity", 0.5f, 2f,
                settings::micSensitivity, settings::setMicSensitivity
        );
        addRenderableWidget(micSlider);
        y += 24;

        inputModeButton = addRenderableWidget(new Button(cx - 140, y, 280, 20, inputModeLabel(), b -> {
            VoiceSettings.InputMode next = settings.inputMode() == VoiceSettings.InputMode.PTT
                    ? VoiceSettings.InputMode.VOICE
                    : VoiceSettings.InputMode.PTT;
            settings.setInputMode(next);
            inputModeButton.setMessage(inputModeLabel());
        }));
        y += 24;

        hudButton = addRenderableWidget(new Button(cx - 140, y, 280, 20, hudLabel(), b -> {
            settings.setSpeakingHudVisible(!settings.speakingHudVisible());
            hudButton.setMessage(hudLabel());
        }));
        y += 24;

        if (RnnoiseDenoiser.get().isAvailable()) {
            noiseButton = addRenderableWidget(new Button(cx - 140, y, 280, 20, noiseLabel(), b -> {
                settings.setNoiseSuppression(!settings.noiseSuppression());
                noiseButton.setMessage(noiseLabel());
            }));
        }
        y += 28;

        muteSelfButton = addRenderableWidget(new Button(cx - 140, y, 137, 20, muteSelfLabel(), b -> {
            VoiceSession.setMuted(!VoiceSession.isMuted());
            muteSelfButton.setMessage(muteSelfLabel());
        }));
        deafenButton = addRenderableWidget(new Button(cx + 3, y, 137, 20, deafenLabel(), b -> {
            VoiceSession.setDeafened(!VoiceSession.isDeafened());
            deafenButton.setMessage(deafenLabel());
        }));
        y += 28;

        List<String> ids = SpeakingTracker.get().otherRosterIds();
        if (!ids.isEmpty()) {
            addRenderableWidget(new Button(cx - 140, y, 280, 20, new TextComponent("Select next user"), b -> {
                String cur = VoiceSettings.get().selectedUserId();
                int idx = ids.indexOf(cur);
                idx = (idx + 1) % ids.size();
                VoiceSettings.get().setSelectedUserId(ids.get(idx));
            }));
            y += 24;
        }

        addRenderableWidget(new Button(
                cx - 100, Math.min(y, height - 28), 200, 20,
                CommonComponents.GUI_DONE, b -> onClose()
        ));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, font, title, width / 2, 12, 0xFFFFFF);
    }

    @Override
    public void tick() {
        if (muteSelfButton != null) muteSelfButton.setMessage(muteSelfLabel());
        if (deafenButton != null) deafenButton.setMessage(deafenLabel());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    private static net.minecraft.network.chat.Component muteSelfLabel() {
        return new TextComponent(VoiceSession.isMuted() ? "Unmute Mic" : "Mute Mic");
    }

    private static net.minecraft.network.chat.Component deafenLabel() {
        return new TextComponent(VoiceSession.isDeafened() ? "Undeafen" : "Deafen");
    }

    private static net.minecraft.network.chat.Component inputModeLabel() {
        return new TextComponent("Input: " + (VoiceSettings.get().inputMode() == VoiceSettings.InputMode.PTT
                ? "Push to Talk" : "Voice Activity"));
    }

    private static net.minecraft.network.chat.Component hudLabel() {
        return new TextComponent("Speaking HUD: " + (VoiceSettings.get().speakingHudVisible() ? "On" : "Off"));
    }

    private static net.minecraft.network.chat.Component noiseLabel() {
        return new TextComponent("Noise suppression: " + (VoiceSettings.get().noiseSuppression() ? "On" : "Off"));
    }
}
