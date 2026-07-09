package edu.jieqi.record;

import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.engine.Board;
import edu.jieqi.model.PlayerColor;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GameRecord implements Closeable {
    private static final DateTimeFormatter FILE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path file;
    private final BufferedWriter writer;
    private int turn = 0;

    public GameRecord(Path directory) throws IOException {
        Files.createDirectories(directory);
        this.file = directory.resolve("game-" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".jsonl");
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
    }

    public GameRecord(Path directory, Board initialBoard, PlayerColor firstPlayer) throws IOException {
        this(directory);
        recordInitialBoard(initialBoard, firstPlayer);
    }

    public GameRecord(Path directory, Board initialBoard, PlayerColor firstPlayer, String mode) throws IOException {
        this(directory);
        recordInitialBoard(initialBoard, firstPlayer, mode);
    }

    public synchronized void recordInitialBoard(Board board, PlayerColor firstPlayer) throws IOException {
        recordInitialBoard(board, firstPlayer, "");
    }

    public synchronized void recordInitialBoard(Board board, PlayerColor firstPlayer, String mode) throws IOException {
        writer.write(RecordEntry.initialBoard(board, firstPlayer, mode));
        writer.newLine();
        writer.flush();
    }

    public synchronized void recordMove(PlayerColor color, Move move, MoveResult result) throws IOException {
        turn++;
        RecordEntry entry = new RecordEntry(
                turn,
                color,
                move,
                result.flipResult(),
                result.capture(),
                result.gameOver(),
                System.currentTimeMillis());
        writer.write(entry.toJsonLine());
        writer.newLine();
        writer.flush();
    }

    public Path file() {
        return file;
    }

    @Override
    public synchronized void close() throws IOException {
        writer.close();
    }
}
