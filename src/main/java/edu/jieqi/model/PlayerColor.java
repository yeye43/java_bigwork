package edu.jieqi.model;

public enum PlayerColor {
    RED("红"),
    BLACK("黑");

    private final String displayName;

    PlayerColor(String displayName) {
        this.displayName = displayName;
    }

    public PlayerColor opponent() {
        return this == RED ? BLACK : RED;
    }

    public int forwardDirection() {
        return this == RED ? 1 : -1;
    }

    public String displayName() {
        return displayName;
    }
}
