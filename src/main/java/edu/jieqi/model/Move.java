package edu.jieqi.model;

public class Move {
    private final Position source;
    private final Position destination;
    private final boolean flipOnly;
    private final long turnStartTime;

    private Move(Position source, Position destination, boolean flipOnly, long turnStartTime) {
        this.source = source;
        this.destination = destination;
        this.flipOnly = flipOnly;
        this.turnStartTime = turnStartTime;
    }

    public static Move flip(Position position, long turnStartTime) {
        return new Move(position, position, true, turnStartTime);
    }

    public static Move move(Position source, Position destination, long turnStartTime) {
        return new Move(source, destination, false, turnStartTime);
    }

    public Position source() {
        return source;
    }

    public Position destination() {
        return destination;
    }

    public boolean flipOnly() {
        return flipOnly;
    }

    public long turnStartTime() {
        return turnStartTime;
    }

    public String notation() {
        if (flipOnly) {
            return "flip " + source.notation();
        }
        return "move " + source.notation() + " " + destination.notation();
    }
}
