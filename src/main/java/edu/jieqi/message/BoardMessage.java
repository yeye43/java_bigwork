package edu.jieqi.message;

public class BoardMessage {
    private final String messageType;
    private final String board;

    public BoardMessage(String board) {
        this.messageType = "board";
        this.board = board;
    }

    public String messageType() {
        return messageType;
    }

    public String board() {
        return board;
    }
}
