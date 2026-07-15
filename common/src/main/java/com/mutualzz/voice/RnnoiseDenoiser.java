package com.mutualzz.voice;

import de.maxhenkel.rnnoise4j.Denoiser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side RNNoise (same library Simple Voice Chat uses).
 * Mutualzz uplink frames are 20 ms (960 samples); RNNoise wants 10 ms (480).
 */
public final class RnnoiseDenoiser implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger("mutualzz_voice");
    /** RNNoise frame length at 48 kHz. */
    private static final int RNNOISE_SAMPLES = 480;

    private static final RnnoiseDenoiser INSTANCE = new RnnoiseDenoiser();

    private Denoiser denoiser;
    private boolean initAttempted;
    private boolean available;
    private boolean loggedFailure;

    private RnnoiseDenoiser() {
    }

    public static RnnoiseDenoiser get() {
        return INSTANCE;
    }

    public boolean isAvailable() {
        ensureInit();
        return available;
    }

    /**
     * Denoise one Mutualzz uplink frame in place when possible.
     * Expects mono s16le, {@link MicCapture#FRAME_SAMPLES} samples (960).
     */
    public byte[] processFrame(byte[] pcm) {
        if (pcm == null || pcm.length < MicCapture.FRAME_BYTES) return pcm;
        if (!VoiceSettings.get().noiseSuppression()) return pcm;
        ensureInit();
        if (!available || denoiser == null) return pcm;

        short[] samples = bytesToShorts(pcm);
        try {
            // Two sequential 10 ms RNNoise frames.
            short[] first = new short[RNNOISE_SAMPLES];
            short[] second = new short[RNNOISE_SAMPLES];
            System.arraycopy(samples, 0, first, 0, RNNOISE_SAMPLES);
            System.arraycopy(samples, RNNOISE_SAMPLES, second, 0, RNNOISE_SAMPLES);
            denoiser.denoiseInPlace(first);
            denoiser.denoiseInPlace(second);
            System.arraycopy(first, 0, samples, 0, RNNOISE_SAMPLES);
            System.arraycopy(second, 0, samples, RNNOISE_SAMPLES, RNNOISE_SAMPLES);
            return shortsToBytes(samples);
        } catch (Throwable t) {
            if (!loggedFailure) {
                loggedFailure = true;
                LOGGER.warn("RNNoise failed while processing; disabling noise suppression", t);
            }
            available = false;
            closeQuietly();
            return pcm;
        }
    }

    public synchronized void close() {
        closeQuietly();
        available = false;
        initAttempted = false;
        loggedFailure = false;
    }

    private synchronized void ensureInit() {
        if (initAttempted) return;
        initAttempted = true;
        try {
            denoiser = new Denoiser();
            available = true;
            LOGGER.debug("RNNoise noise suppression ready");
        } catch (Throwable t) {
            available = false;
            denoiser = null;
            if (!loggedFailure) {
                loggedFailure = true;
                LOGGER.warn(
                        "RNNoise natives unavailable; noise suppression disabled ({})",
                        t.toString()
                );
            }
        }
    }

    private void closeQuietly() {
        if (denoiser != null) {
            try {
                denoiser.close();
            } catch (Throwable ignored) {
            }
            denoiser = null;
        }
    }

    private static short[] bytesToShorts(byte[] pcm) {
        short[] out = new short[MicCapture.FRAME_SAMPLES];
        for (int i = 0, s = 0; s < out.length && i + 1 < pcm.length; s++, i += 2) {
            int sample = (pcm[i] & 0xff) | ((pcm[i + 1] & 0xff) << 8);
            if (sample >= 0x8000) sample -= 0x10000;
            out[s] = (short) sample;
        }
        return out;
    }

    private static byte[] shortsToBytes(short[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int s = 0, i = 0; s < samples.length; s++, i += 2) {
            int v = samples[s];
            out[i] = (byte) (v & 0xff);
            out[i + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }
}
