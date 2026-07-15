package com.mutualzz.voice;

/**
 * Loader-specific networking plugs in a {@link Backend} at client init.
 * Backend is UTF-8 JSON (same body Paper MutualzzBridge expects on {@code mutualzz:voice}).
 */
public final class VoiceNetworking {
    @FunctionalInterface
    public interface Backend {
        void sendJson(String json);
    }

    private static volatile Backend backend;

    private VoiceNetworking() {
    }

    public static void setBackend(Backend backend) {
        VoiceNetworking.backend = backend;
    }

    public static void sendJson(String json) {
        Backend b = backend;
        if (b != null) {
            b.sendJson(json);
        }
    }

    /** Tell the Paper plugin (and hub) about mute/deafen so the Mutualzz app UI updates. */
    public static void sendVoiceState(boolean selfMute, boolean selfDeaf) {
        String json = "{\"t\":\"voice_state\",\"selfMute\":" + selfMute
                + ",\"selfDeaf\":" + selfDeaf + "}";
        sendJson(json);
    }
}
