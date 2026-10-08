package com.swift.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Sidebar extends VBox {

    public enum View { HOME, NOW_PLAYING, QUEUE }

    public enum Mode { FULL, RAIL }

    private final ObjectProperty<View> currentView = new SimpleObjectProperty<>(View.HOME);
    private final Map<View, ToggleButton> railButtons = new EnumMap<>(View.class);
    private final Map<View, ToggleButton> tabButtons = new EnumMap<>(View.class);
    private final List<Node> fullOnly = new ArrayList<>();
    private final List<ButtonBase> sideButtons = new ArrayList<>();
    private final HBox tabBar = new HBox();
    private HBox brand;
    private boolean syncing;

    public Sidebar(Runnable openFiles, Runnable openFolder) {
        getStyleClass().add("sidebar");
        setSpacing(14);
        setMinHeight(0);

        ImageView logoImage = new ImageView(Branding.image("logo-play.png", 36, 36));
        logoImage.setFitWidth(36);
        logoImage.setFitHeight(36);
        logoImage.setPreserveRatio(true);
        Label brandName = new Label("Swift");
        brandName.getStyleClass().add("brand-name");
        fullOnly.add(brandName);
        brand = new HBox(12, logoImage, brandName);
        brand.setPadding(new Insets(0, 0, 24, 0));
        getChildren().add(brand);

        ToggleGroup railGroup = new ToggleGroup();
        ToggleGroup tabGroup = new ToggleGroup();
        addNav(View.HOME, "Home", Icons.HOME, railGroup, tabGroup);
        addNav(View.NOW_PLAYING, "Now playing", Icons.MUSIC, railGroup, tabGroup);
        addNav(View.QUEUE, "Queue", Icons.QUEUE, railGroup, tabGroup);

        Separator divider = new Separator();
        divider.setPadding(new Insets(16, 0, 8, 0));
        Label library = new Label("LIBRARY");
        library.getStyleClass().add("section-label");
        fullOnly.add(library);
        getChildren().addAll(divider, library,
                actionButton("Open files", Icons.ADD, openFiles),
                actionButton("Open folder", Icons.FOLDER, openFolder));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        tabBar.getStyleClass().add("tab-bar");
        tabBar.setVisible(false);
        tabBar.setManaged(false);

        wire(railGroup, railButtons, tabButtons);
        wire(tabGroup, tabButtons, railButtons);
        currentView.addListener((obs, old, view) -> {
            railButtons.get(view).setSelected(true);
            tabButtons.get(view).setSelected(true);
        });
        railButtons.get(View.HOME).setSelected(true);
        tabButtons.get(View.HOME).setSelected(true);

        setMode(Mode.FULL);
    }

    private void wire(ToggleGroup group, Map<View, ToggleButton> own, Map<View, ToggleButton> other) {
        group.selectedToggleProperty().addListener((obs, old, toggle) -> {
            if (syncing) return;
            syncing = true;
            try {
                if (toggle == null) {
                    own.get(currentView.get()).setSelected(true);
                } else {
                    currentView.set((View) toggle.getUserData());
                    other.get(currentView.get()).setSelected(true);
                }
            } finally {
                syncing = false;
            }
        });
    }

    private void addNav(View view, String label, String icon, ToggleGroup railGroup, ToggleGroup tabGroup) {
        Label text = new Label(label);
        fullOnly.add(text);
        HBox content = new HBox(14, Icons.of(icon, 18), text);
        ToggleButton rail = new ToggleButton();
        rail.setGraphic(content);
        rail.getStyleClass().add("nav-button");
        rail.setToggleGroup(railGroup);
        rail.setUserData(view);
        rail.setMaxWidth(Double.MAX_VALUE);
        rail.setTooltip(new Tooltip(label));
        railButtons.put(view, rail);
        sideButtons.add(rail);
        getChildren().add(rail);

        Label tabText = new Label(label);
        tabText.getStyleClass().add("tab-label");
        VBox tabContent = new VBox(3, Icons.of(icon, 20), tabText);
        tabContent.setAlignment(Pos.CENTER);
        ToggleButton tab = new ToggleButton();
        tab.setGraphic(tabContent);
        tab.getStyleClass().add("tab-button");
        tab.setToggleGroup(tabGroup);
        tab.setUserData(view);
        tab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tab, Priority.ALWAYS);
        tabButtons.put(view, tab);
        tabBar.getChildren().add(tab);
    }

    private Button actionButton(String label, String icon, Runnable action) {
        Label text = new Label(label);
        fullOnly.add(text);
        HBox content = new HBox(14, Icons.of(icon, 18), text);
        Button button = new Button();
        button.setGraphic(content);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setTooltip(new Tooltip(label));
        button.setOnAction(e -> action.run());
        sideButtons.add(button);
        return button;
    }

    public void setMode(Mode mode) {
        boolean full = mode == Mode.FULL;
        double width = full ? 208 : 64;
        setMinWidth(width);
        setPrefWidth(width);
        setMaxWidth(width);
        setPadding(full ? new Insets(26, 16, 22, 16) : new Insets(26, 10, 22, 10));
        for (Node n : fullOnly) {
            n.setVisible(full);
            n.setManaged(full);
        }
        Pos alignment = full ? Pos.CENTER_LEFT : Pos.CENTER;
        brand.setAlignment(alignment);
        for (ButtonBase b : sideButtons) {
            b.setAlignment(alignment);
            if (b.getGraphic() instanceof HBox content) content.setAlignment(alignment);
        }
    }

    public HBox getTabBar() {
        return tabBar;
    }

    public ObjectProperty<View> currentViewProperty() {
        return currentView;
    }

    public void setView(View view) {
        currentView.set(view);
    }
}
