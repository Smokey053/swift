package com.swift;

import com.swift.model.MediaItem;
import com.swift.model.Playlist;
import com.swift.model.RecentStore;
import com.swift.model.RepeatMode;
import com.swift.player.MetadataReader;
import com.swift.player.PlayerController;
import com.swift.player.Transcoder;
import com.swift.ui.Branding;
import com.swift.ui.ControlsBar;
import com.swift.ui.HomeView;
import com.swift.ui.MediaPane;
import com.swift.ui.QueueView;
import com.swift.ui.Splash;
import com.swift.ui.Icons;
import com.swift.ui.Sidebar;
import com.swift.ui.TopBar;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SwiftApp extends Application {

    private static final double FULL_SIDEBAR_MIN = 960;
    private static final double RAIL_MIN = 640;

    private final Playlist playlist = new Playlist();
    private final PlayerController player = new PlayerController();
    private final RecentStore recents = new RecentStore();
    private final MetadataReader metadata = new MetadataReader();
    private final BooleanProperty shuffle = new SimpleBooleanProperty(false);
    private final ObjectProperty<RepeatMode> repeat = new SimpleObjectProperty<>(RepeatMode.OFF);
    private final StringProperty query = new SimpleStringProperty("");
    private final Random random = new Random();

    private Stage stage;
    private Sidebar sidebar;
    private TopBar topBar;
    private HomeView home;
    private QueueView queue;
    private MediaPane mediaPane;
    private ControlsBar controls;
    private BorderPane root;
    private BorderPane main;
    private VBox bottom;
    private StackPane centerStack;
    private HBox fsBar;
    private final PauseTransition fsHide = new PauseTransition(Duration.seconds(3));
    private FadeTransition fsFade;
    private MediaItem loaded;
    private String stylesheet;
    private boolean reachedReady;
    private final Set<String> transcoding = ConcurrentHashMap.newKeySet();

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        Branding.loadFonts();
        recents.load();
        metadata.enqueueAll(recents.getRecents());
        stylesheet = Objects.requireNonNull(getClass().getResource("resources/app.css")).toExternalForm();

        sidebar = new Sidebar(this::openFiles, this::openFolder);
        topBar = new TopBar(query, this::openFiles, this::openFolder);
        home = new HomeView(recents, query, this::openFiles, this::openFolder, this::playRecent);
        queue = new QueueView(playlist, query, this::openFiles);
        mediaPane = new MediaPane(this::openFiles, this::toggleFullscreen, player);
        controls = new ControlsBar(player, shuffle, repeat, this::previous, this::next, this::toggleFullscreen);

        StackPane views = new StackPane(home, mediaPane, queue);
        views.getStyleClass().add("content");
        views.setMinSize(0, 0);

        // Full-screen visualizer overlay: floating neumorphic control pill.
        fsBar = buildFullscreenBar();
        centerStack = new StackPane(views, fsBar);
        StackPane.setAlignment(fsBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(fsBar, new Insets(0, 0, 48, 0));

        bottom = new VBox(controls);
        // The sidebar owns the full window height; the top bar sits above the
        // content area only.
        main = new BorderPane(centerStack);
        main.setTop(topBar);
        root = new BorderPane();
        root.getStyleClass().add("app-root");
        root.setCenter(main);
        root.setBottom(bottom);
        root.setLeft(sidebar);

        sidebar.currentViewProperty().addListener((obs, old, v) -> showView(v));
        showView(Sidebar.View.HOME);

        query.addListener((obs, old, q) -> {
            if (!q.isBlank() && sidebar.currentViewProperty().get() == Sidebar.View.NOW_PLAYING) {
                sidebar.setView(Sidebar.View.HOME);
            }
        });

        playlist.currentIndexProperty().addListener((obs, old, idx) -> onSelectionChanged());
        player.setOnEndOfMedia(this::onEndOfMedia);
        player.setOnError(this::onPlayerError);
        player.statusProperty().addListener((obs, old, st) -> {
            if (st == MediaPlayer.Status.READY || st == MediaPlayer.Status.PLAYING
                    || st == MediaPlayer.Status.PAUSED || st == MediaPlayer.Status.STALLED) {
                reachedReady = true;
            }
        });

        Scene scene = new Scene(root, 1100, 680);
        scene.getStylesheets().add(stylesheet);
        scene.widthProperty().addListener((obs, old, w) -> applyLayout(w.doubleValue()));
        applyLayout(scene.getWidth());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (stage.isFullScreen()) showFullscreenBar();
        });
        scene.setOnDragOver(this::onDragOver);
        scene.setOnDragDropped(this::onDragDropped);

        fsHide.setOnFinished(e -> fadeFullscreenBar(false));

        stage.fullScreenProperty().addListener((obs, old, fs) -> {
            applyLayout(scene.getWidth());
            // Eased transition in/out of the immersive visualizer mode.
            FadeTransition t = new FadeTransition(Duration.millis(450), centerStack);
            t.setFromValue(fs ? 0.3 : 0.6);
            t.setToValue(1);
            t.play();
            if (fs) {
                showFullscreenBar();
            } else {
                hideFullscreenBar();
            }
        });
        stage.setFullScreenExitHint("Press Esc to exit full screen");
        stage.getIcons().addAll(Branding.appIcons());
        stage.setTitle("Swift");
        stage.setMinWidth(480);
        stage.setMinHeight(420);
        stage.setScene(scene);
        new Splash(stylesheet).show(stage::show);
    }

    @Override
    public void stop() {
        player.unload();
    }

    private void applyLayout(double width) {
        boolean fs = stage != null && stage.isFullScreen();
        HBoxTabs tabs = HBoxTabs.of(sidebar, bottom, controls);
        if (fs) {
            root.setLeft(null);
            main.setTop(null);
            root.setBottom(null);
            tabs.hide();
            sidebar.setView(Sidebar.View.NOW_PLAYING);
            return;
        }
        main.setTop(topBar);
        root.setBottom(bottom);
        if (width >= RAIL_MIN) {
            sidebar.setMode(width >= FULL_SIDEBAR_MIN ? Sidebar.Mode.FULL : Sidebar.Mode.RAIL);
            root.setLeft(sidebar);
            tabs.hide();
        } else {
            root.setLeft(null);
            tabs.show();
        }
    }

    /** Floating neumorphic pill shown over the full-screen visualizer. */
    private HBox buildFullscreenBar() {
        Button prev = overlayButton(Icons.PREVIOUS, "Previous (P)", this::previous);
        Button stop = overlayButton(Icons.STOP, "Stop (S)", player::stop);
        Button play = overlayButton(Icons.PLAY, "Play / Pause (Space)", player::playPause);
        player.playingProperty().addListener((obs, o, p) ->
                play.setGraphic(Icons.of(p ? Icons.PAUSE : Icons.PLAY, 22)));
        Button next = overlayButton(Icons.NEXT, "Next (N)", this::next);
        Button exit = overlayButton(Icons.CLOSE, "Exit full screen (F)", this::toggleFullscreen);
        exit.getStyleClass().add("fs-exit");

        HBox bar = new HBox(14, prev, stop, play, next, exit);
        bar.getStyleClass().add("fs-overlay");
        bar.setAlignment(Pos.CENTER);
        bar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        bar.setOpacity(0);
        bar.setVisible(false);
        bar.setManaged(false);
        // Keep the pill visible while the pointer rests on it.
        bar.setOnMouseEntered(e -> fsHide.stop());
        bar.setOnMouseExited(e -> {
            if (stage.isFullScreen()) fsHide.playFromStart();
        });
        return bar;
    }

    private Button overlayButton(String icon, String tip, Runnable action) {
        Button b = new Button();
        b.setGraphic(Icons.of(icon, 20));
        b.getStyleClass().add("fs-button");
        b.setTooltip(new Tooltip(tip));
        b.setAccessibleText(tip);
        b.setOnAction(e -> action.run());
        return b;
    }

    private void showFullscreenBar() {
        if (fsBar == null || !stage.isFullScreen()) return;
        fsHide.stop();
        if (!fsBar.isVisible()) {
            fsBar.setVisible(true);
            fsBar.setManaged(true);
            fsBar.toFront();
            fadeFullscreenBar(true);
        }
        fsHide.playFromStart();
    }

    private void hideFullscreenBar() {
        fsHide.stop();
        if (fsFade != null) fsFade.stop();
        fsBar.setOpacity(0);
        fsBar.setVisible(false);
        fsBar.setManaged(false);
    }

    private void fadeFullscreenBar(boolean show) {
        if (fsFade != null) fsFade.stop();
        fsFade = new FadeTransition(Duration.millis(350), fsBar);
        fsFade.setToValue(show ? 1 : 0);
        if (!show) {
            fsFade.setOnFinished(e -> {
                fsBar.setVisible(false);
                fsBar.setManaged(false);
            });
        }
        fsFade.play();
    }

    private record HBoxTabs(Sidebar sidebar, VBox bottom, ControlsBar controls) {
        static HBoxTabs of(Sidebar s, VBox b, ControlsBar c) {
            return new HBoxTabs(s, b, c);
        }

        void show() {
            var tabBar = sidebar.getTabBar();
            tabBar.setVisible(true);
            tabBar.setManaged(true);
            if (!bottom.getChildren().contains(tabBar)) bottom.getChildren().add(tabBar);
        }

        void hide() {
            var tabBar = sidebar.getTabBar();
            tabBar.setVisible(false);
            tabBar.setManaged(false);
            bottom.getChildren().remove(tabBar);
        }
    }

    private void showView(Sidebar.View view) {
        setShown(home, view == Sidebar.View.HOME);
        setShown(mediaPane, view == Sidebar.View.NOW_PLAYING);
        setShown(queue, view == Sidebar.View.QUEUE);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }

    private void toggleFullscreen() {
        stage.setFullScreen(!stage.isFullScreen());
    }

    private void openFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open media files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Media",
                        "*.mp3", "*.wav", "*.aac", "*.m4a", "*.flac", "*.ogg", "*.aiff", "*.wma",
                        "*.mp4", "*.m4v", "*.mov", "*.avi", "*.mkv", "*.wmv", "*.flv", "*.webm",
                        "*.mpg", "*.mpeg"),
                new FileChooser.ExtensionFilter("Audio",
                        "*.mp3", "*.wav", "*.aac", "*.m4a", "*.flac", "*.ogg", "*.aiff", "*.wma"),
                new FileChooser.ExtensionFilter("Video",
                        "*.mp4", "*.m4v", "*.mov", "*.avi", "*.mkv", "*.wmv", "*.flv", "*.webm",
                        "*.mpg", "*.mpeg"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files != null) addFiles(files);
    }

    private void openFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Open media folder");
        File dir = chooser.showDialog(stage);
        if (dir != null) addFiles(List.of(dir));
    }

    private void addFiles(List<File> files) {
        List<File> expanded = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory()) {
                File[] children = f.listFiles(c -> c.isFile() && MediaItem.isSupportedName(c.getName()));
                if (children != null) {
                    List<File> sorted = new ArrayList<>(List.of(children));
                    sorted.sort(Comparator.comparing(c -> c.getName().toLowerCase()));
                    expanded.addAll(sorted);
                }
            } else if (f.isFile()) {
                expanded.add(f);
            }
        }

        List<MediaItem> fresh = new ArrayList<>();
        MediaItem first = null;
        for (File f : expanded) {
            MediaItem item = MediaItem.fromFile(f);
            MediaItem existing = findInQueue(item.toUri());
            if (existing == null) {
                fresh.add(item);
                existing = item;
            }
            if (first == null) first = existing;
        }
        if (first == null) return;

        metadata.enqueueAll(fresh);
        playlist.getItems().addAll(fresh);
        int index = playlist.getItems().indexOf(first);
        if (index == playlist.getCurrentIndex()) {
            restart();
        } else {
            playlist.select(index);
        }
        sidebar.setView(Sidebar.View.NOW_PLAYING);
    }

    private MediaItem findInQueue(String uri) {
        for (MediaItem i : playlist.getItems()) {
            if (i.toUri().equals(uri)) return i;
        }
        return null;
    }

    private void playRecent(MediaItem item) {
        metadata.enqueue(item);
        MediaItem inQueue = findInQueue(item.toUri());
        if (inQueue == null) {
            playlist.getItems().add(item);
            inQueue = item;
        }
        int index = playlist.getItems().indexOf(inQueue);
        if (index == playlist.getCurrentIndex()) {
            restart();
        } else {
            playlist.select(index);
        }
        sidebar.setView(Sidebar.View.NOW_PLAYING);
    }

    private void onSelectionChanged() {
        MediaItem item = playlist.getCurrentItem();
        if (item == null) {
            loaded = null;
            player.unload();
            mediaPane.showEmpty();
            controls.showItem(null);
            return;
        }
        if (item == loaded) return;
        loadAndPlay(item);
    }

    private void loadAndPlay(MediaItem item) {
        if (!item.fileExists()) {
            showError("File not found:\n" + item.getName());
            recents.remove(item);
            loaded = null;
            playlist.remove(item);
            return;
        }
        loaded = item;
        recents.add(item);
        reachedReady = false;
        if (player.load(item)) {
            mediaPane.showMedia(item);
            controls.showItem(item);
        }
    }

    /** Media that never reached READY gets one chance at an ffmpeg conversion. */
    private void onPlayerError(String message) {
        MediaItem item = loaded;
        if (item != null && !reachedReady && !item.hasPlaybackOverride()
                && item.getFile() != null && item.getFile().isFile()
                && Transcoder.isAvailable()) {
            transcodeAndPlay(item);
        } else {
            showError(message);
        }
    }

    private void transcodeAndPlay(MediaItem item) {
        if (!transcoding.add(item.toUri())) return;
        mediaPane.showMedia(item);
        controls.showItem(item);
        boolean audioOnly = item.getType() == MediaItem.Type.AUDIO;
        Transcoder.transcodeAsync(item.getFile(), audioOnly,
                converted -> Platform.runLater(() -> {
                    transcoding.remove(item.toUri());
                    item.setPlaybackUri(converted.toURI().toString());
                    if (item == loaded) {
                        reachedReady = false;
                        if (player.load(item)) {
                            mediaPane.showMedia(item);
                            controls.showItem(item);
                        }
                    }
                }),
                error -> Platform.runLater(() -> {
                    transcoding.remove(item.toUri());
                    if (item == loaded) {
                        showError("This media format is not supported:\n" + item.getName());
                    }
                }));
    }

    private void restart() {
        player.seekTo(Duration.ZERO);
        player.play();
    }

    private void next() {
        int size = playlist.getItems().size();
        if (size == 0) return;
        if (shuffle.get() && size > 1) {
            int current = playlist.getCurrentIndex();
            int pick;
            do {
                pick = random.nextInt(size);
            } while (pick == current);
            playlist.select(pick);
        } else if (playlist.hasNext()) {
            playlist.next();
        } else if (repeat.get() == RepeatMode.ALL) {
            if (size == 1) restart(); else playlist.select(0);
        }
    }

    private void previous() {
        if (player.currentTimeProperty().get().greaterThan(Duration.seconds(3))) {
            player.seekTo(Duration.ZERO);
            return;
        }
        int size = playlist.getItems().size();
        if (playlist.hasPrevious()) {
            playlist.previous();
        } else if (repeat.get() == RepeatMode.ALL && size > 1) {
            playlist.select(size - 1);
        } else {
            player.seekTo(Duration.ZERO);
        }
    }

    private void onEndOfMedia() {
        if (repeat.get() == RepeatMode.ONE) {
            restart();
        } else if (playlist.hasNext() || repeat.get() == RepeatMode.ALL || shuffle.get()) {
            next();
        } else {
            player.stop();
        }
    }

    private void onKey(KeyEvent e) {
        if (stage.getScene().getFocusOwner() instanceof TextInputControl) return;
        KeyCode code = e.getCode();
        boolean handled = true;
        switch (code) {
            case SPACE -> player.playPause();
            case S -> player.stop();
            case N -> next();
            case P -> previous();
            case M -> player.toggleMute();
            case UP -> player.adjustVolume(0.05);
            case DOWN -> player.adjustVolume(-0.05);
            case LEFT -> player.seekBy(Duration.seconds(-5));
            case RIGHT -> player.seekBy(Duration.seconds(5));
            case F, F11 -> toggleFullscreen();
            default -> handled = false;
        }
        if (handled) e.consume();
    }

    private void onDragOver(DragEvent e) {
        if (e.getDragboard().hasFiles()) e.acceptTransferModes(TransferMode.COPY);
        e.consume();
    }

    private void onDragDropped(DragEvent e) {
        boolean ok = e.getDragboard().hasFiles();
        if (ok) addFiles(e.getDragboard().getFiles());
        e.setDropCompleted(ok);
        e.consume();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Swift");
        alert.setHeaderText(null);
        alert.initOwner(stage);
        alert.getDialogPane().getStylesheets().add(stylesheet);
        alert.show();
    }
}
