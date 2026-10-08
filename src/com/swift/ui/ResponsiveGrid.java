package com.swift.ui;

import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;

public class ResponsiveGrid extends GridPane {

    private final double minCell;
    private final int maxCols;
    private List<Node> nodes = new ArrayList<>();
    private int cols = 0;

    public ResponsiveGrid(double gap, double minCell, int maxCols) {
        this.minCell = minCell;
        this.maxCols = maxCols;
        setHgap(gap);
        setVgap(gap);
        setMinWidth(0);
        widthProperty().addListener((obs, old, w) -> relayout(false));
    }

    public void setNodes(List<? extends Node> newNodes) {
        nodes = new ArrayList<>(newNodes);
        relayout(true);
    }

    private void relayout(boolean force) {
        double w = getWidth();
        int c = w <= 0 ? 1 : (int) Math.max(1, Math.min(maxCols, Math.floor((w + getHgap()) / (minCell + getHgap()))));
        if (!force && c == cols) return;
        cols = c;

        getChildren().clear();
        getColumnConstraints().clear();
        for (int i = 0; i < c; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / c);
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(0);
            getColumnConstraints().add(cc);
        }
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            GridPane.setFillWidth(node, true);
            add(node, i % c, i / c);
        }
    }
}
