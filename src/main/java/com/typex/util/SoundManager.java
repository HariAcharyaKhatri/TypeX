package com.typex.util;

import javax.sound.sampled.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

/**
 * Synthesizes short tones - no external audio files required.
 * Respects a global enabled flag + volume stored in Preferences.
 * Uses a single daemon background thread with a bounded non-blocking queue
 * to eliminate thread creation overhead and prevent audio lag during rapid typing.
 */
public final class SoundManager {
    private static final Preferences prefs = Preferences.userNodeForPackage(SoundManager.class);
    private static volatile boolean enabled = prefs.getBoolean("sound.enabled", true);
    private static volatile int volume = prefs.getInt("sound.volume", 70);

    private static final ThreadPoolExecutor AUDIO_EXECUTOR = new ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(16),
            r -> {
                Thread t = new Thread(r, "TypeX-Audio");
                t.setDaemon(true);
                return t;
            },
            new ThreadPoolExecutor.DiscardOldestPolicy()
    );

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                AUDIO_EXECUTOR.shutdownNow();
            } catch (Exception ignored) {}
        }));
    }

    private SoundManager() {}

    public static void setEnabled(boolean e) {
        enabled = e;
        prefs.putBoolean("sound.enabled", e);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean toggleEnabled() {
        boolean next = !enabled;
        setEnabled(next);
        return next;
    }

    public static void setVolume(int v) {
        volume = Math.max(0, Math.min(100, v));
        prefs.putInt("sound.volume", volume);
    }

    public static int getVolume() {
        return volume;
    }

    public static void keypress() {
        playTone(880, 20, volume / 100.0 * 0.25);
    }

    public static void error() {
        playTone(180, 90, volume / 100.0 * 0.35);
    }

    public static void complete() {
        playTone(660, 60, volume / 100.0 * 0.4);
        playTone(990, 120, volume / 100.0 * 0.4, 40);
    }

    public static void achievement() {
        playTone(784, 80, volume / 100.0 * 0.4);
        playTone(1046, 160, volume / 100.0 * 0.4, 50);
    }

    public static void levelUp() {
        playTone(523, 70, volume / 100.0 * 0.4);
        playTone(659, 70, volume / 100.0 * 0.4, 60);
        playTone(784, 80, volume / 100.0 * 0.4, 120);
        playTone(1046, 200, volume / 100.0 * 0.4, 180);
    }

    private static void playTone(int hz, int ms, double gain) {
        playTone(hz, ms, gain, 0);
    }

    private static void playTone(int hz, int ms, double gain, int delayMs) {
        if (!enabled || volume <= 0) return;
        try {
            AUDIO_EXECUTOR.execute(() -> {
                try {
                    if (delayMs > 0) Thread.sleep(delayMs);
                    float sampleRate = 44100f;
                    int samples = (int) (sampleRate * ms / 1000.0);
                    byte[] buf = new byte[samples * 2];
                    for (int i = 0; i < samples; i++) {
                        double angle = i / (sampleRate / hz) * 2.0 * Math.PI;
                        short val = (short) (Math.sin(angle) * Short.MAX_VALUE * gain);
                        buf[i * 2] = (byte) (val & 0xff);
                        buf[i * 2 + 1] = (byte) ((val >> 8) & 0xff);
                    }
                    AudioFormat af = new AudioFormat(sampleRate, 16, 1, true, false);
                    try (SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
                        line.open(af);
                        line.start();
                        line.write(buf, 0, buf.length);
                        line.drain();
                        line.stop();
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }
}
