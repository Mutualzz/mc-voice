package com.mutualzz.voice;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class VoiceClientController {
    public static final String MOD_ID = "mutualzz_voice";
    public static final String CATEGORY = "key.categories.mutualzz_voice";

    public static final class Keys {
        public KeyMapping mute;
        public KeyMapping deafen;
        public KeyMapping speakingHud;
        public KeyMapping ptt;
        public KeyMapping inputMode;
        public KeyMapping volumeUp;
        public KeyMapping volumeDown;
        public KeyMapping micSensUp;
        public KeyMapping micSensDown;
        public KeyMapping selectNext;
        public KeyMapping selectPrev;
        public KeyMapping userVolumeUp;
        public KeyMapping userVolumeDown;
        public KeyMapping userMute;
        public KeyMapping settings;
    }

    private static boolean pttWasDown;

    private VoiceClientController() {
    }

    public static void onClientTick(Minecraft client, Keys keys) {
        handlePtt(keys);

        while (keys.settings.consumeClick()) {
            if (!(client.screen instanceof VoiceSettingsScreen)) {
                client.setScreen(new VoiceSettingsScreen(client.screen));
            }
        }
        while (keys.mute.consumeClick()) {
            boolean next = !VoiceSession.isMuted();
            VoiceSession.setMuted(next);
            chat(client, next ? "Microphone muted" : "Microphone unmuted");
        }
        while (keys.deafen.consumeClick()) {
            boolean next = !VoiceSession.isDeafened();
            VoiceSession.setDeafened(next);
            chat(client, next ? "Deafened" : "Undeafened");
        }
        while (keys.speakingHud.consumeClick()) {
            VoiceSettings.get().toggleSpeakingHudVisible();
            chat(client, VoiceSettings.get().speakingHudVisible()
                    ? "Speaking list shown"
                    : "Speaking list hidden");
        }
        while (keys.inputMode.consumeClick()) {
            VoiceSettings.get().toggleInputMode();
            boolean ptt = VoiceSettings.get().isPushToTalk();
            chat(client, ptt
                    ? "Input: Push-to-talk (hold V)"
                    : "Input: Voice activity");
        }
        while (keys.volumeUp.consumeClick()) {
            VoiceSettings.get().adjustOutputVolume(0.1f);
            chat(client, "Output volume " + pct(VoiceSettings.get().outputVolume()) + "%");
        }
        while (keys.volumeDown.consumeClick()) {
            VoiceSettings.get().adjustOutputVolume(-0.1f);
            chat(client, "Output volume " + pct(VoiceSettings.get().outputVolume()) + "%");
        }
        while (keys.micSensUp.consumeClick()) {
            VoiceSettings.get().adjustMicSensitivity(0.1f);
            chat(client, "Mic sensitivity " + pct(VoiceSettings.get().micSensitivity()) + "%");
        }
        while (keys.micSensDown.consumeClick()) {
            VoiceSettings.get().adjustMicSensitivity(-0.1f);
            chat(client, "Mic sensitivity " + pct(VoiceSettings.get().micSensitivity()) + "%");
        }
        while (keys.selectNext.consumeClick()) {
            cycleSelection(1, client);
        }
        while (keys.selectPrev.consumeClick()) {
            cycleSelection(-1, client);
        }
        while (keys.userVolumeUp.consumeClick()) {
            adjustSelectedUserVolume(0.1f, client);
        }
        while (keys.userVolumeDown.consumeClick()) {
            adjustSelectedUserVolume(-0.1f, client);
        }
        while (keys.userMute.consumeClick()) {
            String id = VoiceSettings.get().selectedUserId();
            if (id.isBlank()) {
                chat(client, "Select a user first (Alt+[ / Alt+])");
            } else {
                VoiceSettings.get().toggleUserMuted(id);
                boolean muted = VoiceSettings.get().isUserMuted(id);
                chat(client, (muted ? "Muted " : "Unmuted ")
                        + SpeakingTracker.get().displayName(id) + " locally");
            }
        }
    }

    public static void renderHud(HudCanvas graphics) {
        VoiceSession.ConnectionState st = VoiceSession.connectionState();
        if (st == VoiceSession.ConnectionState.IDLE) return;

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

        graphics.drawText(label, 4, 4, color, true);

        int y = 16;
        int outPct = pct(VoiceSettings.get().outputVolume());
        graphics.drawText(
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
            graphics.drawText(
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
            graphics.drawText("Speaking: —", 4, y, 0xFFAAAAAA, true);
            return;
        }

        graphics.drawText("Speaking:", 4, y, 0xFFFFFFFF, true);
        y += 10;
        for (String name : speakers) {
            graphics.drawText("  " + name, 4, y, 0xFF55FF55, true);
            y += 10;
        }
    }

    private static void handlePtt(Keys keys) {
        if (!VoiceSettings.get().isPushToTalk()) {
            if (pttWasDown) {
                VoiceSession.setPttHeld(false);
                pttWasDown = false;
            }
            return;
        }
        boolean down = keys.ptt.isDown();
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
            chat(client, "Select a user first (Alt+[ / Alt+])");
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
