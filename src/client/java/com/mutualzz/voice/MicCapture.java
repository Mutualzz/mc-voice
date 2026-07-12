package com.mutualzz.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;


public final class MicCapture {
    public static final int SAMPLE_RATE = 48_000;
    public static final int FRAME_SAMPLES = 960; // 20ms
    public static final int FRAME_BYTES = FRAME_SAMPLES * 2;

    private final Consumer<byte[]> onFrame;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;
    private TargetDataLine line;
    private int actualRate = SAMPLE_RATE;
    private int actualChannels = 1;

    public MicCapture(Consumer<byte[]> onFrame) {
        this.onFrame = onFrame;
    }

    public void start() throws Exception {
        if (running.getAndSet(true)) return;

        line = openMicLine();
        AudioFormat actual = line.getFormat();
        actualRate = Math.max(1, (int) actual.getSampleRate());
        actualChannels = Math.max(1, actual.getChannels());
        line.start();

        thread = new Thread(this::loop, "mutualzz-mic");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running.set(false);
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {
            }
            line = null;
        }
        if (thread != null) {
            try {
                thread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
    }

    private static TargetDataLine openMicLine() throws LineUnavailableException {
        AudioFormat[] candidates = {
                new AudioFormat(48_000, 16, 1, true, false),
                new AudioFormat(48_000, 16, 2, true, false),
                new AudioFormat(44_100, 16, 1, true, false),
                new AudioFormat(44_100, 16, 2, true, false),
                new AudioFormat(16_000, 16, 1, true, false),
        };

        // Prefer the system default capture mixer, then try others.
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        Mixer.Info[] order = new Mixer.Info[mixers.length + 1];
        order[0] = null; // AudioSystem default
        System.arraycopy(mixers, 0, order, 1, mixers.length);

        LineUnavailableException last = null;
        for (Mixer.Info mixerInfo : order) {
            for (AudioFormat format : candidates) {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                try {
                    TargetDataLine target;
                    if (mixerInfo == null) {
                        if (!AudioSystem.isLineSupported(info)) continue;
                        target = (TargetDataLine) AudioSystem.getLine(info);
                    } else {
                        Mixer mixer = AudioSystem.getMixer(mixerInfo);
                        if (!mixer.isLineSupported(info)) continue;
                        target = (TargetDataLine) mixer.getLine(info);
                    }
                    int buffer = (int) (format.getSampleRate() * format.getFrameSize() * 0.1);
                    target.open(format, Math.max(buffer, format.getFrameSize() * 960));
                    return target;
                } catch (LineUnavailableException | IllegalArgumentException e) {
                    if (e instanceof LineUnavailableException lue) last = lue;
                }
            }
        }
        if (last != null) throw last;
        throw new LineUnavailableException("No usable microphone line found");
    }

    private void loop() {
        // Read ~20ms of native audio, normalize, then emit fixed 20ms @ 48k mono chunks.
        int nativeFrames = Math.max(1, (int) Math.round(FRAME_SAMPLES * (actualRate / (double) SAMPLE_RATE)));
        int frameBytes = nativeFrames * actualChannels * 2;
        byte[] buf = new byte[frameBytes];
        byte[] pending = new byte[0];

        while (running.get()) {
            TargetDataLine l = line;
            if (l == null) break;
            int read = l.read(buf, 0, buf.length);
            if (read <= 0) continue;

            byte[] mono48 = toMono48k(buf, read, actualChannels, actualRate);
            if (mono48.length == 0) continue;

            // Accumulate to exact FRAME_BYTES so the hub gets steady frames.
            byte[] merged = new byte[pending.length + mono48.length];
            System.arraycopy(pending, 0, merged, 0, pending.length);
            System.arraycopy(mono48, 0, merged, pending.length, mono48.length);

            int offset = 0;
            while (offset + FRAME_BYTES <= merged.length) {
                byte[] frame = new byte[FRAME_BYTES];
                System.arraycopy(merged, offset, frame, 0, FRAME_BYTES);
                onFrame.accept(frame);
                offset += FRAME_BYTES;
            }
            pending = new byte[merged.length - offset];
            if (pending.length > 0) {
                System.arraycopy(merged, offset, pending, 0, pending.length);
            }
        }
    }

    /** Normalize to mono s16le @ 48kHz for the hub. */
    private static byte[] toMono48k(byte[] src, int len, int channels, int rate) {
        int frames = len / (2 * Math.max(1, channels));
        if (frames <= 0) return new byte[0];

        short[] mono = new short[frames];
        for (int i = 0; i < frames; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                int idx = (i * channels + c) * 2;
                if (idx + 1 >= len) break;
                sum += (short) ((src[idx] & 0xff) | ((src[idx + 1] & 0xff) << 8));
            }
            mono[i] = (short) (sum / channels);
        }

        short[] at48 = mono;
        if (rate != SAMPLE_RATE) {
            int outLen = Math.max(1, (int) Math.round(frames * (SAMPLE_RATE / (double) rate)));
            at48 = new short[outLen];
            double ratio = rate / (double) SAMPLE_RATE;
            for (int i = 0; i < outLen; i++) {
                double srcPos = i * ratio;
                int i0 = Math.min((int) srcPos, frames - 1);
                int i1 = Math.min(i0 + 1, frames - 1);
                double t = srcPos - i0;
                at48[i] = (short) (mono[i0] * (1 - t) + mono[i1] * t);
            }
        }

        byte[] out = new byte[at48.length * 2];
        for (int i = 0; i < at48.length; i++) {
            out[i * 2] = (byte) (at48[i] & 0xff);
            out[i * 2 + 1] = (byte) ((at48[i] >> 8) & 0xff);
        }
        return out;
    }
}
