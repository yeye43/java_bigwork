package edu.jieqi.message;

public class GameStartMessage {
    private final String messageType;
    private final String redPlayerId;
    private final String blackPlayerId;
    private final String yourColor;
    private final boolean firstHand;

    public GameStartMessage(String redPlayerId, String blackPlayerId, String yourColor, boolean firstHand) {
        this.messageType = "gameStart";
        this.redPlayerId = redPlayerId;
        this.blackPlayerId = blackPlayerId;
        this.yourColor = yourColor;
        this.firstHand = firstHand;
    }

    public String messageType() {
        return messageType;
    }

    public String redPlayerId() {
        return redPlayerId;
    }

    public String blackPlayerId() {
        return blackPlayerId;
    }

    public String yourColor() {
        return yourColor;
    }

    public boolean firstHand() {
        return firstHand;
    }
}
