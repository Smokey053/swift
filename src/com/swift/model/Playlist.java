package com.swift.model;

import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Playlist {

    private final ObservableList<MediaItem> items = FXCollections.observableArrayList();
    private final ReadOnlyIntegerWrapper currentIndex = new ReadOnlyIntegerWrapper(-1);

    public ObservableList<MediaItem> getItems() {
        return items;
    }

    public ReadOnlyIntegerProperty currentIndexProperty() {
        return currentIndex.getReadOnlyProperty();
    }

    public int getCurrentIndex() {
        return currentIndex.get();
    }

    public MediaItem getCurrentItem() {
        int idx = currentIndex.get();
        return (idx >= 0 && idx < items.size()) ? items.get(idx) : null;
    }

    public void remove(MediaItem item) {
        int idx = items.indexOf(item);
        if (idx < 0) {
            return;
        }
        items.remove(idx);
        if (items.isEmpty()) {
            currentIndex.set(-1);
        } else if (idx < currentIndex.get()) {
            currentIndex.set(currentIndex.get() - 1);
        } else if (idx == currentIndex.get()) {
            currentIndex.set(-1);
        }
    }

    public void clear() {
        items.clear();
        currentIndex.set(-1);
    }

    public void select(int index) {
        if (index >= 0 && index < items.size() && index != currentIndex.get()) {
            currentIndex.set(index);
        }
    }

    public boolean hasPrevious() {
        return currentIndex.get() > 0;
    }

    public boolean hasNext() {
        return currentIndex.get() >= 0 && currentIndex.get() < items.size() - 1;
    }

    public void previous() {
        if (hasPrevious()) {
            select(currentIndex.get() - 1);
        }
    }

    public void next() {
        if (hasNext()) {
            select(currentIndex.get() + 1);
        }
    }
}
