package com.swift.player;

import com.swift.model.MediaItem;
import javafx.animation.PauseTransition;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Queue;
import java.util.Set;

/**
 * Reads embedded tags (title, artist, cover art) from local media files by
 * probing them one at a time with a throwaway MediaPlayer, so the UI can show
 * real metadata instead of bare file names.
 */
public final class MetadataReader {

    private static final Duration PROBE_TIMEOUT = Duration.seconds(6);

    private final Queue<MediaItem> pending = new ArrayDeque<>();
    private final Set<MediaItem> queued = Collections.newSetFromMap(new IdentityHashMap<>());
    private MediaPlayer probe;
    private PauseTransition timeout;
    private MediaItem current;
    private boolean active;

    public void enqueue(MediaItem item) {
        if (item == null || item.isStream() || item.isMetadataResolved() || !queued.add(item)) {
            return;
        }
        pending.add(item);
        pump();
    }

    public void enqueueAll(Collection<? extends MediaItem> items) {
        for (MediaItem item : items) {
            if (item != null && !item.isStream() && !item.isMetadataResolved() && queued.add(item)) {
                pending.add(item);
            }
        }
        pump();
    }

    private void pump() {
        if (active) return;
        MediaItem item = pending.poll();
        if (item == null) return;
        active = true;
        current = item;

        final Media media;
        try {
            media = new Media(item.toUri());
            probe = new MediaPlayer(media);
        } catch (RuntimeException e) {
            probe = null;
            finish(item);
            return;
        }
        probe.setMute(true);
        probe.setOnReady(() -> {
            item.applyMetadata(media.getMetadata());
            finish(item);
        });
        probe.setOnError(() -> finish(item));
        media.setOnError(() -> finish(item));

        timeout = new PauseTransition(PROBE_TIMEOUT);
        timeout.setOnFinished(e -> finish(item));
        timeout.play();
    }

    private void finish(MediaItem item) {
        if (item != current) return;
        current = null;
        item.setMetadataResolved(true);
        queued.remove(item);
        if (timeout != null) {
            timeout.stop();
            timeout = null;
        }
        if (probe != null) {
            MediaPlayer p = probe;
            probe = null;
            p.setOnReady(null);
            p.setOnError(null);
            p.dispose();
        }
        active = false;
        pump();
    }
}
