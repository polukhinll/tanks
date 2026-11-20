package org.example;

import org.example.bot.AggressiveBot;
import org.example.bot.DefensiveBot;
import org.example.model.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class Main {

    private static final Path SAVE_FILE =
            Path.of("battle.json");

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.println("\n=== ТАНКИ ===");
            System.out.println("1 - Играть вдвоём");
            System.out.println("2 - Бой двух ботов");
            System.out.println("3 - Турнир трёх стратегий");
            System.out.println("4 - Загрузить бой из JSON");
            System.out.println("0 - Выход");

            System.out.print("Выбор: ");

            if (!scanner.hasNextLine()) {
                return;
            }

            String mode = scanner.nextLine().trim();

            try {

                switch (mode) {

                    case "1" -> humanGame(scanner);

                    case "2" -> botGame();

                    case "3" -> printTournament();

                    case "4" -> replay();

                    case "0" -> {
                        return;
                    }

                    default -> System.out.println(
                            "Нет такого пункта");
                }

            } catch (IOException | IllegalArgumentException e) {

                System.out.println(
                        "Ошибка: " + e.getMessage());
            }
        }
    }

    /** Игра двух людей через консоль. */
    private static void humanGame(
            Scanner scanner
    ) throws IOException {

        BattleSession session =
                Tournament.newBattle(42L);

        System.out.println(
                "Команда: dx dy -");

        System.out.println(
                "Или: dx dy ID HULL/TURRET/TRACKS");

        System.out.println(
                "Пример: 0 2 - или 0 0 B TURRET");

        System.out.println(
                "save - сохранить бой");

        System.out.println(
                "exit - выйти в меню");

        while (!session.getGame().isFinished()) {

            printBoard(session.getGame());

            Tank tank = session.getGame()
                    .getCurrentTank();

            System.out.printf(
                    "Ход %s | HP %d | Патроны %d | Скорость %d%n",
                    tank.getId(),
                    tank.getHealth(),
                    tank.getAmmo(),
                    tank.getSpeed()
            );

            System.out.print("> ");

            if (!scanner.hasNextLine()) {
                return;
            }

            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                return;
            }

            if (input.equalsIgnoreCase("save")) {

                session.save(SAVE_FILE);

                System.out.println(
                        "Сохранено: "
                                + SAVE_FILE.toAbsolutePath());

                continue;
            }

            try {

                String[] words = input.split("\\s+");

                if (words.length != 3
                        && words.length != 4) {

                    throw new IllegalArgumentException(
                            "Нужно 3 или 4 аргумента");
                }

                int dx = Integer.parseInt(words[0]);
                int dy = Integer.parseInt(words[1]);

                TurnAction action;

                if (words.length == 3
                        && words[2].equals("-")) {

                    action = TurnAction.move(dx, dy);

                } else if (words.length == 4) {

                    action = new TurnAction(
                            dx,
                            dy,
                            words[2],
                            HitZone.valueOf(
                                    words[3].toUpperCase(
                                            Locale.ROOT
                                    )
                            )
                    );

                } else {

                    throw new IllegalArgumentException(
                            "Укажи '-' или цель и зону");
                }

                session.play(action).forEach(
                        event -> System.out.println(
                                event.message()
                        )
                );

            } catch (IllegalArgumentException
                     | IllegalStateException e) {

                System.out.println(
                        "Недопустимый ход: " + e.getMessage());
            }
        }

        System.out.println(
                "Победитель: "
                        + session.getGame().getWinnerId());

        session.save(SAVE_FILE);

        System.out.println(
                "Бой сохранён в battle.json");
    }

    /** Автоматический бой двух ботов. */
    private static void botGame() throws IOException {

        BattleSession session = Tournament.battle(
                new AggressiveBot(),
                new DefensiveBot(),
                42L
        );

        printBoard(session.getGame());

        printHistory(session.getGame());

        session.save(SAVE_FILE);

        System.out.println(
                "Бой сохранён в battle.json");
    }

    /** Вывод турнирной таблицы. */
    private static void printTournament() {

        List<Tournament.Result> rows =
                Tournament.run(5, 42L);

        System.out.printf(
                "%-18s %6s %7s %6s %9s %10s%n",
                "Тактика",
                "Бои",
                "Победы",
                "Ничьи",
                "Поражения",
                "% побед"
        );

        for (Tournament.Result row : rows) {

            System.out.printf(
                    Locale.ROOT,
                    "%-18s %6d %7d %6d %9d %9.1f%%%n",
                    row.strategy(),
                    row.games(),
                    row.wins(),
                    row.draws(),
                    row.losses(),
                    row.winRate()
            );
        }
    }

    /** Загружает и воспроизводит сохранённый бой. */
    private static void replay() throws IOException {

        BattleSession session =
                BattleSession.load(SAVE_FILE);

        System.out.println(
                "Бой восстановлен успешно!");

        printBoard(session.getGame());

        printHistory(session.getGame());
    }

    /** Вывод журнала событий. */
    private static void printHistory(Game game) {

        game.getHistory().forEach(
                event -> System.out.println(
                        event.message()
                )
        );

        String winner = game.getWinnerId();

        System.out.println(
                "Победитель: "
                        + (winner == null
                        ? "ничья / бой не закончен"
                        : winner)
        );
    }

    /** Отображает поле в консоли. */
    private static void printBoard(Game game) {

        Board board = game.getBoard();

        System.out.print("   ");

        for (int x = 0; x < board.getWidth(); x++) {
            System.out.print(x + " ");
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
                            && tank.getPosition()
                            .equals(position)) {

                        symbol = tank.getId().charAt(0);
                    }
                }

                System.out.print(symbol + " ");
            }

            System.out.println();
        }
    }
}
