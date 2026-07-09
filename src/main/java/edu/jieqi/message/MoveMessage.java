package edu.jieqi.message;

import edu.jieqi.model.Move;
import edu.jieqi.model.Position;

public class MoveMessage {
    private final String messageType;
    private final String fromX;
    private final int fromY;
    private final String toX;
    private final int toY;
    private final boolean flip;

    public MoveMessage(String fromX, int fromY, String toX, int toY, boolean flip) {
        this.messageType = "move";
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.flip = flip;
    }

    public static MoveMessage fromMove(Move move) {
        return new MoveMessage(
                String.valueOf((char) ('a' + move.source().x())),
                move.source().y(),
                String.valueOf((char) ('a' + move.destination().x())),
                move.destination().y(),
                move.flipOnly());
    }

    public Move toMove(long turnStartTime) {
        Position source = toPosition(fromX, fromY);
        Position destination = toPosition(toX, toY);
        return flip ? Move.flip(source, turnStartTime) : Move.move(source, destination, turnStartTime);
    }

    public String messageType() {
        return messageType;
    }

    public String fromX() {
        return fromX;
    }

    public int fromY() {
        return fromY;
    }

    public String toX() {
        return toX;
    }

    public int toY() {
        return toY;
    }

    public boolean flip() {
        return flip;
    }

    private static Position toPosition(String xText, int y) {
        if (xText == null || xText.length() != 1) {
            throw new IllegalArgumentException("横坐标必须是 a-i");
        }
        int x = Character.isDigit(xText.charAt(0))
                ? Integer.parseInt(xText)
                : Character.toLowerCase(xText.charAt(0)) - 'a';
        return new Position(x, y);
    }
}
