package com.mutualzz.voice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class VoiceSession {
    public enum ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("mutualzz_voice");

    private static final AtomicBoolean muted = new AtomicBoolean(false);
    private static final AtomicBoolean deafened = new AtomicBoolean(false);
    private static final AtomicBoolean pttHeld = new AtomicBoolean(false);

    private static WebSocket socket;
    private static MicCapture mic;
    private static SpeakerPlayback speakers;
    private static Thread uplinkThread;
    private static Thread reconnectThread;
    private static final LinkedBlockingQueue<byte[]> uplinkQueue = new LinkedBlockingQueue<>(128);
    /** Control JSON (mute/deafen) — drained by uplink thread to avoid concurrent WS sends. */
    private static volatile String pendingControlJson;
    private static volatile boolean connected;
    private static volatile ConnectionState state = ConnectionState.IDLE;
    /** True after join until intentional leave — drives reconnect. */
    private static volatile boolean sessionWanted;
    private static volatile String lastAudioWsUrl;
    private static volatile String lastAudioToken;
    private static volatile String pendingRedirectUrl;
    private static volatile String channelLabel = "";
    private static volatile String roomName = "";
    private static final AtomicInteger reconnectAttempt = new AtomicInteger(0);
    /** Suppress reconnect while leave()/join() tears down the socket. */
    private static volatile boolean tearingDown;

    private VoiceSession() {
    }

    public static boolean isConnected() {
        return connected;
    }

    public static ConnectionState connectionState() {
        return state;
    }

    public static String channelLabel() {
        return channelLabel;
    }

    public static String roomName() {
        return roomName;
    }

    public static void setMuted(boolean value) {
        muted.set(value);
        syncVoiceStateToServer();
    }

    public static boolean isMuted() {
        return muted.get();
    }

    public static void setDeafened(boolean value) {
        deafened.set(value);
        // Discord-style: deafen forces mute; undeafen clears both.
        muted.set(value);
        syncVoiceStateToServer();
    }

    public static boolean isDeafened() {
        return deafened.get();
    }

    public static void setPttHeld(boolean held) {
        pttHeld.set(held);
    }

    public static boolean isPttHeld() {
        return pttHeld.get();
    }

    /** Mic is silenced when muted, deafened, or PTT mode without hold. */
    public static boolean isMicSilenced() {
        if (muted.get() || deafened.get()) return true;
        if (VoiceSettings.get().isPushToTalk() && !pttHeld.get()) return true;
        return false;
    }

    private static void syncVoiceStateToServer() {
        if (!connected) return;
        boolean selfMute = muted.get() || deafened.get();
        boolean selfDeaf = deafened.get();
        // Primary: audio WS → hub (reliable, no Paper hop).
        pendingControlJson = "{\"t\":\"voice_state\",\"selfMute\":" + selfMute
                + ",\"selfDeaf\":" + selfDeaf + "}";
        // Backup: plugin channel → Paper → hub.
        try {
            VoiceNetworking.sendVoiceState(selfMute, selfDeaf);
        } catch (Exception ignored) {
        }
    }

    public static void handlePluginMessage(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String type = obj.has("t") ? obj.get("t").getAsString() : "";
            switch (type) {
                case "voice_join" -> {
                    String url = obj.get("audioWsUrl").getAsString();
                    String token = obj.get("audioToken").getAsString();
                    roomName = obj.has("room") ? obj.get("room").getAsString() : "default";
                    String channelName = obj.has("channelName") ? obj.get("channelName").getAsString() : "";
                    if (!channelName.isBlank()) {
                        channelLabel = "#" + channelName;
                    } else if (!roomName.isBlank()) {
                        channelLabel = roomName;
                    } else {
                        channelLabel = "";
                    }
                    join(url, token, false);
                }
                case "voice_leave" -> leave(true);
                default -> {
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Voice plugin message parse failed: {}", e.toString());
        }
    }

    public static synchronized void join(String audioWsUrl, String audioToken) {
        join(audioWsUrl, audioToken, false);
    }

    private static synchronized void join(String audioWsUrl, String audioToken, boolean isReconnect) {
        cancelReconnectThread();
        leave(false);

        lastAudioWsUrl = audioWsUrl;
        lastAudioToken = audioToken;
        sessionWanted = true;
        state = isReconnect ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING;

        URI uri = null;
        try {
            String raw = audioWsUrl.replace("://localhost", "://127.0.0.1");
            uri = URI.create(raw);

            speakers = new SpeakerPlayback();
            speakers.start();

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            socket = client.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + audioToken)
                    .buildAsync(uri, new Listener())
                    .join();

            connected = true;
            state = ConnectionState.CONNECTED;
            reconnectAttempt.set(0);
            pendingControlJson = null;
            pendingRedirectUrl = null;
            startUplinkSender();
            syncVoiceStateToServer();

            mic = new MicCapture(pcm -> {
                if (!connected) return;
                byte[] framed = applyMicSensitivity(RnnoiseDenoiser.get().processFrame(pcm));
                String selfId = SpeakingTracker.get().selfId();
                if (!isMicSilenced() && !selfId.isEmpty()) {
                    SpeakingTracker.get().notePcm(selfId, framed);
                }
                if (isMicSilenced()) return;
                if (!uplinkQueue.offer(framed)) {
                    uplinkQueue.poll();
                    uplinkQueue.offer(framed);
                }
            });
            mic.start();
            tellPlayer(isReconnect
                    ? "Reconnected to Mutualzz voice"
                    : "Connected to Mutualzz voice");
        } catch (Exception e) {
            connected = false;
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String where = uri != null ? uri.getHost() + ":" + uri.getPort() : audioWsUrl;
            String detail = String.valueOf(root);
            LOGGER.warn("Voice connect failed at {}: {}", where, root.toString());
            if (isPermanentDisconnect(-1, detail) || isPermanentDisconnect(-1, root.getMessage())) {
                sessionWanted = false;
                cancelReconnectThread();
                leave(true);
                tellPlayer("Voice connect rejected");
                return;
            }
            if (sessionWanted) {
                state = ConnectionState.RECONNECTING;
                tellPlayer("Couldn't connect — retrying…");
                scheduleReconnect();
            } else {
                leave(true);
                tellPlayer("Couldn't connect to Mutualzz voice");
            }
        }
    }

    /** Intentional leave from game / plugin. */
    public static synchronized void leave() {
        leave(true);
    }

    private static synchronized void leave(boolean clearWanted) {
        if (clearWanted) {
            sessionWanted = false;
            lastAudioWsUrl = null;
            lastAudioToken = null;
            pendingRedirectUrl = null;
            channelLabel = "";
            roomName = "";
            cancelReconnectThread();
        }
        connected = false;
        if (clearWanted) {
            state = ConnectionState.IDLE;
            muted.set(false);
            deafened.set(false);
            pttHeld.set(false);
        }
        pendingControlJson = null;
        uplinkQueue.clear();
        SpeakingTracker.get().clear();
        if (uplinkThread != null) {
            uplinkThread.interrupt();
            try {
                uplinkThread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            uplinkThread = null;
        }
        if (mic != null) {
            mic.stop();
            mic = null;
        }
        RnnoiseDenoiser.get().close();
        if (speakers != null) {
            speakers.stop();
            speakers = null;
        }
        tearingDown = true;
        try {
            if (socket != null) {
                try {
                    socket.sendClose(WebSocket.NORMAL_CLOSURE, "leave").join();
                } catch (Exception ignored) {
                }
                socket = null;
            }
        } finally {
            tearingDown = false;
        }
    }

    private static void onSocketLost(String reason) {
        connected = false;
        if (!sessionWanted) {
            state = ConnectionState.IDLE;
            return;
        }
        if (isPermanentDisconnect(-1, reason)) {
            endSessionPermanently(reason);
            return;
        }
        state = ConnectionState.RECONNECTING;
        LOGGER.debug("Voice audio lost: {}", reason == null || reason.isBlank() ? "(no reason)" : reason);
        tellPlayer("Voice connection lost — reconnecting…");
        scheduleReconnect();
    }

    /**
     * Hub closed the session on purpose (kick / leave / revoked token). Do not reconnect.
     */
    private static boolean isPermanentDisconnect(int statusCode, String reason) {
        if (statusCode == 4001 || statusCode == 4002) {
            return true;
        }
        if (statusCode == 4003) {
            return pendingRedirectUrl == null;
        }
        if (reason == null || reason.isBlank()) return false;
        String r = reason.trim().toLowerCase(Locale.ROOT);
        if (r.equals("wrong_instance") && pendingRedirectUrl != null) {
            return false;
        }
        return r.equals("leave")
                || r.equals("kicked")
                || r.equals("invalid_token")
                || r.equals("not_in_voice")
                || r.equals("session_ended")
                || r.contains("invalid token")
                || r.contains("invalid_token");
    }

    private static void endSessionPermanently(String reason) {
        String msg;
        String r = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        if (r.contains("invalid")) {
            msg = "Voice session ended (token invalid) — not reconnecting";
        } else if (r.contains("kick")) {
            msg = "Removed from Mutualzz voice";
        } else {
            msg = "Disconnected from Mutualzz voice";
        }
        // Clear wanted before leave so close callbacks cannot schedule reconnect.
        sessionWanted = false;
        cancelReconnectThread();
        lastAudioWsUrl = null;
        lastAudioToken = null;
        Minecraft.getInstance().execute(() -> leave(true));
        tellPlayer(msg);
    }

    private static synchronized void scheduleReconnect() {
        if (!sessionWanted) return;
        if (reconnectThread != null && reconnectThread.isAlive()) return;
        reconnectThread = new Thread(() -> {
            while (sessionWanted && !connected && !Thread.currentThread().isInterrupted()) {
                int attempt = reconnectAttempt.incrementAndGet();
                if (attempt > 8) {
                    tellPlayer("Voice reconnect gave up — use /mzvoice join again");
                    sessionWanted = false;
                    state = ConnectionState.IDLE;
                    lastAudioWsUrl = null;
                    lastAudioToken = null;
                    break;
                }
                long delayMs = Math.min(15_000L, 1_000L * (1L << Math.min(attempt - 1, 3)));
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!sessionWanted || connected) break;
                String url = lastAudioWsUrl;
                String token = lastAudioToken;
                if (url == null || token == null) break;
                Minecraft.getInstance().execute(() -> join(url, token, true));
                // join runs on client thread via execute — wait a beat before next attempt
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "mutualzz-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    private static void cancelReconnectThread() {
        if (reconnectThread != null) {
            reconnectThread.interrupt();
            reconnectThread = null;
        }
        reconnectAttempt.set(0);
    }

    private static byte[] applyMicSensitivity(byte[] pcm) {
        float sens = VoiceSettings.get().micSensitivity();
        if (Math.abs(sens - 1f) < 0.02f) return pcm;
        byte[] out = new byte[pcm.length];
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int sample = (pcm[i] & 0xff) | ((pcm[i + 1] & 0xff) << 8);
            if (sample >= 0x8000) sample -= 0x10000;
            int scaled = Math.round(sample * sens);
            if (scaled > 32767) scaled = 32767;
            else if (scaled < -32768) scaled = -32768;
            out[i] = (byte) (scaled & 0xff);
            out[i + 1] = (byte) ((scaled >> 8) & 0xff);
        }
        return out;
    }

    /**
     * Java HttpClient WebSocket forbids concurrent send* calls — serialize uplink + control.
     */
    private static void startUplinkSender() {
        uplinkThread = new Thread(() -> {
            while (connected && !Thread.currentThread().isInterrupted()) {
                try {
                    String control = pendingControlJson;
                    if (control != null) {
                        pendingControlJson = null;
                        WebSocket wsCtrl = socket;
                        if (wsCtrl != null && connected) {
                            wsCtrl.sendText(control, true).join();
                        }
                    }

                    byte[] pcm = uplinkQueue.poll(20, TimeUnit.MILLISECONDS);
                    if (pcm == null) continue;
                    WebSocket ws = socket;
                    if (ws == null || !connected) continue;
                    ws.sendBinary(ByteBuffer.wrap(pcm), true).join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.debug("Mic uplink send failed: {}", e.toString());
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "mutualzz-uplink");
        uplinkThread.setDaemon(true);
        uplinkThread.start();
    }

    private static void handleControlMessage(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String type = obj.has("t") ? obj.get("t").getAsString() : "";
            SpeakingTracker tracker = SpeakingTracker.get();
            switch (type) {
                case "ready" -> {
                    if (obj.has("userId")) {
                        tracker.setSelfId(obj.get("userId").getAsString());
                    }
                }
                case "redirect" -> {
                    if (obj.has("audioWsUrl")) {
                        String redirectUrl = obj.get("audioWsUrl").getAsString();
                        pendingRedirectUrl = redirectUrl;
                        lastAudioWsUrl = redirectUrl;
                    }
                }
                case "roster" -> {
                    if (obj.has("selfId")) {
                        tracker.setSelfId(obj.get("selfId").getAsString());
                    }
                    List<String> ids = new ArrayList<>();
                    Map<String, String> nameById = new HashMap<>();
                    if (obj.has("members") && obj.get("members").isJsonArray()) {
                        JsonArray members = obj.getAsJsonArray("members");
                        for (JsonElement el : members) {
                            if (!el.isJsonObject()) continue;
                            JsonObject m = el.getAsJsonObject();
                            if (!m.has("id") || !m.has("name")) continue;
                            String id = m.get("id").getAsString();
                            String name = m.get("name").getAsString();
                            ids.add(id);
                            nameById.put(id, name);
                            tracker.putName(id, name);
                        }
                    }
                    tracker.replaceRoster(ids, nameById);
                }
                case "member" -> {
                    if (obj.has("id") && obj.has("name")) {
                        tracker.putName(obj.get("id").getAsString(), obj.get("name").getAsString());
                    }
                }
                default -> {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void tellPlayer(String message) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.literal("[Mutualzz] " + message)
                );
            }
        });
    }

    private static final class Listener implements WebSocket.Listener {
        private final StringBuilder text = new StringBuilder();
        private final java.io.ByteArrayOutputStream binary = new java.io.ByteArrayOutputStream();

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            text.append(data);
            if (last) {
                String json = text.toString();
                text.setLength(0);
                handleControlMessage(json);
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            // HttpClient may deliver one WS message as multiple partial buffers.
            byte[] chunk = new byte[data.remaining()];
            data.get(chunk);
            try {
                binary.write(chunk);
            } catch (Exception ignored) {
            }
            if (last) {
                byte[] packet = binary.toByteArray();
                binary.reset();
                SpeakerPlayback sp = speakers;
                if (sp != null) {
                    sp.handleDownlink(packet);
                }
            }
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            // Ignore close from a socket we've already replaced / torn down.
            if (tearingDown || webSocket != socket) {
                if (webSocket == socket) connected = false;
                return CompletableFuture.completedFuture(null);
            }
            connected = false;
            socket = null;
            if (!sessionWanted) {
                state = ConnectionState.IDLE;
                return CompletableFuture.completedFuture(null);
            }
            if (statusCode == 4003 && pendingRedirectUrl != null) {
                String url = pendingRedirectUrl;
                String token = lastAudioToken;
                pendingRedirectUrl = null;
                lastAudioWsUrl = url;
                if (token != null) {
                    Minecraft.getInstance().execute(() -> join(url, token, true));
                }
                return CompletableFuture.completedFuture(null);
            }
            if (isPermanentDisconnect(statusCode, reason)) {
                endSessionPermanently(reason != null && !reason.isBlank()
                        ? reason
                        : ("code " + statusCode));
                return CompletableFuture.completedFuture(null);
            }
            onSocketLost(reason);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (tearingDown || webSocket != socket) {
                if (webSocket == socket) connected = false;
                return;
            }
            connected = false;
            if (!sessionWanted) {
                state = ConnectionState.IDLE;
                LOGGER.debug("Voice audio error (idle): {}", error != null ? error.toString() : "null");
                return;
            }
            LOGGER.debug("Voice audio error: {}", error != null ? error.toString() : "null");
            String detail = error != null ? error.getMessage() : null;
            if (isPermanentDisconnect(-1, detail)) {
                endSessionPermanently(detail);
                return;
            }
            onSocketLost(detail);
        }
    }
}
