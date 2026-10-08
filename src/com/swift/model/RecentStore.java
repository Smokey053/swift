package com.swift.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RecentStore {

    private static final int MAX_ENTRIES = 24;
    private static final Path STORE =
            Paths.get(System.getProperty("user.home"), ".swift", "recent.tsv");

    private final ObservableList<MediaItem> recents = FXCollections.observableArrayList();

    public ObservableList<MediaItem> getRecents() {
        return recents;
    }

    public void load() {
        recents.clear();
        if (!Files.isRegularFile(STORE)) return;
        try {
            for (String line : Files.readAllLines(STORE, StandardCharsets.UTF_8)) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\t", 2);
                recents.add(parts.length == 2
                        ? MediaItem.of(parts[0], parts[1])
                        : MediaItem.of(parts[0], parts[0]));
            }
        } catch (IOException ignored) {
        }
    }

    public void add(MediaItem item) {
        recents.removeIf(r -> r.toUri().equals(item.toUri()));
        recents.add(0, item);
        while (recents.size() > MAX_ENTRIES) {
            recents.remove(recents.size() - 1);
        }
        save();
    }

    public void remove(MediaItem item) {
        recents.removeIf(r -> r.toUri().equals(item.toUri()));
        save();
    }

    public void clear() {
        recents.clear();
        save();
    }

    private void save() {
        try {
            Files.createDirectories(STORE.getParent());
            List<String> lines = new ArrayList<>();
            for (MediaItem item : recents) {
                String safeName = item.getName().replace('\t', ' ').replace('\n', ' ');
                lines.add(item.toUri() + "\t" + safeName);
            }
            Files.write(STORE, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
