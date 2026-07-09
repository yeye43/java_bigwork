package edu.jieqi.model;

public class MoveResult {
    private final boolean success;
    private final String message;
    private final boolean capture;
    private final boolean gameOver;
    private final PieceType flipResult;

    private MoveResult(boolean success, String message, boolean capture, boolean gameOver, PieceType flipResult) {
        this.success = success;
        this.message = message;
        this.capture = capture;
        this.gameOver = gameOver;
        this.flipResult = flipResult;
    }

    public static MoveResult success(String message, boolean capture, boolean gameOver) {
        return new MoveResult(true, message, capture, gameOver, null);
    }

    public static MoveResult success(String message, boolean capture, boolean gameOver, PieceType flipResult) {
        return new MoveResult(true, message, capture, gameOver, flipResult);
    }

    public static MoveResult failure(String message) {
        return new MoveResult(false, message, false, false, null);
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public boolean capture() {
        return capture;
    }

    public boolean gameOver() {
        return gameOver;
    }

    public PieceType flipResult() {
        return flipResult;
    }
}
