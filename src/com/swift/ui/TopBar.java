package com.swift.ui;

import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class TopBar extends HBox {

    public TopBar(StringProperty query, Runnable openFiles, Runnable openFolder) {
        getStyleClass().add("top-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        setMinHeight(USE_PREF_SIZE);
        query.set("");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        spacer.setMinWidth(0);

        MenuButton open = new MenuButton("Open");
        open.setGraphic(Icons.of(Icons.ADD, 16));
        open.getStyleClass().add("open-menu");
        open.setMinWidth(USE_PREF_SIZE);
        MenuItem files = new MenuItem("Open files…");
        files.setOnAction(e -> openFiles.run());
        MenuItem folder = new MenuItem("Open folder…");
        folder.setOnAction(e -> openFolder.run());
        open.getItems().addAll(files, folder);

        getChildren().addAll(spacer, open);
    }
}
