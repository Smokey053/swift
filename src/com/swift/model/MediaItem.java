package com.swift.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Set;

public final class MediaItem {

    public enum Type { AUDIO, VIDEO, STREAM, UNKNOWN }

    private static final Set<String> AUDIO_EXT =
            Set.of("mp3", "wav", "aac", "m4a", "flac", "ogg", "aiff", "wma");
    private static final Set<String> VIDEO_EXT =
            Set.of("mp4", "m4v", "mov", "avi", "mkv", "wmv", "flv", "webm", "mpg", "mpeg");

    private final String uri;
    private final String name;
    private final File file;

    private final StringProperty title = new SimpleStringProperty(null);
    private final StringProperty artist = new SimpleStringProperty(null);
    private final ObjectProperty<Image> artwork = new SimpleObjectProperty<>(null);
    private boolean metadataResolved;
    private volatile String playbackUri;

    private MediaItem(String uri, String name, File file) {
        this.uri = uri;
        this.name = name;
        this.file = file;
    }

    public static MediaItem fromFile(File file) {
        return new MediaItem(file.toURI().toString(), stripExtension(file.getName()), file);
    }

    public static MediaItem fromUrl(String url) {
        String clean = url;
        int query = clean.indexOf('?');
        if (query >= 0) clean = clean.substring(0, query);
        int slash = clean.lastIndexOf('/');
        String tail = slash >= 0 ? clean.substring(slash + 1) : clean;
        if (tail.isBlank()) tail = url;
        return new MediaItem(url, tail, null);
    }

    public static MediaItem of(String uri, String name) {
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return new MediaItem(uri, name, null);
        }
        try {
            return new MediaItem(uri, name, new File(URI.create(uri)));
        } catch (IllegalArgumentException e) {
            return new MediaItem(uri, name, new File(name));
        }
    }

    public static boolean isSupportedName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return false;
        String ext = fileName.substring(dot + 1).toLowerCase();
        return AUDIO_EXT.contains(ext) || VIDEO_EXT.contains(ext);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    public File getFile() {
        return file;
    }

    public boolean isStream() {
        return file == null;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        // For local files, always read the real file name: the display name has
        // its extension stripped and may itself contain dots ("my.movie.v2").
        String base = file != null ? file.getName() : name;
        int dot = base.lastIndexOf('.');
        String source = dot > 0 ? base.substring(dot + 1) : "";
        if (source.isEmpty()) {
            String clean = uri;
            int query = clean.indexOf('?');
            if (query >= 0) clean = clean.substring(0, query);
            dot = clean.lastIndexOf('.');
            source = dot > 0 ? clean.substring(dot + 1) : "";
        }
        return source.toLowerCase();
    }

    public Type getType() {
        if (isStream()) return Type.STREAM;
        String ext = getExtension();
        if (AUDIO_EXT.contains(ext)) return Type.AUDIO;
        if (VIDEO_EXT.contains(ext)) return Type.VIDEO;
        return Type.UNKNOWN;
    }

    public String getTypeLabel() {
        return switch (getType()) {
            case AUDIO -> "Audio";
            case VIDEO -> "Video";
            case STREAM -> "Stream";
            case UNKNOWN -> "Media";
        };
    }

    public boolean fileExists() {
        return isStream() || (file != null && file.isFile());
    }

    /** Applies tags parsed from the media's metadata (ID3 etc.). */
    public void applyMetadata(Map<? extends String, ?> metadata) {
        if (metadata == null || metadata.isEmpty()) return;
        String t = text(metadata.get("title"));
        if (t != null) title.set(t);
        String a = text(metadata.get("artist"));
        if (a == null) a = text(metadata.get("album artist"));
        if (a != null) artist.set(a);
        Object image = metadata.get("image");
        if (image instanceof Image img && !img.isError()) artwork.set(img);
    }

    private static String text(Object value) {
        if (value == null) return null;
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** Metadata title if present, otherwise the plain file name. */
    public String getDisplayName() {
        String t = title.get();
        return t != null ? t : name;
    }

    public String getArtist() {
        return artist.get();
    }

    public Image getArtwork() {
        return artwork.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public StringProperty artistProperty() {
        return artist;
    }

    public ObjectProperty<Image> artworkProperty() {
        return artwork;
    }

    public boolean isMetadataResolved() {
        return metadataResolved;
    }

    public void setMetadataResolved(boolean resolved) {
        this.metadataResolved = resolved;
    }

    public String toUri() {
        return uri;
    }

    /** URI handed to the player; differs from {@link #toUri()} after a transcoding fallback. */
    public String getPlaybackUri() {
        String override = playbackUri;
        return override != null ? override : uri;
    }

    public boolean hasPlaybackOverride() {
        return playbackUri != null;
    }

    public void setPlaybackUri(String playbackUri) {
        this.playbackUri = playbackUri;
    }

    @Override
    public String toString() {
        return name;
    }
}
