package org.example.model;

import java.util.Arrays;
import java.util.Objects;

/** Игровое поле с препятствиями и укрытиями. */
public class Board {

    private final int width;
    private final int height;
    private final CellType[][] cells;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Размеры поля должны быть положительными");
        }

        this.width = width;
        this.height = height;
        cells = new CellType[height][width];

        for (CellType[] row : cells) {
            Arrays.fill(row, CellType.EMPTY);
        }
    }

    public boolean contains(Position position) {
        return position != null
                && position.x() >= 0
                && position.x() < width
                && position.y() >= 0
                && position.y() < height;
    }

    public CellType get(Position position) {
        if (!contains(position)) {
            throw new IllegalArgumentException(
                    "Клетка находится за пределами поля");
        }

        return cells[position.y()][position.x()];
    }

    public void set(Position position, CellType type) {
        Objects.requireNonNull(type);

        if (!contains(position)) {
            throw new IllegalArgumentException(
                    "Клетка находится за пределами поля");
        }

        cells[position.y()][position.x()] = type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}

