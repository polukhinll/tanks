package org.example.model;

public enum HitZone {

    HULL(0),      // корпус
    TURRET(3),    // башня
    TRACKS(-2);   // гусеницы

    private final int damageBonus;

    HitZone(int damageBonus) {
        this.damageBonus = damageBonus;
    }

    public int getDamageBonus() {
        return damageBonus;
    }
}
