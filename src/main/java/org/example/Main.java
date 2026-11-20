package org.example;

import org.example.model.*;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Board board = new Board(8, 8);

        // Стены
        board.set(new Position(3, 1), CellType.WALL);
        board.set(new Position(3, 2), CellType.WALL);
        board.set(new Position(3, 3), CellType.WALL);

        // Укрытия
        board.set(new Position(1, 4), CellType.COVER);
        board.set(new Position(6, 4), CellType.COVER);

        Tank light = new LightTank("L", new Position(0, 2));
        Tank heavy = new HeavyTank("H", new Position(7, 2));

        Game game = new Game(board, light, heavy, 42L);

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== ТАНКОВЫЙ БОЙ ===");
        System.out.println("L - лёгкий танк, H - тяжёлый танк");
        System.out.println("# - стена, + - укрытие, . - пусто");
        System.out.println("Цель: уничтожить противника.");

        while (!game.isFinished()) {

            printBoard(game);

            Tank current = game.getCurrentTank();

            System.out.println();
            System.out.println("Ход танка " + current.getId()
                    + " (" + current.getModel() + ")");
            System.out.println("HP: " + current.getHealth()
                    + " | Патроны: " + current.getAmmo()
                    + " | Скорость: " + current.getSpeed());

            System.out.println(
                    "Команда: dx dy цель зона");
            System.out.println(
                    "Пример выстрела: 0 0 H TURRET");
            System.out.println(
                    "Пример движения: 0 2 -");
            System.out.println(
                    "Зоны: HULL, TURRET, TRACKS");
            System.out.println(
                    "Для выхода введи exit");

            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("exit")) {
                break;
            }

            String[] parts = line.split("\\s+");

            try {
                if (parts.length != 3 && parts.length != 4) {
                    throw new IllegalArgumentException(
                            "Неверный формат команды");
                }

                int dx = Integer.parseInt(parts[0]);
                int dy = Integer.parseInt(parts[1]);

                String targetId = null;
                HitZone zone = null;

                if (parts[2].equals("-")) {
                    if (parts.length != 3) {
                        throw new IllegalArgumentException(
                                "Для пропуска выстрела укажи -");
                    }
                } else {
                    if (parts.length != 4) {
                        throw new IllegalArgumentException(
                                "Нужно указать зону попадания");
                    }

                    targetId = parts[2];
                    zone = HitZone.valueOf(
                            parts[3].toUpperCase(Locale.ROOT));
                }

                List<GameEvent> events =
                        game.takeTurn(dx, dy, targetId, zone);

                for (GameEvent event : events) {
                    System.out.println(event.message());
                }

            } catch (IllegalArgumentException
                     | IllegalStateException e) {

                System.out.println("Ошибка: " + e.getMessage());
            }
        }

        if (game.isFinished()) {
            printBoard(game);

            if (game.getWinnerId() != null) {
                System.out.println(
                        "Победил танк " + game.getWinnerId());
            } else {
                System.out.println("Бой закончился ничьей");
            }
        }

        System.out.println("Игра завершена.");
    }

    private static void printBoard(Game game) {

        Board board = game.getBoard();

        System.out.println();
        System.out.print("   ");

        for (int x = 0; x < board.getWidth(); x++) {
            System.out.printf("%2d", x);
        }

        System.out.println();

        for (int y = 0; y < board.getHeight(); y++) {

            System.out.printf("%2d ", y);

            for (int x = 0; x < board.getWidth(); x++) {

                Position position = new Position(x, y);

                char symbol = switch (board.get(position)) {
                    case EMPTY -> '.';
                    case WALL -> '#';
                    case COVER -> '+';
                };

                for (Tank tank : game.getTanks()) {
                    if (tank.isAlive()
                            && tank.getPosition().equals(position)) {
                        symbol = tank instanceof LightTank
                                ? 'L' : 'H';
                    }
                }

                System.out.printf("%2c", symbol);
            }

            System.out.println();
        }
    }
}
