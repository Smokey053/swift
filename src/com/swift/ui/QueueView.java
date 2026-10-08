package com.swift.ui;

import com.swift.model.MediaItem;
import com.swift.model.Playlist;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class QueueView extends VBox {

    private final Playlist playlist;
    private final FilteredList<MediaItem> filtered;
    private final ListView<MediaItem> listView;
    private boolean syncing;

    public QueueView(Playlist playlist, StringProperty query, Runnable addMedia) {
        this.playlist = playlist;
        this.filtered = new FilteredList<>(playlist.getItems(), item -> true);

        getStyleClass().add("page-content");
        setSpacing(22);
        setMinSize(0, 0);
        // Responsive reflow: gutters adapt to the viewport width.
        widthProperty().addListener((obs, old, w) -> setPadding(HomeView.pagePadding(w.doubleValue())));
        setPadding(HomeView.pagePadding(getWidth()));

        Label title = new Label("Queue");
        title.getStyleClass().add("page-title");
        Label count = new Label();
        count.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(6, title, count);
        titles.setMinWidth(USE_PREF_SIZE);

        Button add = new Button("Add files");
        add.setGraphic(Icons.of(Icons.ADD, 16));
        add.getStyleClass().add("secondary-button");
        add.setOnAction(e -> addMedia.run());
        Button clear = new Button("Clear");
        clear.getStyleClass().add("secondary-button");
        clear.setOnAction(e -> playlist.clear());
        clear.disableProperty().bind(Bindings.isEmpty(playlist.getItems()));
        FlowPane actions = new FlowPane(8, 8, add, clear);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMinWidth(0);
        HBox.setHgrow(actions, Priority.ALWAYS);

        HBox head = new HBox(16, titles, actions);
        head.setAlignment(Pos.CENTER_LEFT);

        listView = new ListView<>(filtered);
        listView.getStyleClass().add("queue-list");
        listView.setCellFactory(lv -> new Row());
        listView.setMinSize(0, 0);
        VBox.setVgrow(listView, Priority.ALWAYS);

        Label placeholder = new Label();
        placeholder.getStyleClass().add("muted-note");
        placeholder.setWrapText(true);
        placeholder.setTextAlignment(TextAlignment.CENTER);
        listView.setPlaceholder(placeholder);

        Runnable labels = () -> {
            String q = query.get() == null ? "" : query.get().trim().toLowerCase();
            int total = playlist.getItems().size();
            int shown = filtered.size();
            count.setText(shown == total
                    ? total + (total == 1 ? " item" : " items")
                    : shown + " of " + total + " items");
            placeholder.setText(total == 0
                    ? "Your queue is empty.\nOpen files to start listening."
                    : "No results for “" + q + "”");
        };
        Runnable refresh = () -> {
            String q = query.get() == null ? "" : query.get().trim().toLowerCase();
            filtered.setPredicate(item -> q.isEmpty() || item.getDisplayName().toLowerCase().contains(q)
                    || item.getName().toLowerCase().contains(q)
                    || (item.getArtist() != null && item.getArtist().toLowerCase().contains(q))
                    || item.getExtension().contains(q));
        };
        query.addListener((obs, old, q) -> { refresh.run(); labels.run(); });
        filtered.addListener((ListChangeListener<MediaItem>) c -> labels.run());
        refresh.run();
        labels.run();

        wireSelection();
        getChildren().addAll(head, listView);
    }

    private void wireSelection() {
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, item) -> {
            if (syncing || item == null) return;
            int index = playlist.getItems().indexOf(item);
            if (index >= 0) playlist.select(index);
        });
        playlist.currentIndexProperty().addListener((obs, old, index) -> {
            syncing = true;
            MediaItem item = playlist.getCurrentItem();
            int i = item == null ? -1 : filtered.indexOf(item);
            if (i >= 0) {
                listView.getSelectionModel().select(i);
                listView.scrollTo(i);
            } else {
                listView.getSelectionModel().clearSelection();
            }
            syncing = false;
            listView.refresh();
        });
    }

    private class Row extends ListCell<MediaItem> {
        private final Label indicator = new Label();
        private final Label name = new Label();
        private final Label meta = new Label();
        private final Button remove = new Button();
        private final HBox layout;

        Row() {
            setPrefWidth(0);
            indicator.getStyleClass().add("track-index");
            indicator.setMinWidth(28);
            name.getStyleClass().add("card-title");
            name.setMinWidth(0);
            meta.getStyleClass().add("card-sub");
            meta.setMinWidth(0);
            VBox text = new VBox(2, name, meta);
            text.setAlignment(Pos.CENTER_LEFT);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);

            remove.setGraphic(Icons.of(Icons.CLOSE, 14));
            remove.getStyleClass().add("clear-button");
            remove.setAccessibleText("Remove from queue");

            layout = new HBox(12, indicator, new StackPane(), text, remove);
            layout.setAlignment(Pos.CENTER_LEFT);
            layout.setMinWidth(0);
        }

        @Override
        protected void updateItem(MediaItem item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().remove("playing");
            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }
            boolean active = item == playlist.getCurrentItem();
            indicator.setText(active ? "▶" : String.valueOf(playlist.getItems().indexOf(item) + 1));
            name.textProperty().unbind();
            name.textProperty().bind(Bindings.createStringBinding(
                    item::getDisplayName, item.titleProperty()));
            meta.textProperty().unbind();
            meta.textProperty().bind(Bindings.createStringBinding(() -> {
                String artist = item.getArtist();
                if (artist != null) return artist;
                String ext = item.getExtension();
                return item.getTypeLabel() + (ext.isEmpty() ? "" : " • " + ext.toUpperCase());
            }, item.artistProperty()));
            layout.getChildren().set(1, Art.fixed(item, 40, 8));
            remove.setOnAction(e -> playlist.remove(item));
            if (active) getStyleClass().add("playing");
            setGraphic(layout);

            MenuItem play = new MenuItem("Play");
            play.setOnAction(e -> {
                int index = playlist.getItems().indexOf(item);
                if (index >= 0) playlist.select(index);
            });
            MenuItem del = new MenuItem("Remove from queue");
            del.setOnAction(e -> playlist.remove(item));
            setContextMenu(new ContextMenu(play, del));
        }
    }
}
