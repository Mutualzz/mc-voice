package com.mutualzz.voice;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * In-game Mutualzz voice settings (volumes, PTT, mute/deafen, per-user).
 */
public final class VoiceSettingsScreen extends Screen {
    private final Screen lastScreen;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

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
        layout.addTitleHeader(title, font);

        LinearLayout body = LinearLayout.vertical().spacing(6);
        body.defaultCellSetting().alignHorizontallyCenter();

        VoiceSettings settings = VoiceSettings.get();

        outputSlider = new PercentSlider(
                0, 0, 280, 20,
                "Output volume",
                0f, 2f,
                settings::outputVolume,
                settings::setOutputVolume
        );
        body.addChild(outputSlider);

        micSlider = new PercentSlider(
                0, 0, 280, 20,
                "Mic sensitivity",
                0.5f, 2f,
                settings::micSensitivity,
                settings::setMicSensitivity
        );
        body.addChild(micSlider);

        body.addChild(CycleButton.builder(
                        (VoiceSettings.InputMode mode) -> Component.literal(
                                mode == VoiceSettings.InputMode.PTT
                                        ? "Push to Talk"
                                        : "Voice Activity"
                        ),
                        settings.inputMode()
                )
                .withValues(VoiceSettings.InputMode.VOICE, VoiceSettings.InputMode.PTT)
                .create(0, 0, 280, 20, Component.literal("Input mode"), (btn, value) -> {
                    settings.setInputMode(value);
                }));

        body.addChild(CycleButton.onOffBuilder(settings.speakingHudVisible())
                .create(0, 0, 280, 20, Component.literal("Speaking list HUD"), (btn, value) -> {
                    settings.setSpeakingHudVisible(value);
                }));

        boolean rnnoiseOk = RnnoiseDenoiser.get().isAvailable();
        if (rnnoiseOk) {
            body.addChild(CycleButton.onOffBuilder(settings.noiseSuppression())
                    .create(0, 0, 280, 20, Component.literal("Noise suppression"), (btn, value) -> {
                        settings.setNoiseSuppression(value);
                    }));
        } else {
            body.addChild(new StringWidget(
                    Component.literal("Noise suppression unavailable (natives)"),
                    font
            ));
        }

        LinearLayout selfRow = LinearLayout.horizontal().spacing(6);
        muteSelfButton = Button.builder(muteSelfLabel(), b -> {
            VoiceSession.setMuted(!VoiceSession.isMuted());
            refreshSelfButtons();
        }).width(137).build();
        deafenButton = Button.builder(deafenLabel(), b -> {
            VoiceSession.setDeafened(!VoiceSession.isDeafened());
            refreshSelfButtons();
        }).width(137).build();
        selfRow.addChild(muteSelfButton);
        selfRow.addChild(deafenButton);
        body.addChild(selfRow);

        body.addChild(new StringWidget(Component.literal("Per-user"), font));

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

        userPicker = CycleButton.builder(
                        (String id) -> Component.literal(
                                id.isBlank()
                                        ? "(none)"
                                        : SpeakingTracker.get().displayName(id)
                                                + (VoiceSettings.get().isUserMuted(id) ? " [muted]" : "")
                        ),
                        selected
                )
                .withValues(pickerIds)
                .create(0, 0, 280, 20, Component.literal("User"), (btn, id) -> {
                    VoiceSettings.get().setSelectedUserId(id);
                    refreshUserControls();
                });
        body.addChild(userPicker);

        userVolumeSlider = new PercentSlider(
                0, 0, 280, 20,
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
        body.addChild(userVolumeSlider);

        muteUserButton = Button.builder(muteUserLabel(), b -> {
            String id = VoiceSettings.get().selectedUserId();
            if (id.isBlank()) return;
            VoiceSettings.get().toggleUserMuted(id);
            refreshUserControls();
            if (userPicker != null) {
                userPicker.setValue(id);
            }
        }).width(280).build();
        body.addChild(muteUserButton);

        if (userIds.isEmpty()) {
            body.addChild(new StringWidget(
                    Component.literal("Join voice to adjust other users"),
                    font
            ));
        }

        layout.addToContents(body);

        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .width(200)
                .build());

        layout.visitWidgets(this::addRenderableWidget);
        layout.arrangeElements();
        refreshUserControls();
    }

    @Override
    public void tick() {
        refreshSelfButtons();
    }

    @Override
    public void onClose() {
        minecraft.setScreen(lastScreen);
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
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
