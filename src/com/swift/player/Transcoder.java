package com.swift.player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Fallback converter for media the JavaFX engine cannot open natively
 * (e.g. MKV, AVI, WebM, FLAC, OGG). Uses ffmpeg from PATH and caches the
 * converted copy under ~/.swift/transcode so each file is converted once.
 */
public final class Transcoder {

    private static final Path CACHE_DIR =
            Paths.get(System.getProperty("user.home"), ".swift", "transcode");

    private static volatile Boolean available;

    private Transcoder() {
    }

    /** Whether an ffmpeg executable is available on PATH (checked once). */
    public static boolean isAvailable() {
        Boolean cached = available;
        if (cached == null) {
            synchronized (Transcoder.class) {
                if (available == null) available = probe();
                cached = available;
            }
        }
        return cached;
    }

    private static boolean probe() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return p.waitFor(10, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Converts {@code source} to a JavaFX-friendly file on a background thread.
     * Exactly one of the callbacks is invoked, on the worker thread.
     */
    public static void transcodeAsync(File source, boolean audioOnly,
                                      Consumer<File> onDone, Consumer<String> onError) {
        Thread worker = new Thread(() -> {
            try {
                Path target = targetFor(source, audioOnly);
                if (!isUsable(target)) {
                    Files.createDirectories(CACHE_DIR);
                    convert(source, target, audioOnly);
                    if (!isUsable(target)) {
                        Files.deleteIfExists(target);
                        onError.accept("Conversion produced no playable output");
                        return;
                    }
                }
                onDone.accept(target.toFile());
            } catch (IOException | InterruptedException e) {
                onError.accept(String.valueOf(e.getMessage()));
            }
        });
        worker.setDaemon(true);
        worker.start();
    }

    private static void convert(File source, Path target, boolean audioOnly)
            throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of(
                "ffmpeg", "-y", "-v", "error", "-i", source.getAbsolutePath()));
        if (audioOnly) {
            cmd.addAll(List.of("-map", "0:a:0", "-c:a", "aac", "-b:a", "192k"));
        } else {
            cmd.addAll(List.of("-map", "0:v:0", "-map", "0:a:0?",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "23", "-pix_fmt", "yuv420p",
                    "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart"));
        }
        cmd.addAll(List.of("-map_metadata", "0", target.toString()));
        int exit = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .start()
                .waitFor();
        if (exit != 0) {
            throw new IOException("ffmpeg exited with code " + exit);
        }
    }

    private static boolean isUsable(Path target) throws IOException {
        return Files.isRegularFile(target) && Files.size(target) > 0;
    }

    /** Stable cache location keyed on the source path, size and modification time. */
    private static Path targetFor(File source, boolean audioOnly) {
        String key = source.getAbsolutePath() + "|" + source.lastModified() + "|" + source.length();
        return CACHE_DIR.resolve(hash(key) + (audioOnly ? ".m4a" : ".mp4"));
    }

    private static String hash(String key) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes());
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(key.hashCode());
        }
    }
}
