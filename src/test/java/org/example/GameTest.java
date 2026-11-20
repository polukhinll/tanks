package org.example;

import org.example.model.*;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameTest {

    private Game createGame(Board board, long seed) {
        Tank first = new LightTank("A", new Position(0, 0));
        Tank second = new HeavyTank("B", new Position(3, 0));

        return new Game(board, first, second, seed);
    }

    @Test
    void shouldMoveTank() {
        Game game = createGame(new Board(5, 5), 42);

        game.takeTurn(0, 2, null, null);

        assertEquals(new Position(0, 2),
                game.getTanks().get(0).getPosition());
    }

    @Test
    void shouldSwitchTurns() {
        Game game = createGame(new Board(5, 5), 42);

        game.takeTurn(0, 1, null, null);

        assertEquals("B", game.getCurrentTank().getId());
    }

    @Test
    void shouldNotMoveThroughWall() {
        Board board = new Board(5, 5);
        board.set(new Position(1, 0), CellType.WALL);

        Game game = createGame(board, 42);

        assertThrows(IllegalArgumentException.class,
                () -> game.takeTurn(2, 0, null, null));
    }

    @Test
    void shouldNotMoveDiagonally() {
        Game game = createGame(new Board(5, 5), 42);

        assertThrows(IllegalArgumentException.class,
                () -> game.takeTurn(1, 1, null, null));
    }

    @Test
    void shouldNotExceedSpeed() {
        Game game = createGame(new Board(5, 5), 42);

        assertThrows(IllegalArgumentException.class,
                () -> game.takeTurn(0, 4, null, null));
    }

    @Test
    void shouldNotShootThroughWall() {
        Board board = new Board(5, 5);
        board.set(new Position(2, 0), CellType.WALL);

        Game game = createGame(board, 42);

        assertThrows(IllegalArgumentException.class,
                () -> game.takeTurn(0, 0, "B", HitZone.HULL));

        assertEquals(8, game.getTanks().get(0).getAmmo());
    }

    @Test
    void shouldNotMoveWhenShotIsInvalid() {
        Board board = new Board(5, 5);
        board.set(new Position(2, 0), CellType.WALL);

        Game game = createGame(board, 42);

        assertThrows(IllegalArgumentException.class,
                () -> game.takeTurn(1, 0, "B", HitZone.HULL));

        assertEquals(new Position(0, 0),
                game.getTanks().get(0).getPosition());
    }

    @Test
    void shouldDamageEnemy() {
        Game game = createGame(new Board(5, 5), 42);

        game.takeTurn(0, 0, "B", HitZone.HULL);

        assertTrue(game.getTanks().get(1).getHealth() < 26);
        assertEquals(7, game.getTanks().get(0).getAmmo());
    }

    @Test
    void shouldDamageTracks() {
        Game game = createGame(new Board(5, 5), 42);

        game.takeTurn(0, 0, "B", HitZone.TRACKS);

        assertEquals(1, game.getTanks().get(1).getSpeed());
    }

    @Test
    void coverShouldReduceDamage() {
        Board plain = new Board(5, 5);
        Board covered = new Board(5, 5);

        covered.set(new Position(3, 0), CellType.COVER);

        Game first = createGame(plain, 42);
        Game second = createGame(covered, 42);

        first.takeTurn(0, 0, "B", HitZone.HULL);
        second.takeTurn(0, 0, "B", HitZone.HULL);

        int damageWithoutCover =
                26 - first.getTanks().get(1).getHealth();

        int damageWithCover =
                26 - second.getTanks().get(1).getHealth();

        assertTrue(damageWithCover < damageWithoutCover);
    }

    private List<String> playBattle(long seed) {
        Game game = createGame(new Board(5, 5), seed);

        int turns = 0;

        while (!game.isFinished() && turns < 20) {
            String target = game.getCurrentTank()
                    .getId().equals("A") ? "B" : "A";

            game.takeTurn(0, 0, target, HitZone.TURRET);
            turns++;
        }

        assertTrue(game.isFinished());
        assertNotNull(game.getWinnerId());

        return game.getHistory()
                .stream()
                .map(GameEvent::message)
                .toList();
    }

    @Test
    void shouldFinishBattle() {
        assertFalse(playBattle(42).isEmpty());
    }

    @Test
    void sameSeedShouldGiveSameBattle() {
        assertEquals(playBattle(42), playBattle(42));
    }
}

