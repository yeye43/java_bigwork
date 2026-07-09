package edu.jieqi;

import edu.jieqi.ai.SearchAi;
import edu.jieqi.engine.Board;
import edu.jieqi.model.Move;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AiRegressionSelfTest {
    public static void main(String[] args) throws Exception {
        new AiRegressionSelfTest().runAll();
        System.out.println("AI regression self test passed.");
    }

    private void runAll() throws Exception {
        Path file = Path.of("puzzles", "ai-regression.tsv");
        if (!Files.isRegularFile(file)) {
            throw new AssertionError("Missing puzzle file: " + file);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        int cases = 0;
        for (String line : lines) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            runCase(line);
            cases++;
        }
        if (cases == 0) {
            throw new AssertionError("No AI regression cases found.");
        }
    }

    private void runCase(String line) {
        String[] parts = line.split("\t");
        if (parts.length != 4) {
            throw new AssertionError("Bad puzzle row: " + line);
        }
        String name = parts[0];
        PlayerColor side = parseColor(parts[1]);
        String expected = parts[2];
        Board board = parseBoard(parts[3]);
        Move move = new SearchAi(null).chooseMove(board, side, System.currentTimeMillis(), 6_000);
        String actual = move == null ? "null" : move.notation();
        if (expected.startsWith("avoid ")) {
            String discouraged = expected.substring(6);
            if (discouraged.equals(actual)) {
                throw new AssertionError(name + " should not prefer " + discouraged + " in this puzzle, but got it");
            }
            return;
        }
        if (move == null || !expected.equals(actual)) {
            throw new AssertionError(name + " expected " + expected
                    + " but got " + actual);
        }
    }

    private Board parseBoard(String text) {
        Board board = new Board();
        for (String item : text.split(";")) {
            if (item.isBlank()) {
                continue;
            }
            String[] parts = item.split(":");
            if (parts.length < 3 || parts.length > 5) {
                throw new AssertionError("Bad piece: " + item);
            }
            PlayerColor color = parseColor(parts[0]);
            PieceType type = PieceType.valueOf(parts[1].toUpperCase());
            Position position = Position.parse(parts[2]);
            boolean visible = parts.length < 4 || Boolean.parseBoolean(parts[3]);
            PieceType hiddenMoveType = parts.length == 5 ? PieceType.valueOf(parts[4].toUpperCase()) : type;
            board.set(position, new Piece(color, type, hiddenMoveType, visible));
        }
        return board;
    }

    private PlayerColor parseColor(String text) {
        return "red".equalsIgnoreCase(text) ? PlayerColor.RED : PlayerColor.BLACK;
    }
}
