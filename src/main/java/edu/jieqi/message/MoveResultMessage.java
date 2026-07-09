package edu.jieqi.message;

import edu.jieqi.model.MoveResult;
import edu.jieqi.model.PieceType;

public class MoveResultMessage {
    private final String messageType;
    private final boolean success;
    private final boolean valid;
    private final String message;
    private final MoveMessage move;
    private final PieceType flipResult;

    public MoveResultMessage(boolean success, boolean valid, String message, MoveMessage move) {
        this(success, valid, message, move, null);
    }

    public MoveResultMessage(boolean success, boolean valid, String message, MoveMessage move, PieceType flipResult) {
        this.messageType = "moveResult";
        this.success = success;
        this.valid = valid;
        this.message = message;
        this.move = move;
        this.flipResult = flipResult;
    }

    public static MoveResultMessage fromResult(MoveResult result, MoveMessage move) {
        return new MoveResultMessage(result.success(), result.success(), result.message(), move, result.flipResult());
    }

    public String messageType() {
        return messageType;
    }

    public boolean success() {
        return success;
    }

    public boolean valid() {
        return valid;
    }

    public String message() {
        return message;
    }

    public MoveMessage move() {
        return move;
    }

    public PieceType flipResult() {
        return flipResult;
    }
}
