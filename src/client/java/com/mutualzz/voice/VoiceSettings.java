package com.mutualzz.voice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persisted Mutualzz voice QoL settings (volumes, PTT, per-user mute).
 */
public final class VoiceSettings {
    public enum InputMode {
        VOICE,
        PTT
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final VoiceSettings INSTANCE = new VoiceSettings();

    /** 0.0–2.0, default 1.0 */
    private volatile float outputVolume = 1.0f;
    /** 0.5–2.0, default 1.0 — scales uplink PCM amplitude */
    private volatile float micSensitivity = 1.0f;
    private volatile InputMode inputMode = InputMode.VOICE;
    private volatile boolean speakingHudVisible = true;
    private final Map<String, Float> userVolumes = new ConcurrentHashMap<>();
    private final Set<String> mutedUsers = ConcurrentHashMap.newKeySet();
    private volatile String selectedUserId = "";

    private VoiceSettings() {
    }

    public static VoiceSettings get() {
        return INSTANCE;
    }

    public float outputVolume() {
        return outputVolume;
    }

    public float micSensitivity() {
        return micSensitivity;
    }

    public InputMode inputMode() {
        return inputMode;
    }

    public boolean isPushToTalk() {
        return inputMode == InputMode.PTT;
    }

    public boolean speakingHudVisible() {
        return speakingHudVisible;
    }

    public void setSpeakingHudVisible(boolean visible) {
        speakingHudVisible = visible;
        save();
    }

    public void toggleSpeakingHudVisible() {
        setSpeakingHudVisible(!speakingHudVisible);
    }

    public String selectedUserId() {
        return selectedUserId;
    }

    public void setSelectedUserId(String id) {
        selectedUserId = id == null ? "" : id;
    }

    public void setOutputVolume(float value) {
        outputVolume = clamp(value, 0f, 2f);
        save();
    }

    public void adjustOutputVolume(float delta) {
        setOutputVolume(outputVolume + delta);
    }

    public void setMicSensitivity(float value) {
        micSensitivity = clamp(value, 0.5f, 2f);
        save();
    }

    public void adjustMicSensitivity(float delta) {
        setMicSensitivity(micSensitivity + delta);
    }

    public void setInputMode(InputMode mode) {
        if (mode == null) return;
        inputMode = mode;
        save();
    }

    public void toggleInputMode() {
        setInputMode(inputMode == InputMode.PTT ? InputMode.VOICE : InputMode.PTT);
    }

    public float userVolume(String userId) {
        if (userId == null || userId.isBlank()) return 1f;
        return userVolumes.getOrDefault(userId, 1f);
    }

    public void setUserVolume(String userId, float value) {
        if (userId == null || userId.isBlank()) return;
        float v = clamp(value, 0f, 2f);
        if (Math.abs(v - 1f) < 0.01f) {
            userVolumes.remove(userId);
        } else {
            userVolumes.put(userId, v);
        }
        save();
    }

    public void adjustUserVolume(String userId, float delta) {
        setUserVolume(userId, userVolume(userId) + delta);
    }

    public boolean isUserMuted(String userId) {
        return userId != null && mutedUsers.contains(userId);
    }

    public void setUserMuted(String userId, boolean muted) {
        if (userId == null || userId.isBlank()) return;
        if (muted) mutedUsers.add(userId);
        else mutedUsers.remove(userId);
        save();
    }

    public void toggleUserMuted(String userId) {
        setUserMuted(userId, !isUserMuted(userId));
    }

    public Set<String> mutedUserIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(mutedUsers));
    }

    /** Effective playback gain for a remote user (0 if locally muted). */
    public float playbackGain(String userId) {
        if (isUserMuted(userId)) return 0f;
        return outputVolume * userVolume(userId);
    }

    public void load() {
        Path path = configPath();
        if (path == null || !Files.isRegularFile(path)) return;
        try {
            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            if (obj.has("outputVolume")) {
                outputVolume = clamp(obj.get("outputVolume").getAsFloat(), 0f, 2f);
            }
            if (obj.has("micSensitivity")) {
                micSensitivity = clamp(obj.get("micSensitivity").getAsFloat(), 0.5f, 2f);
            }
            if (obj.has("inputMode")) {
                String mode = obj.get("inputMode").getAsString();
                inputMode = "ptt".equalsIgnoreCase(mode) ? InputMode.PTT : InputMode.VOICE;
            }
            if (obj.has("speakingHudVisible")) {
                speakingHudVisible = obj.get("speakingHudVisible").getAsBoolean();
            }
            userVolumes.clear();
            if (obj.has("userVolumes") && obj.get("userVolumes").isJsonObject()) {
                JsonObject vols = obj.getAsJsonObject("userVolumes");
                for (String key : vols.keySet()) {
                    userVolumes.put(key, clamp(vols.get(key).getAsFloat(), 0f, 2f));
                }
            }
            mutedUsers.clear();
            if (obj.has("mutedUsers") && obj.get("mutedUsers").isJsonArray()) {
                obj.getAsJsonArray("mutedUsers").forEach(el -> {
                    if (el.isJsonPrimitive()) mutedUsers.add(el.getAsString());
                });
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void save() {
        Path path = configPath();
        if (path == null) return;
        try {
            Files.createDirectories(path.getParent());
            JsonObject obj = new JsonObject();
            obj.addProperty("outputVolume", outputVolume);
            obj.addProperty("micSensitivity", micSensitivity);
            obj.addProperty("inputMode", inputMode == InputMode.PTT ? "ptt" : "voice");
            obj.addProperty("speakingHudVisible", speakingHudVisible);
            JsonObject vols = new JsonObject();
            for (Map.Entry<String, Float> e : userVolumes.entrySet()) {
                vols.addProperty(e.getKey(), e.getValue());
            }
            obj.add("userVolumes", vols);
            var muted = new com.google.gson.JsonArray();
            for (String id : mutedUsers) muted.add(id);
            obj.add("mutedUsers", muted);
            Files.writeString(path, GSON.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    private static Path configPath() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameDirectory == null) return null;
        return client.gameDirectory.toPath().resolve("config").resolve("mutualzz_voice.json");
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
