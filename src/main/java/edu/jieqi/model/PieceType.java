package edu.jieqi.model;

public enum PieceType {
    KING("帅", "将"),
    ROOK("车", "车"),
    KNIGHT("马", "马"),
    CANNON("炮", "炮"),
    PAWN("兵", "卒"),
    GUARD("仕", "士"),
    BISHOP("相", "象");

    private final String redName;
    private final String blackName;

    PieceType(String redName, String blackName) {
        this.redName = redName;
        this.blackName = blackName;
    }

    public String displayName(PlayerColor color) {
        return color == PlayerColor.RED ? redName : blackName;
    }
}
