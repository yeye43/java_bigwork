package edu.jieqi.message;

public class ErrorMessage {
    private final String messageType;
    private final int code;
    private final String message;

    public ErrorMessage(int code, String message) {
        this.messageType = "error";
        this.code = code;
        this.message = message;
    }

    public String messageType() {
        return messageType;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }
}
