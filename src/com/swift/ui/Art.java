package com.swift.ui;

import com.swift.model.MediaItem;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.util.Locale;

public class Art extends StackPane {

    private static final String[][] PALETTE = {
            {"#0b1530", "#3159a6"},
            {"#123f73", "#2f69d9"},
            {"#1e5b98", "#4c86f7"},
            {"#10243f", "#2563eb"},
            {"#06b6d4", "#2563eb"},
            {"#0f6d9a", "#38bdf8"},
            {"#155e75", "#2dd4bf"},
    };

    private final boolean fluid;
    private final Region glyph;
    private final ImageView cover;
    private final ChangeListener<Image> artworkListener;

    private Art(MediaItem item, double radius, boolean fluid) {
        this.fluid = fluid;
        getStyleClass().add("art");
        setMinSize(0, 0);

        String name = item == null ? "" : item.getName();
        String[] colours = PALETTE[Math.floorMod(name.hashCode(), PALETTE.length)];
        setStyle(String.format(Locale.ROOT,
                "-fx-background-color: linear-gradient(to bottom right, %s, %s); -fx-background-radius: %.0f;",
                colours[0], colours[1], radius));

        boolean video = item != null && item.getType() == MediaItem.Type.VIDEO;
        glyph = Icons.of(video ? Icons.VIDEO : Icons.MUSIC);
        glyph.getStyleClass().add("art-glyph");
        glyph.setMinSize(0, 0);
        glyph.prefWidthProperty().bind(widthProperty().multiply(0.42));
        glyph.prefHeightProperty().bind(widthProperty().multiply(0.42));
        glyph.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);

        cover = new ImageView();
        cover.setPreserveRatio(true);
        cover.setSmooth(true);
        cover.fitWidthProperty().bind(widthProperty());
        cover.fitHeightProperty().bind(heightProperty());
        cover.setVisible(false);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        cover.setClip(clip);

        getChildren().addAll(glyph, cover);

        artworkListener = (obs, old, image) -> showArtwork(image);
        if (item != null) {
            item.artworkProperty().addListener(new WeakChangeListener<>(artworkListener));
            showArtwork(item.getArtwork());
        }
    }

    private void showArtwork(Image image) {
        boolean has = image != null && !image.isError();
        cover.setImage(has ? image : null);
        cover.setVisible(has);
        glyph.setVisible(!has);
    }

    public static Art fixed(MediaItem item, double size, double radius) {
        Art art = new Art(item, radius, false);
        art.setMinSize(size, size);
        art.setPrefSize(size, size);
        art.setMaxSize(size, size);
        return art;
    }

    public static Art fluid(MediaItem item, double radius) {
        return new Art(item, radius, true);
    }

    @Override
    public Orientation getContentBias() {
        return fluid ? Orientation.HORIZONTAL : super.getContentBias();
    }

    @Override
    protected double computePrefWidth(double height) {
        return fluid ? 96 : super.computePrefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width) {
        return fluid ? (width >= 0 ? width : 96) : super.computePrefHeight(width);
    }

    @Override
    protected double computeMinHeight(double width) {
        return fluid ? (width >= 0 ? width : 0) : super.computeMinHeight(width);
    }
}
