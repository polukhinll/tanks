package org.example;

import org.example.bot.AggressiveBot;
import org.example.bot.BotStrategy;
import org.example.bot.DefensiveBot;
import org.example.bot.SniperBot;
import org.example.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Автоматические бои и турнир трёх стратегий. */
public final class Tournament {

    private Tournament() {
    }

    /** Строка итоговой таблицы. */
    public record Result(
            String strategy,
            int games,
            int wins,
            int draws,
            int losses
    ) {

        public double winRate() {

            return games == 0
                    ? 0
                    : 100.0 * wins / games;
        }
    }

    /** Внутренние счётчики результатов. */
    private static final class Counter {

        int games;
        int wins;
        int draws;
        int losses;
    }

    /** Создаёт поле для демонстрации боя. */
    public static Board defaultBoard() {

        Board board = new Board(8, 8);

        // Стены.
        for (int y = 1; y <= 3; y++) {
            board.set(
                    new Position(3, y),
                    CellType.WALL
            );
        }

        // Укрытия.
        board.set(
                new Position(1, 4),
                CellType.COVER
        );

        board.set(
                new Position(6, 4),
                CellType.COVER
        );

        return board;
    }

    /** Создаёт новый бой. */
    public static BattleSession newBattle(long seed) {

        return new BattleSession(
                defaultBoard(),
                new LightTank("A", new Position(0, 2)),
                new HeavyTank("B", new Position(7, 5)),
                seed
        );
    }

    /** Проводит один бой между двумя ботами. */
    public static BattleSession battle(
            BotStrategy first,
            BotStrategy second,
            long seed
    ) {

        BattleSession session = newBattle(seed);

        for (int turn = 0;
             turn < 100 && !session.getGame().isFinished();
             turn++) {

            String player = session.getGame()
                    .getCurrentTank().getId();

            BotStrategy bot = player.equals("A")
                    ? first
                    : second;

            TurnAction action = bot.decide(
                    session.getGame());

            session.play(action);
        }

        return session;
    }

    /** Проводит турнир всех трёх стратегий. */
    public static List<Result> run(
            int gamesPerPair,
            long seed
    ) {

        if (gamesPerPair <= 0) {
            throw new IllegalArgumentException(
                    "Число боёв должно быть больше 0");
        }

        List<BotStrategy> bots = List.of(
                new AggressiveBot(),
                new DefensiveBot(),
                new SniperBot()
        );

        Map<String, Counter> table = new HashMap<>();

        for (BotStrategy bot : bots) {
            table.put(bot.name(), new Counter());
        }

        long battleNumber = 0;

        for (int i = 0; i < bots.size(); i++) {

            for (int j = i + 1; j < bots.size(); j++) {

                for (int k = 0; k < gamesPerPair; k++) {

                    // Первая расстановка.
                    update(
                            bots.get(i),
                            bots.get(j),
                            battle(
                                    bots.get(i),
                                    bots.get(j),
                                    seed + battleNumber++
                            ),
                            table
                    );

                    // Меняем ботов местами.
                    update(
                            bots.get(j),
                            bots.get(i),
                            battle(
                                    bots.get(j),
                                    bots.get(i),
                                    seed + battleNumber++
                            ),
                            table
                    );
                }
            }
        }

        // Статистика через Stream API.
        return bots.stream()
                .map(bot -> {

                    Counter c = table.get(bot.name());

                    return new Result(
                            bot.name(),
                            c.games,
                            c.wins,
                            c.draws,
                            c.losses
                    );
                })
                .sorted((a, b) ->
                        Double.compare(
                                b.winRate(),
                                a.winRate()
                        ))
                .toList();
    }

    /** Записывает результат боя в таблицу. */
    private static void update(
            BotStrategy first,
            BotStrategy second,
            BattleSession session,
            Map<String, Counter> table
    ) {

        Counter a = table.get(first.name());
        Counter b = table.get(second.name());

        a.games++;
        b.games++;

        String winner = session.getGame().getWinnerId();

        if ("A".equals(winner)) {

            a.wins++;
            b.losses++;

        } else if ("B".equals(winner)) {

            b.wins++;
            a.losses++;

        } else {

            a.draws++;
            b.draws++;
        }
    }
}

