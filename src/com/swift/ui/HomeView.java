package com.swift.ui;

import com.swift.model.MediaItem;
import com.swift.model.RecentStore;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HomeView extends ScrollPane {

    private static final int QUICK_PICKS = 4;

    private final RecentStore recents;
    private final StringProperty query;
    private final Runnable openFiles;
    private final Runnable openFolder;
    private final Consumer<MediaItem> play;
    private final VBox content = new VBox(36);

    public HomeView(RecentStore recents, StringProperty query, Runnable openFiles,
                    Runnable openFolder, Consumer<MediaItem> play) {
        this.recents = recents;
        this.query = query;
        this.openFiles = openFiles;
        this.openFolder = openFolder;
        this.play = play;

        getStyleClass().add("page");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setMinSize(0, 0);

        content.getStyleClass().add("page-content");
        content.setMinWidth(0);
        setContent(content);
        // Responsive reflow: page gutters grow/shrink with the viewport.
        widthProperty().addListener((obs, old, w) -> content.setPadding(pagePadding(w.doubleValue())));
        content.setPadding(pagePadding(getWidth()));

        recents.getRecents().addListener((ListChangeListener<MediaItem>) c -> rebuild());
        query.addListener((obs, old, q) -> rebuild());
        rebuild();
    }

    private void rebuild() {
        String q = query.get() == null ? "" : query.get().trim().toLowerCase();
        List<MediaItem> items = recents.getRecents().stream()
                .filter(i -> q.isEmpty() || i.getDisplayName().toLowerCase().contains(q)
                        || i.getName().toLowerCase().contains(q)
                        || (i.getArtist() != null && i.getArtist().toLowerCase().contains(q)))
                .toList();

        content.getChildren().clear();
        content.getChildren().add(header());

        if (recents.getRecents().isEmpty()) {
            content.getChildren().add(emptyState());
            return;
        }
        if (items.isEmpty()) {
            Label none = new Label("No results for “" + query.get().trim() + "”");
            none.getStyleClass().add("muted-note");
            none.setWrapText(true);
            content.getChildren().add(none);
            return;
        }

        List<MediaItem> picks = items.subList(0, Math.min(QUICK_PICKS, items.size()));
        ResponsiveGrid quick = new ResponsiveGrid(22, 280, 2);
        quick.setNodes(picks.stream().map(this::quickCard).toList());
        content.getChildren().add(section("Recently played", quick, true));

        if (items.size() > QUICK_PICKS) {
            ResponsiveGrid tiles = new ResponsiveGrid(20, 150, 8);
            List<Node> nodes = new ArrayList<>();
            for (MediaItem item : items.subList(QUICK_PICKS, items.size())) nodes.add(tile(item));
            tiles.setNodes(nodes);
            content.getChildren().add(section("Earlier", tiles, false));
        }
    }

    /** Generous outer gutters that adapt to the available width. */
    static Insets pagePadding(double w) {
        double h = w >= 960 ? 48 : w >= 640 ? 36 : 22;
        return new Insets(32, h, 48, h);
    }

    private VBox header() {
        Label title = new Label("Home");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Your media, played entirely offline.");
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);
        VBox box = new VBox(6, title, subtitle);
        box.setPadding(new Insets(0, 0, 4, 0));
        return box;
    }

    private VBox section(String title, Node body, boolean withClear) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox head = new HBox(12, label, spacer);
        head.setAlignment(Pos.BASELINE_LEFT);
        if (withClear) {
            Button clear = new Button("Clear history");
            clear.getStyleClass().add("link-button");
            clear.setOnAction(e -> recents.clear());
            head.getChildren().add(clear);
        }
        return new VBox(18, head, body);
    }

    private VBox emptyState() {
        Label icon = new Label();
        icon.setGraphic(Icons.of(Icons.MUSIC, 40));
        icon.getStyleClass().add("empty-icon");

        Label title = new Label("Nothing played yet");
        title.getStyleClass().add("empty-title");
        Label subtitle = new Label("Open some music or videos, or drop files onto this window.");
        subtitle.getStyleClass().add("empty-subtitle");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);

        Button files = new Button("Open files");
        files.getStyleClass().add("primary-button");
        files.setOnAction(e -> openFiles.run());
        Button folder = new Button("Open folder");
        folder.getStyleClass().add("secondary-button");
        folder.setOnAction(e -> openFolder.run());
        FlowPane actions = new FlowPane(12, 12, files, folder);
        actions.setAlignment(Pos.CENTER);

        VBox box = new VBox(16, icon, title, subtitle, actions);
        box.getStyleClass().add("empty-card");
        box.setAlignment(Pos.CENTER);
        // Padding comes from .empty-card in the stylesheet.
        return box;
    }

    private HBox quickCard(MediaItem item) {
        Label title = new Label();
        title.textProperty().bind(Bindings.createStringBinding(
                item::getDisplayName, item.titleProperty()));
        title.getStyleClass().add("card-title");
        title.setMinWidth(0);
        Label sub = new Label();
        sub.textProperty().bind(subtitleBinding(item));
        sub.getStyleClass().add("card-sub");
        sub.setMinWidth(0);
        VBox text = new VBox(3, title, sub);
        text.setAlignment(Pos.CENTER_LEFT);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox card = new HBox(16, Art.fixed(item, 64, 12), text);
        card.getStyleClass().add("quick-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinWidth(0);
        makeClickable(card, item);
        return card;
    }

    private VBox tile(MediaItem item) {
        Label title = new Label();
        title.textProperty().bind(Bindings.createStringBinding(
                item::getDisplayName, item.titleProperty()));
        title.getStyleClass().add("card-title");
        title.setMinWidth(0);
        Label sub = new Label();
        sub.textProperty().bind(subtitleBinding(item));
        sub.getStyleClass().add("card-sub");
        sub.setMinWidth(0);

        VBox tile = new VBox(8, Art.fluid(item, 14), new VBox(2, title, sub));
        tile.getStyleClass().add("tile");
        tile.setMinWidth(0);
        makeClickable(tile, item);
        return tile;
    }

    /** Artist from metadata when known, otherwise the generic type label. */
    private static StringBinding subtitleBinding(MediaItem item) {
        return Bindings.createStringBinding(() -> {
            String artist = item.getArtist();
            return artist != null ? artist : item.getTypeLabel();
        }, item.artistProperty());
    }

    private void makeClickable(Region card, MediaItem item) {
        card.setFocusTraversable(true);
        card.setAccessibleText("Play " + item.getName());
        Tooltip.install(card, new Tooltip(item.getName()));
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) play.accept(item);
        });
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) play.accept(item);
        });

        MenuItem playItem = new MenuItem("Play");
        playItem.setOnAction(e -> play.accept(item));
        MenuItem remove = new MenuItem("Remove from history");
        remove.setOnAction(e -> recents.remove(item));
        ContextMenu menu = new ContextMenu(playItem, remove);
        card.setOnContextMenuRequested(e -> {
            menu.show(card, e.getScreenX(), e.getScreenY());
            e.consume();
        });
    }
}
