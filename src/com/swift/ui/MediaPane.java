package com.swift.ui;

import com.swift.model.MediaItem;
import com.swift.player.PlayerController;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

public class MediaPane extends StackPane {

    private final PlayerController player;
    private final MediaView mediaView = new MediaView();
    private final VBox emptyState = new VBox(14);
    private final VBox audioState = new VBox(18);
    private final StackPane artHolder = new StackPane();
    private final Label audioTitle = new Label();
    private final Label audioSub = new Label();
    private final ProgressIndicator spinner = new ProgressIndicator();

    public MediaPane(Runnable addMedia, Runnable toggleFullscreen, PlayerController player) {
        this.player = player;
        getStyleClass().add("media-pane");
        setMinSize(0, 0);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(widthProperty());
        mediaView.fitHeightProperty().bind(heightProperty());
        mediaView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) toggleFullscreen.run();
        });
        mediaView.setVisible(false);

        Label icon = new Label();
        icon.setGraphic(Icons.of(Icons.MUSIC, 40));
        icon.getStyleClass().add("empty-icon");
        Label t = new Label("Nothing playing");
        t.getStyleClass().add("empty-title");
        Label s = new Label("Open files or a folder to start.");
        s.getStyleClass().add("empty-subtitle");
        s.setWrapText(true);
        s.setTextAlignment(TextAlignment.CENTER);
        Button open = new Button("Open files");
        open.getStyleClass().add("primary-button");
        open.setOnAction(e -> addMedia.run());
        emptyState.getChildren().addAll(icon, t, s, open);
        emptyState.getStyleClass().add("empty-panel");
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(24));
        emptyState.setMaxSize(360, VBox.USE_PREF_SIZE);

        artHolder.setMinSize(0, 0);
        artHolder.getStyleClass().add("now-art-holder");
        audioTitle.getStyleClass().add("now-title");
        audioTitle.setWrapText(true);
        audioTitle.setTextAlignment(TextAlignment.CENTER);
        audioTitle.setMinHeight(0);
        audioSub.getStyleClass().add("now-sub");
        audioSub.setWrapText(true);
        audioSub.setTextAlignment(TextAlignment.CENTER);
        audioState.getChildren().addAll(artHolder, audioTitle, audioSub);
        audioState.getStyleClass().add("audio-state");
        audioState.setAlignment(Pos.CENTER);
        audioState.setPadding(new Insets(24));
        audioState.setMinSize(0, 0);
        audioState.setVisible(false);
        audioState.maxWidthProperty().bind(widthProperty());
        audioState.setMaxHeight(VBox.USE_PREF_SIZE);

        spinner.setMaxSize(40, 40);
        spinner.setVisible(false);
        spinner.setMouseTransparent(true);

        getChildren().addAll(emptyState, audioState, mediaView, spinner);

        player.statusProperty().addListener((obs, old, st) ->
                spinner.setVisible(st == MediaPlayer.Status.STALLED));
    }

    public void showMedia(MediaItem item) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        boolean video = item.getType() == MediaItem.Type.VIDEO;
        mediaView.setMediaPlayer(player.getMediaPlayer());
        mediaView.setVisible(video);
        audioState.setVisible(!video);
        audioState.setManaged(!video);
        if (!video) {
            artHolder.getChildren().setAll(Art.fluid(item, 24));
            artHolder.maxWidthProperty().unbind();
            artHolder.maxWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> Math.max(72, Math.min(260, Math.min(getWidth(), getHeight()) * 0.4)),
                    widthProperty(), heightProperty()));
            audioTitle.textProperty().bind(Bindings.createStringBinding(
                    item::getDisplayName, item.titleProperty()));
            audioSub.textProperty().bind(Bindings.createStringBinding(() -> {
                String artist = item.getArtist();
                return artist != null ? artist : item.getTypeLabel();
            }, item.artistProperty()));
        }
    }

    public void showEmpty() {
        audioTitle.textProperty().unbind();
        audioSub.textProperty().unbind();
        mediaView.setMediaPlayer(null);
        mediaView.setVisible(false);
        audioState.setVisible(false);
        audioState.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
    }
}
