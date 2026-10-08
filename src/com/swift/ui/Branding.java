package com.swift.ui;

import javafx.scene.image.Image;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.List;
import java.util.Objects;

public final class Branding {

    private static final String BASE = "/com/swift/resources/";
    private static final List<String> FONTS = List.of("Regular", "Medium", "SemiBold", "Bold");

    private Branding() {}

    public static void loadFonts() {
        for (String weight : FONTS) {
            URL url = Branding.class.getResource(BASE + "fonts/Inter-" + weight + ".ttf");
            if (url != null) {
                Font.loadFont(url.toExternalForm(), 13);
            }
        }
    }

    public static Image image(String name, double width, double height) {
        URL url = Objects.requireNonNull(Branding.class.getResource(BASE + "images/" + name), name);
        return new Image(url.toExternalForm(), width, height, true, true);
    }

    public static List<Image> appIcons() {
        return List.of(
                image("icon-32.png", 32, 32),
                image("icon-64.png", 64, 64),
                image("icon-256.png", 256, 256));
    }
}
