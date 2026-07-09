package edu.jieqi.engine;

import edu.jieqi.model.Move;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    private final RuleEngine ruleEngine;

    public MoveGenerator(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public List<Move> generateActions(Board board, PlayerColor color, long turnStartTime) {
        List<Move> actions = new ArrayList<>();
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            for (int y = 0; y < Position.HEIGHT; y++) {
                for (int x = 0; x < Position.WIDTH; x++) {
                    Position destination = new Position(x, y);
                    if (ruleEngine.canMove(board, source, destination, color)) {
                        actions.add(Move.move(source, destination, turnStartTime));
                    }
                }
            }
        }
        return actions;
    }

    public List<Move> generateCheckEscapes(Board board, PlayerColor color, long turnStartTime) {
        List<Move> escapes = new ArrayList<>();
        for (Move action : generateActions(board, color, turnStartTime)) {
            if (escapesCheck(board, action, color)) {
                escapes.add(action);
            }
        }
        return escapes;
    }

    public boolean hasAnyAction(Board board, PlayerColor color) {
        return !generateActions(board, color, System.currentTimeMillis()).isEmpty();
    }

    public boolean hasCheckEscape(Board board, PlayerColor color) {
        return !generateCheckEscapes(board, color, System.currentTimeMillis()).isEmpty();
    }

    private boolean escapesCheck(Board board, Move move, PlayerColor color) {
        return !move.flipOnly() && ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), color);
    }
}
