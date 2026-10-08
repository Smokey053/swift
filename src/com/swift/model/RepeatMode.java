package com.swift.model;

public enum RepeatMode {
    OFF, ALL, ONE;

    public RepeatMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
