package org.example;

import org.example.bot.AggressiveBot;
import org.example.bot.BotStrategy;
import org.example.bot.DefensiveBot;
import org.example.bot.SniperBot;
import org.example.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThirdStageTest {

    @TempDir
    Path temp;

    private BattleSession simpleGame(long seed) {

        return new BattleSession(
                new Board(5, 5),
                new LightTank("A", new Position(0, 0)),
                new HeavyTank("B", new Position(3, 0)),
                seed
        );
    }

    // 10 сценариев с разными seed.
    @ParameterizedTest
    @ValueSource(longs = {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10
    })
    void botBattlesAreValidForTenSeeds(long seed) {

        BattleSession session = Tournament.battle(
                new AggressiveBot(),
                new DefensiveBot(),
                seed
        );

        assertFalse(session.getTurns().isEmpty());

        assertTrue(session.getTurns().size() <= 100);

        long moves = session.getGame()
                .getHistory()
                .stream()
                .filter(e ->
                        e.message().contains("остался")
                                || e.message().contains(
                                "переместился"))
                .count();

        assertEquals(session.getTurns().size(), moves);
    }

    // Проверка трёх видов ботов.
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void eachBotMakesLegalFirstTurn(int botIndex) {

        List<BotStrategy> bots = List.of(
                new AggressiveBot(),
                new DefensiveBot(),
                new SniperBot()
        );

        BattleSession session =
                Tournament.newBattle(42L);

        session.play(
                bots.get(botIndex).decide(
                        session.getGame()
                )
        );

        assertEquals(1, session.getTurns().size());
    }

    // Проверка статистики на разных размерах турнира.
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void tournamentCountsAllMatches(int rounds) {

        List<Tournament.Result> table =
                Tournament.run(rounds, 42L);

        assertEquals(3, table.size());

        for (Tournament.Result result : table) {

            assertEquals(
                    4 * rounds,
                    result.games()
            );

            assertEquals(
                    result.games(),
                    result.wins()
                            + result.draws()
                            + result.losses()
            );

            assertTrue(
                    result.winRate() >= 0
                            && result.winRate() <= 100
            );
        }
    }

    @Test
    void saveAndLoadBeforeFirstTurn() throws IOException {

        BattleSession session = simpleGame(42);

        Path file = temp.resolve("start.json");

        session.save(file);

        BattleSession loaded =
                BattleSession.load(file);

        assertEquals(0, loaded.getTurns().size());

        assertEquals(
                "A",
                loaded.getGame().getCurrentTank().getId()
        );
    }

    @Test
    void saveAndLoadDuringBattle() throws IOException {

        BattleSession session = simpleGame(42);

        session.play(
                TurnAction.shoot("B", HitZone.HULL)
        );

        session.play(
                TurnAction.move(0, 1)
        );

        Path file = temp.resolve("middle.json");

        session.save(file);

        BattleSession loaded =
                BattleSession.load(file);

        assertEquals(
                session.getTurns(),
                loaded.getTurns()
        );

        assertEquals(
                session.getGame().getHistory(),
                loaded.getGame().getHistory()
        );

        assertEquals(
                session.getGame().getTanks().get(1).getHealth(),
                loaded.getGame().getTanks().get(1).getHealth()
        );
    }

    @Test
    void saveAndLoadAfterEnd() throws IOException {

        BattleSession session = simpleGame(42);

        for (int i = 0;
             i < 20 && !session.getGame().isFinished();
             i++) {

            String target = session.getGame()
                    .getCurrentTank()
                    .getId()
                    .equals("A") ? "B" : "A";

            session.play(
                    TurnAction.shoot(target, HitZone.TURRET)
            );
        }

        assertTrue(session.getGame().isFinished());

        Path file = temp.resolve("finish.json");

        session.save(file);

        BattleSession loaded =
                BattleSession.load(file);

        assertEquals(
                session.getGame().getWinnerId(),
                loaded.getGame().getWinnerId()
        );

        assertTrue(loaded.getGame().isFinished());
    }

    @Test
    void rejectedTurnIsNotSaved() {

        BattleSession session = simpleGame(42);

        assertThrows(
                IllegalArgumentException.class,
                () -> session.play(
                        TurnAction.move(100, 0)
                )
        );

        assertTrue(session.getTurns().isEmpty());
    }

    @Test
    void changedHealthInJsonIsDetected() throws IOException {

        Path file = temp.resolve("damaged.json");

        simpleGame(42).save(file);

        String json = Files.readString(file);

        assertTrue(
                json.contains("\"health\": 18")
        );

        Files.writeString(
                file,
                json.replace(
                        "\"health\": 18",
                        "\"health\": 999"
                )
        );

        assertThrows(
                IOException.class,
                () -> BattleSession.load(file)
        );
    }

    @Test
    void brokenJsonIsRejected() throws IOException {

        Path file = temp.resolve("broken.json");

        Files.writeString(file, "{ invalid }");

        assertThrows(
                IOException.class,
                () -> BattleSession.load(file)
        );
    }

    @Test
    void missingFileIsRejected() {

        assertThrows(
                IOException.class,
                () -> BattleSession.load(
                        temp.resolve("missing.json")
                )
        );
    }

    @Test
    void tournamentRejectsZeroGames() {

        assertThrows(
                IllegalArgumentException.class,
                () -> Tournament.run(0, 42)
        );
    }

    @Test
    void sameSeedProducesSameBattle() {

        BattleSession a = Tournament.battle(
                new AggressiveBot(),
                new SniperBot(),
                1234
        );

        BattleSession b = Tournament.battle(
                new AggressiveBot(),
                new SniperBot(),
                1234
        );

        assertEquals(
                a.getTurns(),
                b.getTurns()
        );

        assertEquals(
                a.getGame().getHistory(),
                b.getGame().getHistory()
        );

        assertEquals(
                a.getGame().getWinnerId(),
                b.getGame().getWinnerId()
        );
    }
}
