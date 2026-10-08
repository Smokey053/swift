package com.swift.util;

import javafx.util.Duration;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String format(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite() || d.lessThan(Duration.ZERO)) {
            return "--:--";
        }
        long totalSeconds = (long) Math.floor(d.toSeconds());
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }
}
