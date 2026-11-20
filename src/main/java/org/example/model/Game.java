package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Управляет пошаговым боем двух танков. */
public class Game {

    private final Board board;
    private final List<Tank> tanks;
    private final List<GameEvent> history = new ArrayList<>();
    private final Random random;

    private int currentIndex = 0;
    private boolean finished = false;
    private String winnerId;

    public Game(Board board, Tank first, Tank second, long seed) {
        if (board == null || first == null || second == null) {
            throw new IllegalArgumentException("Нет поля или танка");
        }

        if (first.getId().equals(second.getId())) {
            throw new IllegalArgumentException(
                    "Идентификаторы должны отличаться");
        }

        if (!board.contains(first.getPosition())
                || !board.contains(second.getPosition())
                || board.get(first.getPosition()) == CellType.WALL
                || board.get(second.getPosition()) == CellType.WALL
                || first.getPosition().equals(second.getPosition())) {
            throw new IllegalArgumentException(
                    "Недопустимые начальные позиции");
        }

        this.board = board;
        this.tanks = List.of(first, second);
        this.random = new Random(seed);
    }

    public Board getBoard() {
        return board;
    }

    public List<Tank> getTanks() {
        return List.copyOf(tanks);
    }

    public Tank getCurrentTank() {
        return tanks.get(currentIndex);
    }

    public List<GameEvent> getHistory() {
        return List.copyOf(history);
    }

    public boolean isFinished() {
        return finished;
    }

    public String getWinnerId() {
        return winnerId;
    }

    /**
     * Выполняет ход текущего танка.
     * dx, dy - перемещение.
     * targetId - цель выстрела или null.
     * zone - зона попадания или null.
     */
    public List<GameEvent> takeTurn(
            int dx, int dy, String targetId, HitZone zone) {

        if (finished) {
            throw new IllegalStateException("Бой завершён");
        }

        if ((targetId == null) != (zone == null)) {
            throw new IllegalArgumentException(
                    "Нужно указать цель и зону вместе");
        }

        Tank attacker = getCurrentTank();

        // Сначала проверяем ход, затем меняем состояние.
        Position destination = validateMovement(attacker, dx, dy);

        Tank target = null;

        if (targetId != null) {
            target = findTank(targetId);

            if (target == attacker || !target.isAlive()) {
                throw new IllegalArgumentException(
                        "Недопустимая цель");
            }

            if (attacker.getAmmo() <= 0) {
                throw new IllegalArgumentException(
                        "У танка закончились патроны");
            }

            if (!canShoot(destination, target.getPosition())) {
                throw new IllegalArgumentException(
                        "Нет прямой линии огня");
            }
        }

        List<GameEvent> events = new ArrayList<>();

        if (!destination.equals(attacker.getPosition())) {
            attacker.moveTo(destination);

            events.add(new GameEvent(
                    attacker.getId() + " переместился в "
                            + destination.x() + ","
                            + destination.y()));
        } else {
            events.add(new GameEvent(
                    attacker.getId() + " остался на месте"));
        }

        if (target != null) {
            attacker.spendAmmo();

            // Случайный разброс урона от -1 до +1.
            int power = attacker.getShotPower()
                    + random.nextInt(3) - 1;

            boolean cover = board.get(target.getPosition())
                    == CellType.COVER;

            int damage = target.takeDamage(power, zone, cover);

            events.add(new GameEvent(
                    attacker.getId() + " выстрелил по "
                            + target.getId()
                            + " (" + zone + "), урон: " + damage));

            if (!target.isAlive()) {
                finished = true;
                winnerId = attacker.getId();

                events.add(new GameEvent(
                        "Победитель: " + winnerId));
            }
        }

        if (!finished
                && tanks.stream().allMatch(t -> t.getAmmo() == 0)) {
            finished = true;
            events.add(new GameEvent(
                    "Ничья: у всех закончились патроны"));
        }

        if (!finished) {
            currentIndex = 1 - currentIndex;
        }

        history.addAll(events);
        return List.copyOf(events);
    }

    private Tank findTank(String id) {
        for (Tank tank : tanks) {
            if (tank.getId().equals(id)) {
                return tank;
            }
        }

        throw new IllegalArgumentException(
                "Танк не найден: " + id);
    }

    private Position validateMovement(
            Tank tank, int dx, int dy) {

        if (dx != 0 && dy != 0) {
            throw new IllegalArgumentException(
                    "По диагонали двигаться нельзя");
        }

        long steps = Math.abs((long) dx)
                + Math.abs((long) dy);

        if (steps > tank.getSpeed()) {
            throw new IllegalArgumentException(
                    "Превышена скорость танка");
        }

        int stepX = Integer.compare(dx, 0);
        int stepY = Integer.compare(dy, 0);

        Position position = tank.getPosition();

        for (int i = 0; i < steps; i++) {
            position = position.add(stepX, stepY);

            if (!board.contains(position)
                    || board.get(position) == CellType.WALL) {
                throw new IllegalArgumentException(
                        "Путь перекрыт стеной или границей");
            }

            for (Tank other : tanks) {
                if (other != tank
                        && other.isAlive()
                        && other.getPosition().equals(position)) {
                    throw new IllegalArgumentException(
                            "Клетка занята другим танком");
                }
            }
        }

        return position;
    }

    /**
     * Проверяет прямую линию огня.
     * Стрельба разрешена по одной строке или столбцу.
     */
    private boolean canShoot(Position from, Position to) {

        if (from.x() != to.x() && from.y() != to.y()) {
            return false;
        }

        int stepX = Integer.compare(to.x(), from.x());
        int stepY = Integer.compare(to.y(), from.y());

        Position position = from.add(stepX, stepY);

        while (!position.equals(to)) {
            if (board.get(position) == CellType.WALL) {
                return false;
            }

            position = position.add(stepX, stepY);
        }

        return true;
    }
}
