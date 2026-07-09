package edu.jieqi.record;

import edu.jieqi.model.Move;
import edu.jieqi.engine.Board;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

public class RecordEntry {
    private final int turn;
    private final PlayerColor color;
    private final Move move;
    private final PieceType type;
    private final boolean capture;
    private final boolean gameOver;
    private final long timestamp;

    public RecordEntry(
            int turn,
            PlayerColor color,
            Move move,
            PieceType type,
            boolean capture,
            boolean gameOver,
            long timestamp) {
        this.turn = turn;
        this.color = color;
        this.move = move;
        this.type = type;
        this.capture = capture;
        this.gameOver = gameOver;
        this.timestamp = timestamp;
    }

    public static String initialBoard(Board board, PlayerColor firstPlayer) {
        return initialBoard(board, firstPlayer, "");
    }

    public static String initialBoard(Board board, PlayerColor firstPlayer, String mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendPair(sb, "kind", "initial").append(",");
        appendPair(sb, "firstPlayer", firstPlayer == PlayerColor.RED ? "red" : "black").append(",");
        if (mode != null && !mode.isBlank()) {
            appendPair(sb, "mode", mode).append(",");
        }
        appendPair(sb, "timestamp", System.currentTimeMillis()).append(",");
        sb.append("\"board\":[");
        boolean first = true;
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = board.get(new Position(x, y));
                if (piece == null) {
                    continue;
                }
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("{");
                appendPair(sb, "position", new Position(x, y).notation()).append(",");
                appendPair(sb, "color", piece.color() == PlayerColor.RED ? "red" : "black").append(",");
                appendPair(sb, "type", pieceName(piece.type())).append(",");
                appendPair(sb, "hiddenMoveType", pieceName(piece.hiddenMoveType())).append(",");
                appendPair(sb, "visible", piece.visible());
                sb.append("}");
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    public String toJsonLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        appendPair(sb, "turn", turn).append(",");
        appendPair(sb, "color", colorName()).append(",");
        appendPair(sb, "source", move.source().notation()).append(",");
        appendPair(sb, "destination", move.destination().notation());
        if (type != null) {
            sb.append(",");
            appendPair(sb, "type", pieceName(type));
        }
        sb.append(",");
        appendPair(sb, "capture", capture).append(",");
        appendPair(sb, "gameOver", gameOver).append(",");
        appendPair(sb, "timestamp", timestamp);
        sb.append("}");
        return sb.toString();
    }

    private String colorName() {
        return color == PlayerColor.RED ? "red" : "black";
    }

    private static String pieceName(PieceType pieceType) {
        return switch (pieceType) {
            case KING -> "king";
            case ROOK -> "rook";
            case KNIGHT -> "knight";
            case CANNON -> "cannon";
            case PAWN -> "pawn";
            case GUARD -> "guard";
            case BISHOP -> "bishop";
        };
    }

    private static StringBuilder appendPair(StringBuilder sb, String key, String value) {
        return sb.append("\"").append(escape(key)).append("\":\"").append(escape(value)).append("\"");
    }

    private static StringBuilder appendPair(StringBuilder sb, String key, int value) {
        return sb.append("\"").append(escape(key)).append("\":").append(value);
    }

    private static StringBuilder appendPair(StringBuilder sb, String key, long value) {
        return sb.append("\"").append(escape(key)).append("\":").append(value);
    }

    private static StringBuilder appendPair(StringBuilder sb, String key, boolean value) {
        return sb.append("\"").append(escape(key)).append("\":").append(value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
