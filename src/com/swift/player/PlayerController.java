package com.swift.player;

import com.swift.model.MediaItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.MapChangeListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.util.function.Consumer;

public class PlayerController {

    private MediaPlayer mediaPlayer;

    private final SimpleBooleanProperty playing = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty muted = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty volume = new SimpleDoubleProperty(0.8);
    private final SimpleDoubleProperty rate = new SimpleDoubleProperty(1.0);
    private final SimpleObjectProperty<Duration> currentTime = new SimpleObjectProperty<>(Duration.ZERO);
    private final SimpleObjectProperty<Duration> totalDuration = new SimpleObjectProperty<>(Duration.UNKNOWN);
    private final SimpleObjectProperty<MediaPlayer.Status> status =
            new SimpleObjectProperty<>(MediaPlayer.Status.UNKNOWN);

    private Runnable onEndOfMedia;
    private Consumer<String> onError;

    public void setOnEndOfMedia(Runnable handler) {
        this.onEndOfMedia = handler;
    }

    public void setOnError(Consumer<String> handler) {
        this.onError = handler;
    }

    public boolean load(MediaItem item) {
        unload();

        final Media media;
        try {
            media = new Media(item.getPlaybackUri());
            mediaPlayer = new MediaPlayer(media);
        } catch (MediaException | IllegalArgumentException | NullPointerException e) {
            mediaPlayer = null;
            fireError("This media could not be opened:\n" + item.getName());
            return false;
        }

        media.setOnError(() -> fireError("Cannot read media:\n" + errorMessage(media.getError())));
        mediaPlayer.setOnError(() -> fireError("Playback failed:\n" + errorMessage(mediaPlayer.getError())));

        // Surface embedded tags (title, artist, cover) as soon as they parse.
        media.getMetadata().addListener((MapChangeListener<String, Object>) change ->
                item.applyMetadata(media.getMetadata()));

        mediaPlayer.volumeProperty().bindBidirectional(volume);
        mediaPlayer.muteProperty().bindBidirectional(muted);

        mediaPlayer.setOnReady(() -> {
            totalDuration.set(media.getDuration());
            mediaPlayer.setRate(rate.get());
            mediaPlayer.play();
        });
        mediaPlayer.setOnEndOfMedia(() -> {
            if (onEndOfMedia != null) onEndOfMedia.run();
        });
        mediaPlayer.currentTimeProperty().addListener((obs, old, time) -> currentTime.set(time));
        mediaPlayer.statusProperty().addListener((obs, old, newStatus) -> {
            status.set(newStatus == null ? MediaPlayer.Status.UNKNOWN : newStatus);
            playing.set(newStatus == MediaPlayer.Status.PLAYING);
        });
        return true;
    }

    public void unload() {
        if (mediaPlayer != null) {
            mediaPlayer.volumeProperty().unbindBidirectional(volume);
            mediaPlayer.muteProperty().unbindBidirectional(muted);
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        playing.set(false);
        status.set(MediaPlayer.Status.UNKNOWN);
        currentTime.set(Duration.ZERO);
        totalDuration.set(Duration.UNKNOWN);
    }

    public void playPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.play();
        }
    }

    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public void seekTo(Duration target) {
        if (mediaPlayer == null || target == null) return;
        MediaPlayer.Status current = mediaPlayer.getStatus();
        if (current != MediaPlayer.Status.UNKNOWN && current != MediaPlayer.Status.HALTED) {
            mediaPlayer.seek(target);
        }
    }

    public void seekBy(Duration delta) {
        if (mediaPlayer == null || delta == null) return;
        Duration target = currentTime.get().add(delta);
        if (target.lessThan(Duration.ZERO)) {
            target = Duration.ZERO;
        }
        seekTo(target);
    }

    public void setRate(double newRate) {
        rate.set(newRate);
        if (mediaPlayer != null) {
            mediaPlayer.setRate(newRate);
        }
    }

    public void adjustVolume(double delta) {
        volume.set(clamp(volume.get() + delta, 0.0, 1.0));
    }

    public void toggleMute() {
        muted.set(!muted.get());
    }

    private void fireError(String message) {
        if (onError != null) onError.accept(message);
    }

    private static String errorMessage(Throwable t) {
        return (t != null && t.getMessage() != null) ? t.getMessage() : "Unknown media error";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public ReadOnlyBooleanProperty playingProperty() {
        return playing;
    }

    public BooleanProperty mutedProperty() {
        return muted;
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public DoubleProperty rateProperty() {
        return rate;
    }

    public ReadOnlyObjectProperty<Duration> currentTimeProperty() {
        return currentTime;
    }

    public ReadOnlyObjectProperty<Duration> totalDurationProperty() {
        return totalDuration;
    }

    public ReadOnlyObjectProperty<MediaPlayer.Status> statusProperty() {
        return status;
    }
}
