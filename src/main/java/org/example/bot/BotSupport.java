package org.example.bot;

import org.example.model.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/** Общий поиск пути к клетке для стрельбы. */
final class BotSupport {

    private BotSupport() {
    }

    static TurnAction choose(
            Game game,
            HitZone zone,
            int tactic
    ) {

        Tank self = game.getCurrentTank();

        Tank enemy = game.getTanks().stream()
                .filter(t -> t != self && t.isAlive())
                .findFirst()
                .orElseThrow();

        Board board = game.getBoard();

        Position start = self.getPosition();
        Position target = enemy.getPosition();

        // Если противник уже виден, стреляем.
        if (visible(board, start, target)) {
            return TurnAction.shoot(enemy.getId(), zone);
        }

        if (self.getSpeed() == 0) {
            return TurnAction.move(0, 0);
        }

        // Направления движения.
        int[][] directions = {
                {0, 1},
                {1, 0},
                {0, -1},
                {-1, 0}
        };

        Queue<Position> queue = new ArrayDeque<>();

        Map<Position, Position> previous = new HashMap<>();
        Map<Position, Integer> distances = new HashMap<>();

        queue.add(start);
        previous.put(start, null);
        distances.put(start, 0);

        // Поиск в ширину.
        while (!queue.isEmpty()) {

            Position p = queue.remove();

            for (int[] d : directions) {

                Position next = p.add(d[0], d[1]);

                if (!board.contains(next)
                        || board.get(next) == CellType.WALL
                        || next.equals(target)
                        || previous.containsKey(next)) {

                    continue;
                }

                previous.put(next, p);
                distances.put(
                        next,
                        distances.get(p) + 1
                );

                queue.add(next);
            }
        }

        Position goal = null;
        int best = Integer.MAX_VALUE;

        // Выбираем подходящую огневую позицию.
        for (Position p : distances.keySet()) {

            if (!visible(board, p, target)) {
                continue;
            }

            int distance = distances.get(p);

            int enemyDistance =
                    Math.abs(p.x() - target.x())
                            + Math.abs(p.y() - target.y());

            int score = switch (tactic) {

                // Агрессивный: ближе к противнику.
                case 1 -> distance * 100 + enemyDistance;

                // Оборонительный: предпочитает укрытия.
                case 2 -> distance * 100
                        - (board.get(p) == CellType.COVER ? 35 : 0);

                // Снайпер: предпочитает дальнюю позицию.
                default -> distance * 100 - enemyDistance;
            };

            if (score < best
                    || (score == best && compare(p, goal) < 0)) {

                best = score;
                goal = p;
            }
        }

        if (goal == null) {
            return TurnAction.move(0, 0);
        }

        // Восстанавливаем найденный путь.
        List<Position> path = new ArrayList<>();

        for (Position p = goal;
             p != null;
             p = previous.get(p)) {

            path.add(p);
        }

        Collections.reverse(path);

        int dxDirection = Integer.compare(
                path.get(1).x(), start.x());

        int dyDirection = Integer.compare(
                path.get(1).y(), start.y());

        Position destination = start;

        // Движение по прямой в пределах скорости.
        for (int i = 1;
             i < path.size() && i <= self.getSpeed();
             i++) {

            Position next = path.get(i);

            Position expected = destination.add(
                    dxDirection, dyDirection);

            if (!next.equals(expected)) {
                break;
            }

            destination = next;
        }

        int dx = destination.x() - start.x();
        int dy = destination.y() - start.y();

        // Если после перемещения можно стрелять,
        // выполняем перемещение и выстрел за ход.
        if (visible(board, destination, target)) {

            return new TurnAction(
                    dx,
                    dy,
                    enemy.getId(),
                    zone
            );
        }

        return TurnAction.move(dx, dy);
    }

    private static int compare(Position a, Position b) {

        if (b == null) {
            return -1;
        }

        int x = Integer.compare(a.x(), b.x());

        return x == 0
                ? Integer.compare(a.y(), b.y())
                : x;
    }

    /** Проверяет линию огня с учётом стен. */
    private static boolean visible(
            Board board,
            Position from,
            Position to
    ) {

        if (from.x() != to.x()
                && from.y() != to.y()) {

            return false;
        }

        int dx = Integer.compare(to.x(), from.x());
        int dy = Integer.compare(to.y(), from.y());

        for (Position p = from.add(dx, dy);
             !p.equals(to);
             p = p.add(dx, dy)) {

            if (board.get(p) == CellType.WALL) {
                return false;
            }
        }

        return true;
    }
}
