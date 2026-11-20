package org.example.model;

/** Одна команда: перемещение и, возможно, выстрел. */
public record TurnAction(
        int dx,
        int dy,
        String targetId,
        HitZone zone
) {

    public static TurnAction move(int dx, int dy) {
        return new TurnAction(dx, dy, null, null);
    }

    public static TurnAction shoot(String id, HitZone zone) {
        return new TurnAction(0, 0, id, zone);
    }
}

