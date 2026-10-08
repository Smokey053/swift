package com.swift.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public final class Splash {

    private static final double WIDTH = 480;
    private static final double HEIGHT = 320;

    private final Stage stage = new Stage(StageStyle.TRANSPARENT);
    private final SequentialTransition playIn;

    public Splash(String stylesheet) {
        ImageView play = new ImageView(Branding.image("logo-play.png", 140, 140));
        play.setFitWidth(120);
        play.setFitHeight(120);
        play.setPreserveRatio(true);
        play.setOpacity(0);

        ImageView text = new ImageView(Branding.image("logo-text.png", 260, 0));
        text.setFitWidth(240);
        text.setPreserveRatio(true);
        text.setOpacity(0);

        VBox content = new VBox(18, play, text);
        content.setAlignment(Pos.CENTER);

        StackPane card = new StackPane(content);
        card.getStyleClass().add("splash");
        card.setPadding(new Insets(24));
        card.setPrefSize(WIDTH - 40, HEIGHT - 40);
        card.setMaxSize(WIDTH - 40, HEIGHT - 40);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: transparent;");
        card.setEffect(new DropShadow(24, 0, 6, Color.rgb(0, 0, 0, 0.35)));

        Scene scene = new Scene(root, WIDTH, HEIGHT, Color.TRANSPARENT);
        scene.getStylesheets().add(stylesheet);
        stage.setScene(scene);
        stage.getIcons().addAll(Branding.appIcons());
        stage.setTitle("Swift");
        stage.setAlwaysOnTop(true);

        var bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX() + (bounds.getWidth() - WIDTH) / 2);
        stage.setY(bounds.getMinY() + (bounds.getHeight() - HEIGHT) / 2);

        playIn = animateIn(play, text);
    }

    private static SequentialTransition animateIn(ImageView play, ImageView text) {
        play.setTranslateX(-40);
        play.setScaleX(0.8);
        play.setScaleY(0.8);
        text.setTranslateY(12);

        FadeTransition playFade = new FadeTransition(Duration.millis(450), play);
        playFade.setToValue(1);
        TranslateTransition playSlide = new TranslateTransition(Duration.millis(450), play);
        playSlide.setToX(0);
        ScaleTransition playScale = new ScaleTransition(Duration.millis(450), play);
        playScale.setToX(1);
        playScale.setToY(1);

        FadeTransition textFade = new FadeTransition(Duration.millis(450), text);
        textFade.setToValue(1);
        TranslateTransition textSlide = new TranslateTransition(Duration.millis(450), text);
        textSlide.setToY(0);

        return new SequentialTransition(
                new ParallelTransition(playFade, playSlide, playScale),
                new ParallelTransition(textFade, textSlide),
                new PauseTransition(Duration.millis(700)));
    }

    public void show(Runnable onFinished) {
        stage.show();
        playIn.setOnFinished(e -> {
            FadeTransition out = new FadeTransition(Duration.millis(300), stage.getScene().getRoot());
            out.setToValue(0);
            out.setOnFinished(ev -> {
                stage.hide();
                onFinished.run();
            });
            out.play();
        });
        playIn.play();
    }
}
