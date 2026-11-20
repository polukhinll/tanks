package org.example.model;

/** Базовый класс боевой техники. */
public abstract class Tank {

    private final String id;
    private Position position;

    private int health;
    private final int armor;
    private int ammo;
    private int speed;

    protected Tank(String id, Position position,
                   int health, int armor,
                   int ammo, int speed) {

        if (id == null || id.isBlank()
                || position == null
                || health <= 0 || armor < 0
                || ammo < 0 || speed < 0) {
            throw new IllegalArgumentException(
                    "Неверные параметры танка");
        }

        this.id = id;
        this.position = position;
        this.health = health;
        this.armor = armor;
        this.ammo = ammo;
        this.speed = speed;
    }

    /** Сила выстрела зависит от типа техники. */
    public abstract int getShotPower();

    public abstract String getModel();

    public String getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public int getHealth() {
        return health;
    }

    public int getArmor() {
        return armor;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean isAlive() {
        return health > 0;
    }

    void moveTo(Position newPosition) {
        this.position = newPosition;
    }

    void spendAmmo() {
        if (ammo <= 0) {
            throw new IllegalStateException("Нет патронов");
        }
        ammo--;
    }

    /** Применяет повреждение и возвращает нанесённый урон. */
    int takeDamage(int rawDamage, HitZone zone, boolean cover) {

        int damage = rawDamage
                + zone.getDamageBonus()
                - armor;

        if (cover) {
            damage -= 3;
        }

        damage = Math.max(1, damage);
        health = Math.max(0, health - damage);

        if (zone == HitZone.TRACKS && speed > 0) {
            speed--;
        }

        return damage;
    }
}

