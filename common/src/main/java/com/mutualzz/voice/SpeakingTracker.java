package com.mutualzz.voice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public final class SpeakingTracker {
    private static final double RMS_THRESHOLD = 700.0;
    /** Lower bar for speaker ducking so feedback is cut earlier than HUD "speaking". */
    private static final double DUCK_RMS_THRESHOLD = 280.0;
    private static final long HOLD_MS = 450;
    private static final long DUCK_HOLD_MS = 350;

    private static final SpeakingTracker INSTANCE = new SpeakingTracker();

    private final Map<String, String> names = new ConcurrentHashMap<>();
    /** Insertion-ordered roster for cycling selection. */
    private final Map<String, String> roster = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, Long> lastSpeakAt = new ConcurrentHashMap<>();
    private volatile long lastSelfDuckAt = 0;
    private volatile String selfId = "";

    private SpeakingTracker() {
    }

    public static SpeakingTracker get() {
        return INSTANCE;
    }

    public void clear() {
        names.clear();
        roster.clear();
        lastSpeakAt.clear();
        lastSelfDuckAt = 0;
        selfId = "";
    }

    public void setSelfId(String id) {
        if (id != null && !id.isBlank()) {
            selfId = id;
            names.putIfAbsent(id, "You");
            roster.putIfAbsent(id, "You");
        }
    }

    public String selfId() {
        return selfId;
    }

    public void putName(String id, String name) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) return;
        String display = id.equals(selfId) ? "You" : name;
        names.put(id, display);
        roster.put(id, display);
    }

    public void replaceRoster(List<String> ids, Map<String, String> nameById) {
        roster.clear();
        if (ids == null) return;
        for (String id : ids) {
            if (id == null || id.isBlank()) continue;
            String name = nameById != null ? nameById.get(id) : null;
            if (name == null || name.isBlank()) name = names.getOrDefault(id, shortId(id));
            if (id.equals(selfId)) name = "You";
            names.put(id, name);
            roster.put(id, name);
        }
    }

    public String displayName(String id) {
        if (id == null || id.isBlank()) return "?";
        String name = names.get(id);
        if (name != null) return name;
        return id.equals(selfId) ? "You" : shortId(id);
    }

    /** Roster user ids in stable order (includes self). */
    public List<String> rosterIds() {
        synchronized (roster) {
            return new ArrayList<>(roster.keySet());
        }
    }

    /** Other members only — for local mute/volume targeting. */
    public List<String> otherRosterIds() {
        List<String> out = new ArrayList<>();
        for (String id : rosterIds()) {
            if (!id.equals(selfId)) out.add(id);
        }
        return out;
    }

    public void notePcm(String userId, byte[] pcm) {
        if (userId == null || userId.isBlank() || pcm == null || pcm.length < 4) return;
        double r = rms(pcm);
        if (r >= RMS_THRESHOLD) {
            lastSpeakAt.put(userId, System.currentTimeMillis());
        }
        if (userId.equals(selfId) && r >= DUCK_RMS_THRESHOLD) {
            lastSelfDuckAt = System.currentTimeMillis();
        }
    }

    /** True if local mic has been above VAD threshold recently. */
    public boolean isSelfSpeaking() {
        if (selfId.isEmpty()) return false;
        Long at = lastSpeakAt.get(selfId);
        return at != null && System.currentTimeMillis() - at <= HOLD_MS;
    }

    /** Earlier/more sensitive than speaking — used to duck speakers against howl. */
    public boolean shouldDuckSpeakers() {
        if (selfId.isEmpty()) return false;
        return System.currentTimeMillis() - lastSelfDuckAt <= DUCK_HOLD_MS;
    }

    /** Display names of people currently speaking (held briefly after last loud frame). */
    public List<String> speakingNames() {
        long now = System.currentTimeMillis();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : lastSpeakAt.entrySet()) {
            if (now - e.getValue() > HOLD_MS) continue;
            out.add(displayName(e.getKey()));
        }
        Collections.sort(out);
        return out;
    }

    private static String shortId(String id) {
        return id.length() <= 6 ? id : id.substring(id.length() - 6);
    }

    private static double rms(byte[] pcm) {
        int samples = pcm.length / 2;
        if (samples == 0) return 0;
        double sum = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (pcm[i] & 0xFF) | ((pcm[i + 1] & 0xFF) << 8);
            if (sample >= 0x8000) sample -= 0x10000;
            sum += (double) sample * sample;
        }
        return Math.sqrt(sum / samples);
    }
}
