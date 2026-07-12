package com.mutualzz.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


public final class SpeakerPlayback {
    private static final int PREROLL_BYTES = MicCapture.FRAME_BYTES * 4; // ~80ms
    private static final int LINE_BUFFER_BYTES = MicCapture.FRAME_BYTES * 16; // ~320ms
    private static final int WRITE_CHUNK = MicCapture.FRAME_BYTES; // 20ms
    private static final int RING_CAPACITY = MicCapture.SAMPLE_RATE * 2; // 1s of samples

    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private volatile boolean running;

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
        for (Player player : players.values()) {
            player.stop();
        }
        players.clear();
    }

    public void handleDownlink(byte[] packet) {
        if (!running || packet.length < 3) return;

        ByteBuffer buf = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN);
        int idLen = buf.getShort() & 0xFFFF;
        if (idLen <= 0 || idLen > 512 || buf.remaining() < idLen) return;

        byte[] idBytes = new byte[idLen];
        buf.get(idBytes);
        String userId = new String(idBytes, StandardCharsets.UTF_8);

        if (!buf.hasRemaining()) return;
        byte[] pcm = new byte[buf.remaining()];
        buf.get(pcm);

        SpeakingTracker.get().notePcm(userId, pcm);

        if (VoiceSession.isDeafened()) return;
        if (VoiceSettings.get().isUserMuted(userId)) return;

        Player player = players.computeIfAbsent(userId, id -> {
            Player p = new Player(id);
            if (!p.start()) return null;
            return p;
        });
        if (player != null) {
            player.offer(pcm);
        }
    }

    private static final class Player {
        private final String userId;
        private final short[] ring = new short[RING_CAPACITY];
        private final Object lock = new Object();
        private int writePos;
        private int readPos;
        private int available;
        private boolean primed;
        private final AtomicBoolean alive = new AtomicBoolean(false);
        private SourceDataLine line;
        private Thread thread;

        Player(String userId) {
            this.userId = userId;
        }

        boolean start() {
            try {
                AudioFormat format = new AudioFormat(MicCapture.SAMPLE_RATE, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format, LINE_BUFFER_BYTES);
                line.start();
                alive.set(true);
                thread = new Thread(this::loop, "mutualzz-speaker");
                thread.setDaemon(true);
                thread.start();
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        void offer(byte[] pcm) {
            int samples = pcm.length / 2;
            if (samples <= 0) return;
            synchronized (lock) {
                for (int i = 0; i < samples; i++) {
                    int lo = pcm[i * 2] & 0xff;
                    int hi = pcm[i * 2 + 1] & 0xff;
                    short sample = (short) (lo | (hi << 8));
                    if (available == RING_CAPACITY) {
                        // Drop oldest to stay realtime
                        readPos = (readPos + 1) % RING_CAPACITY;
                        available--;
                    }
                    ring[writePos] = sample;
                    writePos = (writePos + 1) % RING_CAPACITY;
                    available++;
                }
                if (!primed && available * 2 >= PREROLL_BYTES) {
                    primed = true;
                }
            }
        }

        void stop() {
            alive.set(false);
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            if (line != null) {
                try {
                    line.stop();
                    line.close();
                } catch (Exception ignored) {
                }
            }
        }

        private void loop() {
            byte[] chunk = new byte[WRITE_CHUNK];
            byte[] silence = new byte[WRITE_CHUNK];
            while (alive.get()) {
                try {
                    boolean ready;
                    synchronized (lock) {
                        ready = primed;
                    }
                    if (!ready) {
                        Thread.sleep(5);
                        continue;
                    }

                    int got = pull(chunk);
                    if (got <= 0) {
                        // Underrun: write silence instead of starving the line (crackles).
                        line.write(silence, 0, silence.length);
                        Thread.sleep(5);
                        continue;
                    }
                    int off = 0;
                    while (off < got && alive.get()) {
                        off += line.write(chunk, off, got - off);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception ignored) {
                }
            }
        }

        /** Fill dest with up to dest.length bytes of PCM; return bytes written. */
        private int pull(byte[] dest) {
            int wantSamples = dest.length / 2;
            // Duck speakers hard while local mic is hot — breaks acoustic howl.
            // Also keep baseline playback quieter so headset bleed is less likely.
            float duck = SpeakingTracker.get().shouldDuckSpeakers() ? 0.06f : 0.55f;
            float userGain = VoiceSettings.get().playbackGain(userId);
            float gain = duck * userGain;
            synchronized (lock) {
                if (available < wantSamples) {
                    return 0;
                }
                for (int i = 0; i < wantSamples; i++) {
                    short s = ring[readPos];
                    readPos = (readPos + 1) % RING_CAPACITY;
                    available--;
                    int scaled = Math.round(s * gain);
                    if (scaled > 32767) scaled = 32767;
                    else if (scaled < -32768) scaled = -32768;
                    dest[i * 2] = (byte) (scaled & 0xff);
                    dest[i * 2 + 1] = (byte) ((scaled >> 8) & 0xff);
                }
                return wantSamples * 2;
            }
        }
    }
}
