package org.example.model;

/** Координаты клетки на игровом поле. */
public record Position(int x, int y) {

    public Position add(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }
}