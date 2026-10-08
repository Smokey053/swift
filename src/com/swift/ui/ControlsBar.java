package com.swift.ui;

import com.swift.model.MediaItem;
import com.swift.model.RepeatMode;
import com.swift.player.PlayerController;
import com.swift.util.FormatUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.Locale;

public class ControlsBar extends HBox {

    private final PlayerController player;
    private final HBox left = new HBox(14);
    private final VBox centre = new VBox(10);
    private final HBox right = new HBox(8);
    private final HBox progressRow = new HBox(10);
    private final VBox text = new VBox(2);
    private final StackPane artHolder = new StackPane();
    private final Label title = new Label("Nothing playing");
    private final Label sub = new Label("Open a file to begin");

    private final Slider seek = new Slider(0, 1, 0);
    private final Slider volume = new Slider(0, 1, 0.8);
    private final Label elapsed = new Label("0:00");
    private final Label total = new Label("0:00");
    private final Button playButton = new Button();
    private final Node playIcon = Icons.of(Icons.PLAY, 18);
    private final Node pauseIcon = Icons.of(Icons.PAUSE, 18);
    private final Button muteButton = new Button();
    private boolean showRemaining;
    private boolean seeking;

    private final Node[] fullOnly;

    public ControlsBar(PlayerController player, BooleanProperty shuffle,
                       ObjectProperty<RepeatMode> repeat, Runnable previous, Runnable next,
                       Runnable toggleFullscreen) {
        this.player = player;
        getStyleClass().add("controls-bar");
        setAlignment(Pos.CENTER);
        setMinWidth(0);
        setMinHeight(88);
        setPadding(new Insets(16, 28, 16, 28));
        setSpacing(24);

        buildLeft();
        buildCentre(shuffle, repeat, previous, next);
        buildRight(toggleFullscreen);
        fullOnly = new Node[]{volume};

        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        for (HBox side : new HBox[]{left, right}) {
            side.setPrefWidth(0);
            side.setMinWidth(0);
        }
        centre.setMinWidth(260);
        centre.setMaxWidth(Region.USE_PREF_SIZE);
        centre.prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(260, Math.min(560, getWidth() * 0.4)), widthProperty()));
        getChildren().addAll(left, centre, right);

        widthProperty().addListener((obs, old, w) -> applyBreakpoints(w.doubleValue()));
        applyBreakpoints(1000);
        bindPlayer();
    }

    public void showItem(MediaItem item) {
        artHolder.getChildren().clear();
        artHolder.setVisible(item != null);
        artHolder.setManaged(item != null);
        title.textProperty().unbind();
        sub.textProperty().unbind();
        if (item == null) {
            title.setText("Nothing playing");
            sub.setText("Open a file to begin");
            return;
        }
        artHolder.getChildren().add(Art.fixed(item, 48, 8));
        title.textProperty().bind(Bindings.createStringBinding(
                item::getDisplayName, item.titleProperty()));
        sub.textProperty().bind(Bindings.createStringBinding(() -> {
            String artist = item.getArtist();
            return artist != null ? artist : item.getTypeLabel();
        }, item.artistProperty()));
    }

    private void buildLeft() {
        left.setAlignment(Pos.CENTER_LEFT);
        left.getStyleClass().add("player-meta");
        artHolder.setMinSize(52, 52);
        artHolder.setVisible(false);
        artHolder.setManaged(false);
        artHolder.setPrefSize(52, 52);
        artHolder.setMaxSize(52, 52);
        title.getStyleClass().add("track-title");
        sub.getStyleClass().add("track-sub");
        title.setMinWidth(0);
        sub.setMinWidth(0);
        title.setMaxWidth(Double.MAX_VALUE);
        text.getChildren().addAll(title, sub);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        left.getChildren().addAll(artHolder, text);
    }

    private void buildCentre(BooleanProperty shuffle, ObjectProperty<RepeatMode> repeat,
                             Runnable previous, Runnable next) {
        centre.setAlignment(Pos.CENTER);

        Button shuffleButton = iconButton(Icons.SHUFFLE, "Shuffle", "mode-button");
        shuffleButton.setOnAction(e -> shuffle.set(!shuffle.get()));
        shuffle.addListener((obs, old, on) -> shuffleButton.pseudoClassStateChanged(ACTIVE, on));

        Button prev = iconButton(Icons.PREVIOUS, "Previous (P)", "transport-button");
        prev.setOnAction(e -> previous.run());
        Button stop = iconButton(Icons.STOP, "Stop (S)", "transport-button");
        stop.setOnAction(e -> player.stop());
        Button nextButton = iconButton(Icons.NEXT, "Next (N)", "transport-button");
        nextButton.setOnAction(e -> next.run());

        playButton.getStyleClass().add("play-button");
        playButton.setGraphic(playIcon);
        playButton.setTooltip(new Tooltip("Play / Pause (Space)"));
        playButton.setOnAction(e -> player.playPause());
        playButton.setMinSize(40, 40);
        playButton.setPrefSize(40, 40);
        playButton.setMaxSize(40, 40);

        Button repeatButton = iconButton(Icons.REPEAT, "Repeat", "mode-button");
        repeatButton.setOnAction(e -> repeat.set(repeat.get().next()));
        repeat.addListener((obs, old, mode) -> {
            repeatButton.setGraphic(Icons.of(mode == RepeatMode.ONE ? Icons.REPEAT_ONE : Icons.REPEAT, 16));
            repeatButton.pseudoClassStateChanged(ACTIVE, mode != RepeatMode.OFF);
        });

        HBox transport = new HBox(12, shuffleButton, prev, stop, playButton, nextButton, repeatButton);
        transport.getStyleClass().add("transport-row");
        transport.setAlignment(Pos.CENTER);

        elapsed.getStyleClass().add("time-label");
        total.getStyleClass().add("time-label");
        elapsed.setMinWidth(Region.USE_PREF_SIZE);
        total.setMinWidth(Region.USE_PREF_SIZE);
        total.setTooltip(new Tooltip("Click to toggle remaining time"));
        total.setOnMouseClicked(e -> {
            showRemaining = !showRemaining;
            refreshTimes();
        });
        seek.getStyleClass().add("seek-slider");
        seek.setMinWidth(0);
        HBox.setHgrow(seek, Priority.ALWAYS);
        seek.setMaxWidth(Double.MAX_VALUE);
        progressRow.getChildren().addAll(elapsed, seek, total);
        progressRow.getStyleClass().add("progress-row");
        progressRow.setAlignment(Pos.CENTER);
        progressRow.setVisible(false);
        progressRow.setManaged(false);

        centre.getChildren().addAll(transport, progressRow);
        trackFill(seek);
    }

    private void buildRight(Runnable toggleFullscreen) {
        right.setAlignment(Pos.CENTER_RIGHT);

        muteButton.getStyleClass().add("transport-button");
        muteButton.setGraphic(Icons.of(Icons.VOLUME_HIGH, 18));
        muteButton.setTooltip(new Tooltip("Mute (M)"));
        muteButton.setOnAction(e -> player.toggleMute());

        volume.getStyleClass().add("volume-slider");
        volume.setPrefWidth(64);
        volume.setMinWidth(48);
        volume.setMaxWidth(64);
        volume.valueProperty().bindBidirectional(player.volumeProperty());
        trackFill(volume);

        Button fullscreen = iconButton(Icons.FULLSCREEN, "Fullscreen (F)", "transport-button");
        fullscreen.setOnAction(e -> toggleFullscreen.run());

        // The neumorphic well hugs its contents instead of stretching across
        // the whole right column and leaving dead space.
        HBox pill = new HBox(8, muteButton, volume, fullscreen);
        pill.getStyleClass().add("player-tools");
        pill.setAlignment(Pos.CENTER_RIGHT);
        pill.setMaxWidth(Region.USE_PREF_SIZE);
        right.getChildren().add(pill);
    }

    private void bindPlayer() {
        player.playingProperty().addListener((obs, old, p) -> playButton.setGraphic(p ? pauseIcon : playIcon));

        player.totalDurationProperty().addListener((obs, old, d) -> {
            boolean known = d != null && !d.isUnknown() && !d.isIndefinite();
            seek.setDisable(!known);
            progressRow.setVisible(known);
            progressRow.setManaged(known);
            seek.setMax(known ? d.toSeconds() : 1);
            refreshTimes();
        });
        player.currentTimeProperty().addListener((obs, old, t) -> {
            if (!seeking && !seek.isValueChanging()) seek.setValue(t.toSeconds());
            refreshTimes();
        });
        seek.valueChangingProperty().addListener((obs, was, now) -> {
            seeking = now;
            if (!now) player.seekTo(Duration.seconds(seek.getValue()));
        });
        seek.setOnMouseReleased(e -> player.seekTo(Duration.seconds(seek.getValue())));
        seek.setDisable(true);

        Runnable volumeIcon = () -> {
            double v = player.volumeProperty().get();
            String path = player.mutedProperty().get() || v == 0 ? Icons.VOLUME_MUTE
                    : v < 0.5 ? Icons.VOLUME_LOW : Icons.VOLUME_HIGH;
            muteButton.setGraphic(Icons.of(path, 18));
        };
        player.volumeProperty().addListener((obs, o, n) -> volumeIcon.run());
        player.mutedProperty().addListener((obs, o, n) -> volumeIcon.run());
    }

    private void refreshTimes() {
        Duration t = player.currentTimeProperty().get();
        Duration d = player.totalDurationProperty().get();
        elapsed.setText(FormatUtils.format(t));
        boolean known = d != null && !d.isUnknown() && !d.isIndefinite();
        if (!known) {
            total.setText("--:--");
        } else if (showRemaining) {
            total.setText("-" + FormatUtils.format(d.subtract(t)));
        } else {
            total.setText(FormatUtils.format(d));
        }
    }

    private void applyBreakpoints(double w) {
        boolean full = w >= 960;
        for (Node n : fullOnly) {
            n.setVisible(full);
            n.setManaged(full);
        }
        boolean extras = w >= 640;
        right.setVisible(extras);
        text.setVisible(w >= 560);
        text.setManaged(w >= 560);
        setPadding(w >= 640 ? new Insets(16, 28, 16, 28) : new Insets(12, 16, 12, 16));
        setSpacing(w >= 640 ? 24 : 12);
    }

    private Button iconButton(String path, String tip, String styleClass) {
        Button b = new Button();
        b.setGraphic(Icons.of(path, 16));
        b.getStyleClass().add(styleClass);
        b.setTooltip(new Tooltip(tip));
        b.setAccessibleText(tip);
        return b;
    }

    private static final javafx.css.PseudoClass ACTIVE = javafx.css.PseudoClass.getPseudoClass("active");

    private static void trackFill(Slider slider) {
        Runnable update = () -> {
            Node track = slider.lookup(".track");
            if (track == null) return;
            double range = slider.getMax() - slider.getMin();
            double pct = range <= 0 ? 0 : (slider.getValue() - slider.getMin()) / range * 100;
            pct = Math.max(0, Math.min(100, pct));
            if (slider.isDisabled() || pct == 0) {
                track.setStyle("");
                return;
            }
            String p = String.format(Locale.ROOT, "%.2f%%", pct);
            track.setStyle("-fx-background-color: linear-gradient(to right, #2f69d9 " + p
                    + ", #b9cbe6 " + p + ");");
        };
        slider.valueProperty().addListener((o, a, b) -> update.run());
        slider.maxProperty().addListener((o, a, b) -> update.run());
        slider.minProperty().addListener((o, a, b) -> update.run());
        slider.disabledProperty().addListener((o, a, b) -> update.run());
        slider.skinProperty().addListener((o, a, b) -> Platform.runLater(update));
        slider.widthProperty().addListener((o, a, b) -> update.run());
    }
}
