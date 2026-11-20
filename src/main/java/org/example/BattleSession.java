package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.model.*;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Управляет боем и сохраняет его для воспроизведения. */
public class BattleSession {

    private static final Gson JSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    private final Game game;
    private final long seed;
    private final List<List<CellType>> initialField;
    private final TankInfo first;
    private final TankInfo second;

    private final List<TurnAction> turns = new ArrayList<>();

    public BattleSession(Board board, Tank a, Tank b, long seed) {
        this.initialField = fieldToRows(board);
        this.first = TankInfo.from(a);
        this.second = TankInfo.from(b);
        this.seed = seed;

        this.game = new Game(
                rowsToField(initialField), a, b, seed);
    }

    public Game getGame() {
        return game;
    }

    /** Выполняет ход и записывает его в журнал. */
    public List<GameEvent> play(TurnAction command) {
        List<GameEvent> events = game.takeTurn(
                command.dx(),
                command.dy(),
                command.targetId(),
                command.zone()
        );

        turns.add(command);
        return events;
    }

    public List<TurnAction> getTurns() {
        return List.copyOf(turns);
    }

    /** Сохраняет бой в JSON-файл. */
    public void save(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(
                file, StandardCharsets.UTF_8)) {

            JSON.toJson(snapshot(), writer);
        }
    }

    /** Загружает сохранение и повторяет все ходы. */
    public static BattleSession load(Path file) throws IOException {

        try (Reader reader = Files.newBufferedReader(
                file, StandardCharsets.UTF_8)) {

            SaveData data = JSON.fromJson(reader, SaveData.class);

            if (data == null
                    || data.version() != 1
                    || data.turns() == null) {

                throw new IllegalArgumentException(
                        "Неверный формат JSON");
            }

            BattleSession session = new BattleSession(
                    rowsToField(data.field()),
                    data.first().create(),
                    data.second().create(),
                    data.seed()
            );

            for (TurnAction command : data.turns()) {
                session.play(command);
            }

            if (!session.snapshot().equals(data)) {
                throw new IllegalArgumentException(
                        "Состояние не совпало с журналом");
            }

            return session;

        } catch (RuntimeException e) {
            throw new IOException(
                    "Некорректный файл боя: " + file, e);
        }
    }

    /** Формирует полный снимок текущего состояния. */
    private SaveData snapshot() {

        List<TankState> states = game.getTanks().stream()
                .map(TankState::from)
                .toList();

        List<String> events = game.getHistory().stream()
                .map(GameEvent::message)
                .toList();

        return new SaveData(
                1,
                initialField,
                first,
                second,
                seed,
                List.copyOf(turns),
                states,
                game.getCurrentTank().getId(),
                game.isFinished(),
                game.getWinnerId(),
                events
        );
    }

    private static List<List<CellType>> fieldToRows(Board board) {

        List<List<CellType>> rows = new ArrayList<>();

        for (int y = 0; y < board.getHeight(); y++) {
            List<CellType> row = new ArrayList<>();

            for (int x = 0; x < board.getWidth(); x++) {
                row.add(board.get(new Position(x, y)));
            }

            rows.add(List.copyOf(row));
        }

        return List.copyOf(rows);
    }

    private static Board rowsToField(List<List<CellType>> rows) {

        if (rows == null || rows.isEmpty()
                || rows.get(0) == null
                || rows.get(0).isEmpty()) {

            throw new IllegalArgumentException("Пустое поле");
        }

        int width = rows.get(0).size();
        Board board = new Board(width, rows.size());

        for (int y = 0; y < rows.size(); y++) {

            if (rows.get(y) == null
                    || rows.get(y).size() != width) {

                throw new IllegalArgumentException(
                        "Неверные строки поля");
            }

            for (int x = 0; x < width; x++) {
                board.set(
                        new Position(x, y),
                        rows.get(y).get(x)
                );
            }
        }

        return board;
    }

    /** Начальные данные танка. */
    private record TankInfo(
            String kind,
            String id,
            Position position
    ) {

        static TankInfo from(Tank tank) {

            String kind = tank instanceof LightTank
                    ? "LIGHT"
                    : tank instanceof HeavyTank
                    ? "HEAVY"
                    : null;

            if (kind == null) {
                throw new IllegalArgumentException(
                        "Неизвестный танк");
            }

            return new TankInfo(
                    kind, tank.getId(), tank.getPosition());
        }

        Tank create() {

            return switch (kind) {
                case "LIGHT" -> new LightTank(id, position);
                case "HEAVY" -> new HeavyTank(id, position);

                default -> throw new IllegalArgumentException(
                        "Неизвестная модель: " + kind);
            };
        }
    }

    /** Текущее состояние одного танка. */
    private record TankState(
            String id,
            Position position,
            int health,
            int armor,
            int ammo,
            int speed
    ) {

        static TankState from(Tank tank) {

            return new TankState(
                    tank.getId(),
                    tank.getPosition(),
                    tank.getHealth(),
                    tank.getArmor(),
                    tank.getAmmo(),
                    tank.getSpeed()
            );
        }
    }

    /** Данные, которые записываются в JSON. */
    private record SaveData(
            int version,
            List<List<CellType>> field,
            TankInfo first,
            TankInfo second,
            long seed,
            List<TurnAction> turns,
            List<TankState> tanks,
            String currentId,
            boolean finished,
            String winnerId,
            List<String> events
    ) {
    }
}
