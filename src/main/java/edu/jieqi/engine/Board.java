package edu.jieqi.engine;

import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final Piece[][] grid = new Piece[Position.WIDTH][Position.HEIGHT];

    public static Board initialBoard() {
        Board board = new Board();
        board.setupSide(PlayerColor.RED);
        board.setupSide(PlayerColor.BLACK);
        return board;
    }

    public static Board standardVisibleBoard() {
        Board board = new Board();
        board.setupVisibleSide(PlayerColor.RED);
        board.setupVisibleSide(PlayerColor.BLACK);
        return board;
    }

    public Board copy() {
        Board copied = new Board();
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = grid[x][y];
                if (piece != null) {
                    copied.grid[x][y] = piece.copy();
                }
            }
        }
        return copied;
    }

    private void setupSide(PlayerColor color) {
        int backRank = color == PlayerColor.RED ? 0 : 9;
        int pawnRank = color == PlayerColor.RED ? 3 : 6;
        int cannonRank = color == PlayerColor.RED ? 2 : 7;

        List<PieceType> hiddenTypes = hiddenTypes();
        Collections.shuffle(hiddenTypes, RANDOM);

        List<StartingSquare> squares = new ArrayList<>();
        squares.add(new StartingSquare(new Position(0, backRank), PieceType.ROOK));
        squares.add(new StartingSquare(new Position(1, backRank), PieceType.KNIGHT));
        squares.add(new StartingSquare(new Position(2, backRank), PieceType.BISHOP));
        squares.add(new StartingSquare(new Position(3, backRank), PieceType.GUARD));
        squares.add(new StartingSquare(new Position(5, backRank), PieceType.GUARD));
        squares.add(new StartingSquare(new Position(6, backRank), PieceType.BISHOP));
        squares.add(new StartingSquare(new Position(7, backRank), PieceType.KNIGHT));
        squares.add(new StartingSquare(new Position(8, backRank), PieceType.ROOK));
        squares.add(new StartingSquare(new Position(1, cannonRank), PieceType.CANNON));
        squares.add(new StartingSquare(new Position(7, cannonRank), PieceType.CANNON));
        squares.add(new StartingSquare(new Position(0, pawnRank), PieceType.PAWN));
        squares.add(new StartingSquare(new Position(2, pawnRank), PieceType.PAWN));
        squares.add(new StartingSquare(new Position(4, pawnRank), PieceType.PAWN));
        squares.add(new StartingSquare(new Position(6, pawnRank), PieceType.PAWN));
        squares.add(new StartingSquare(new Position(8, pawnRank), PieceType.PAWN));

        set(new Position(4, backRank), new Piece(color, PieceType.KING, PieceType.KING, true));
        for (int i = 0; i < squares.size(); i++) {
            StartingSquare square = squares.get(i);
            set(square.position(), new Piece(color, hiddenTypes.get(i), square.originalType(), false));
        }
    }

    private void setupVisibleSide(PlayerColor color) {
        int backRank = color == PlayerColor.RED ? 0 : 9;
        int pawnRank = color == PlayerColor.RED ? 3 : 6;
        int cannonRank = color == PlayerColor.RED ? 2 : 7;
        setVisible(color, PieceType.ROOK, 0, backRank);
        setVisible(color, PieceType.KNIGHT, 1, backRank);
        setVisible(color, PieceType.BISHOP, 2, backRank);
        setVisible(color, PieceType.GUARD, 3, backRank);
        setVisible(color, PieceType.KING, 4, backRank);
        setVisible(color, PieceType.GUARD, 5, backRank);
        setVisible(color, PieceType.BISHOP, 6, backRank);
        setVisible(color, PieceType.KNIGHT, 7, backRank);
        setVisible(color, PieceType.ROOK, 8, backRank);
        setVisible(color, PieceType.CANNON, 1, cannonRank);
        setVisible(color, PieceType.CANNON, 7, cannonRank);
        for (int x : new int[]{0, 2, 4, 6, 8}) {
            setVisible(color, PieceType.PAWN, x, pawnRank);
        }
    }

    private void setVisible(PlayerColor color, PieceType type, int x, int y) {
        set(new Position(x, y), new Piece(color, type, type, true));
    }

    private static List<PieceType> hiddenTypes() {
        List<PieceType> types = new ArrayList<>();
        types.add(PieceType.ROOK);
        types.add(PieceType.ROOK);
        types.add(PieceType.KNIGHT);
        types.add(PieceType.KNIGHT);
        types.add(PieceType.CANNON);
        types.add(PieceType.CANNON);
        types.add(PieceType.BISHOP);
        types.add(PieceType.BISHOP);
        types.add(PieceType.GUARD);
        types.add(PieceType.GUARD);
        types.add(PieceType.PAWN);
        types.add(PieceType.PAWN);
        types.add(PieceType.PAWN);
        types.add(PieceType.PAWN);
        types.add(PieceType.PAWN);
        return types;
    }

    public Piece get(Position position) {
        return grid[position.x()][position.y()];
    }

    public void set(Position position, Piece piece) {
        grid[position.x()][position.y()] = piece;
    }

    public Piece remove(Position position) {
        Piece piece = get(position);
        set(position, null);
        return piece;
    }

    public boolean isEmpty(Position position) {
        return get(position) == null;
    }

    public int countBetween(Position source, Position destination) {
        if (source.x() != destination.x() && source.y() != destination.y()) {
            throw new IllegalArgumentException("只能统计直线之间的棋子");
        }
        int dx = Integer.compare(destination.x(), source.x());
        int dy = Integer.compare(destination.y(), source.y());
        int x = source.x() + dx;
        int y = source.y() + dy;
        int count = 0;
        while (x != destination.x() || y != destination.y()) {
            if (grid[x][y] != null) {
                count++;
            }
            x += dx;
            y += dy;
        }
        return count;
    }

    public Position findKing(PlayerColor color) {
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = grid[x][y];
                if (piece != null && piece.color() == color && piece.visible() && piece.type() == PieceType.KING) {
                    return new Position(x, y);
                }
            }
        }
        return null;
    }

    public List<Position> occupiedPositions() {
        List<Position> positions = new ArrayList<>();
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                if (grid[x][y] != null) {
                    positions.add(new Position(x, y));
                }
            }
        }
        return positions;
    }

    public String positionKey() {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = grid[x][y];
                sb.append(piece == null ? "." : piece.key()).append("|");
            }
        }
        return sb.toString();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();
        appendHeader(sb);
        for (int y = Position.HEIGHT - 1; y >= 0; y--) {
            sb.append(" ").append(y).append(" ");
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = grid[x][y];
                sb.append(padCell(piece == null ? "·" : piece.display()));
            }
            sb.append(y).append(System.lineSeparator());
        }
        appendHeader(sb);
        return sb.toString();
    }

    private void appendHeader(StringBuilder sb) {
        sb.append("   ");
        for (int x = 0; x < Position.WIDTH; x++) {
            sb.append(padCell(String.valueOf((char) ('a' + x))));
        }
        sb.append(System.lineSeparator());
    }

    private String padCell(String text) {
        int cellWidth = 4;
        int width = visualWidth(text);
        StringBuilder sb = new StringBuilder(text);
        for (int i = width; i < cellWidth; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private int visualWidth(String text) {
        int width = 0;
        boolean inAnsi = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\u001B') {
                inAnsi = true;
                continue;
            }
            if (inAnsi) {
                if (ch == 'm') {
                    inAnsi = false;
                }
                continue;
            }
            width += isWide(ch) ? 2 : 1;
        }
        return width;
    }

    private boolean isWide(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS;
    }

    private record StartingSquare(Position position, PieceType originalType) {
    }
}
