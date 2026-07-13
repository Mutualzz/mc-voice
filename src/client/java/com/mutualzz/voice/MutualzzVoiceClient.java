package com.mutualzz.voice;

import de.siphalor.amecs.key_modifiers.api.AmecsKeyMappingWithKeyModifiers;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifierCombination;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifiers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MutualzzVoiceClient implements ClientModInitializer {
    public static final String MOD_ID = "mutualzz_voice";

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(MOD_ID, "voice")
    );

    private static final AmecsKeyModifierCombination CTRL =
            new AmecsKeyModifierCombination(AmecsKeyModifiers.CONTROL);
    private static final AmecsKeyModifierCombination CTRL_SHIFT =
            new AmecsKeyModifierCombination(AmecsKeyModifiers.CONTROL, AmecsKeyModifiers.SHIFT);

    private static KeyMapping muteKey;
    private static KeyMapping deafenKey;
    private static KeyMapping speakingHudKey;
    private static KeyMapping pttKey;
    private static KeyMapping inputModeKey;
    private static KeyMapping volumeUpKey;
    private static KeyMapping volumeDownKey;
    private static KeyMapping micSensUpKey;
    private static KeyMapping micSensDownKey;
    private static KeyMapping selectNextKey;
    private static KeyMapping selectPrevKey;
    private static KeyMapping userVolumeUpKey;
    private static KeyMapping userVolumeDownKey;
    private static KeyMapping userMuteKey;
    private static KeyMapping settingsKey;

    private static boolean pttWasDown;

    @Override
    public void onInitializeClient() {
        VoiceSettings.get().load();
        VoicePayload.register();

        ClientPlayNetworking.registerGlobalReceiver(VoicePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> VoiceSession.handlePluginMessage(payload.json()));
        });

        muteKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                CATEGORY,
                CTRL
        ));

        deafenKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.deafen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_D,
                CATEGORY,
                CTRL_SHIFT
        ));

        speakingHudKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.speaking_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY,
                CTRL
        ));

        pttKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mutualzz_voice.ptt",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        inputModeKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.input_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                CATEGORY,
                CTRL
        ));

        volumeUpKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.volume_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                CATEGORY,
                CTRL
        ));

        volumeDownKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.volume_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                CATEGORY,
                CTRL
        ));

        micSensUpKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.mic_sens_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                CATEGORY,
                CTRL_SHIFT
        ));

        micSensDownKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.mic_sens_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                CATEGORY,
                CTRL_SHIFT
        ));

        selectNextKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.select_next",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                CATEGORY,
                CTRL
        ));

        selectPrevKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.select_prev",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                CATEGORY,
                CTRL
        ));

        userVolumeUpKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.user_volume_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                CATEGORY,
                CTRL
        ));

        userVolumeDownKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.user_volume_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                CATEGORY,
                CTRL
        ));

        userMuteKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.user_mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                CATEGORY,
                CTRL
        ));

        settingsKey = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY,
                CTRL
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handlePtt(client);

            while (settingsKey.consumeClick()) {
                if (!(client.screen instanceof VoiceSettingsScreen)) {
                    client.setScreen(new VoiceSettingsScreen(client.screen));
                }
            }
            while (muteKey.consumeClick()) {
                boolean next = !VoiceSession.isMuted();
                VoiceSession.setMuted(next);
                chat(client, next ? "Microphone muted" : "Microphone unmuted");
            }
            while (deafenKey.consumeClick()) {
                boolean next = !VoiceSession.isDeafened();
                VoiceSession.setDeafened(next);
                chat(client, next ? "Deafened" : "Undeafened");
            }
            while (speakingHudKey.consumeClick()) {
                VoiceSettings.get().toggleSpeakingHudVisible();
                chat(client, VoiceSettings.get().speakingHudVisible()
                        ? "Speaking list shown"
                        : "Speaking list hidden");
            }
            while (inputModeKey.consumeClick()) {
                VoiceSettings.get().toggleInputMode();
                boolean ptt = VoiceSettings.get().isPushToTalk();
                chat(client, ptt
                        ? "Input: Push-to-talk (hold V)"
                        : "Input: Voice activity");
            }
            while (volumeUpKey.consumeClick()) {
                VoiceSettings.get().adjustOutputVolume(0.1f);
                chat(client, "Output volume " + pct(VoiceSettings.get().outputVolume()) + "%");
            }
            while (volumeDownKey.consumeClick()) {
                VoiceSettings.get().adjustOutputVolume(-0.1f);
                chat(client, "Output volume " + pct(VoiceSettings.get().outputVolume()) + "%");
            }
            while (micSensUpKey.consumeClick()) {
                VoiceSettings.get().adjustMicSensitivity(0.1f);
                chat(client, "Mic sensitivity " + pct(VoiceSettings.get().micSensitivity()) + "%");
            }
            while (micSensDownKey.consumeClick()) {
                VoiceSettings.get().adjustMicSensitivity(-0.1f);
                chat(client, "Mic sensitivity " + pct(VoiceSettings.get().micSensitivity()) + "%");
            }
            while (selectNextKey.consumeClick()) {
                cycleSelection(1, client);
            }
            while (selectPrevKey.consumeClick()) {
                cycleSelection(-1, client);
            }
            while (userVolumeUpKey.consumeClick()) {
                adjustSelectedUserVolume(0.1f, client);
            }
            while (userVolumeDownKey.consumeClick()) {
                adjustSelectedUserVolume(-0.1f, client);
            }
            while (userMuteKey.consumeClick()) {
                String id = VoiceSettings.get().selectedUserId();
                if (id.isBlank()) {
                    chat(client, "Select a user first (Ctrl+[ / Ctrl+])");
                } else {
                    VoiceSettings.get().toggleUserMuted(id);
                    boolean muted = VoiceSettings.get().isUserMuted(id);
                    chat(client, (muted ? "Muted " : "Unmuted ")
                            + SpeakingTracker.get().displayName(id) + " locally");
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            VoiceSession.leave();
        });

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "status"),
                (graphics, tickCounter) -> {
                    VoiceSession.ConnectionState st = VoiceSession.connectionState();
                    if (st == VoiceSession.ConnectionState.IDLE) return;

                    var client = Minecraft.getInstance();
                    boolean deafened = VoiceSession.isDeafened();
                    boolean muted = VoiceSession.isMuted();
                    boolean ptt = VoiceSettings.get().isPushToTalk();
                    boolean talking = ptt && VoiceSession.isPttHeld() && !VoiceSession.isMicSilenced();

                    String label;
                    int color;
                    if (st == VoiceSession.ConnectionState.CONNECTING) {
                        label = "Mutualzz Voice (connecting…)";
                        color = 0xFFAAAAAA;
                    } else if (st == VoiceSession.ConnectionState.RECONNECTING) {
                        label = "Mutualzz Voice (reconnecting…)";
                        color = 0xFFFFFF55;
                    } else if (deafened) {
                        label = "Mutualzz Voice (deafened)";
                        color = 0xFFFF5555;
                    } else if (muted) {
                        label = "Mutualzz Voice (muted)";
                        color = 0xFFFFAA00;
                    } else if (ptt && !talking) {
                        label = "Mutualzz Voice (PTT)";
                        color = 0xFF55FFFF;
                    } else if (talking) {
                        label = "Mutualzz Voice (talking)";
                        color = 0xFF55FF55;
                    } else {
                        label = "Mutualzz Voice";
                        color = 0xFF55FF55;
                    }

                    String channel = VoiceSession.channelLabel();
                    if (!channel.isBlank() && st == VoiceSession.ConnectionState.CONNECTED) {
                        label = label + " · " + channel;
                    }

                    graphics.text(client.font, label, 4, 4, color, true);

                    int y = 16;
                    int outPct = pct(VoiceSettings.get().outputVolume());
                    graphics.text(
                            client.font,
                            "Vol " + outPct + "% · Mic " + pct(VoiceSettings.get().micSensitivity()) + "%",
                            4,
                            y,
                            0xFFAAAAAA,
                            true
                    );
                    y += 10;

                    if (!VoiceSettings.get().speakingHudVisible()) return;
                    if (st != VoiceSession.ConnectionState.CONNECTED) return;

                    String selected = VoiceSettings.get().selectedUserId();
                    if (!selected.isBlank()) {
                        String selLabel = SpeakingTracker.get().displayName(selected);
                        int selVol = pct(VoiceSettings.get().userVolume(selected));
                        String muteTag = VoiceSettings.get().isUserMuted(selected) ? " [muted]" : "";
                        graphics.text(
                                client.font,
                                "Selected: " + selLabel + " " + selVol + "%" + muteTag,
                                4,
                                y,
                                0xFF55FFFF,
                                true
                        );
                        y += 10;
                    }

                    List<String> speakers = SpeakingTracker.get().speakingNames();
                    if (speakers.isEmpty()) {
                        graphics.text(
                                client.font,
                                "Speaking: —",
                                4,
                                y,
                                0xFFAAAAAA,
                                true
                        );
                        return;
                    }

                    graphics.text(
                            client.font,
                            "Speaking:",
                            4,
                            y,
                            0xFFFFFFFF,
                            true
                    );
                    y += 10;
                    for (String name : speakers) {
                        graphics.text(
                                client.font,
                                "  " + name,
                                4,
                                y,
                                0xFF55FF55,
                                true
                        );
                        y += 10;
                    }
                }
        );
    }

    private static void handlePtt(Minecraft client) {
        if (!VoiceSettings.get().isPushToTalk()) {
            if (pttWasDown) {
                VoiceSession.setPttHeld(false);
                pttWasDown = false;
            }
            return;
        }
        boolean down = pttKey.isDown();
        if (down != pttWasDown) {
            VoiceSession.setPttHeld(down);
            pttWasDown = down;
        }
    }

    private static void cycleSelection(int delta, Minecraft client) {
        List<String> ids = SpeakingTracker.get().otherRosterIds();
        if (ids.isEmpty()) {
            chat(client, "No other users in voice yet");
            return;
        }
        String current = VoiceSettings.get().selectedUserId();
        int idx = ids.indexOf(current);
        if (idx < 0) idx = delta > 0 ? -1 : 0;
        idx = Math.floorMod(idx + delta, ids.size());
        String next = ids.get(idx);
        VoiceSettings.get().setSelectedUserId(next);
        chat(client, "Selected " + SpeakingTracker.get().displayName(next)
                + " (" + pct(VoiceSettings.get().userVolume(next)) + "%)");
    }

    private static void adjustSelectedUserVolume(float delta, Minecraft client) {
        String id = VoiceSettings.get().selectedUserId();
        if (id.isBlank()) {
            chat(client, "Select a user first (Ctrl+[ / Ctrl+])");
            return;
        }
        VoiceSettings.get().adjustUserVolume(id, delta);
        chat(client, SpeakingTracker.get().displayName(id)
                + " volume " + pct(VoiceSettings.get().userVolume(id)) + "%");
    }

    private static int pct(float gain) {
        return Math.round(gain * 100f);
    }

    private static void chat(Minecraft client, String message) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("[Mutualzz] " + message));
        }
    }
}
