package edu.jieqi.model;

public class Piece {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String BLACK_SIDE = "\u001B[36m";

    private final PlayerColor color;
    private final PieceType type;
    private final PieceType hiddenMoveType;
    private boolean visible;

    public Piece(PlayerColor color, PieceType type, PieceType hiddenMoveType, boolean visible) {
        this.color = color;
        this.type = type;
        this.hiddenMoveType = hiddenMoveType;
        this.visible = visible;
    }

    public PlayerColor color() {
        return color;
    }

    public PieceType type() {
        return type;
    }

    public PieceType moveType() {
        return visible ? type : hiddenMoveType;
    }

    public PieceType hiddenMoveType() {
        return hiddenMoveType;
    }

    public boolean visible() {
        return visible;
    }

    public void reveal() {
        visible = true;
    }

    public Piece copy() {
        return new Piece(color, type, hiddenMoveType, visible);
    }

    public String key() {
        return color.name() + ":" + type.name()
                + ":" + hiddenMoveType.name() + ":" + (visible ? "1" : "0");
    }

    public String display() {
        String colorCode = color == PlayerColor.RED ? RED : BLACK_SIDE;
        if (!visible) {
            return colorCode + "■" + RESET;
        }
        return colorCode + type.displayName(color) + RESET;
    }
}
