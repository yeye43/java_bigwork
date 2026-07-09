package edu.jieqi.engine;

import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

public class RuleEngine {
    public boolean canMove(Board board, Position source, Position destination, PlayerColor currentPlayer) {
        if (source.equals(destination)) {
            return false;
        }
        Piece mover = board.get(source);
        if (mover == null || mover.color() != currentPlayer) {
            return false;
        }
        Piece target = board.get(destination);
        if (target != null && target.color() == currentPlayer) {
            return false;
        }
        return switch (mover.moveType()) {
            case KING -> canKingMove(board, source, destination, mover.color());
            case ROOK -> canRookMove(board, source, destination);
            case KNIGHT -> canKnightMove(board, source, destination);
            case CANNON -> canCannonMove(board, source, destination, target != null);
            case PAWN -> canPawnMove(source, destination, mover.color());
            case GUARD -> canGuardMove(source, destination, mover.visible(), mover.color());
            case BISHOP -> canBishopMove(board, source, destination, mover.visible(), mover.color());
        };
    }

    public boolean canMoveAndKeepKingSafe(Board board, Position source, Position destination, PlayerColor currentPlayer) {
        if (!canMove(board, source, destination, currentPlayer)) {
            return false;
        }
        Board copied = board.copy();
        Piece mover = copied.remove(source);
        copied.set(destination, mover);
        if (mover != null && !mover.visible()) {
            mover.reveal();
        }
        return !isInCheck(copied, currentPlayer);
    }

    public boolean isInCheck(Board board, PlayerColor color) {
        Position king = board.findKing(color);
        if (king == null) {
            return true;
        }
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece != null && piece.color() == color.opponent() && canMove(board, source, king, piece.color())) {
                return true;
            }
        }
        return kingsFace(board);
    }

    private boolean canKingMove(Board board, Position source, Position destination, PlayerColor color) {
        Piece target = board.get(destination);
        if (target != null && target.type() == PieceType.KING && source.x() == destination.x()) {
            return board.countBetween(source, destination) == 0;
        }
        return inPalace(destination, color) && distance(source, destination) == 1;
    }

    private boolean canRookMove(Board board, Position source, Position destination) {
        return sameLine(source, destination) && board.countBetween(source, destination) == 0;
    }

    private boolean canKnightMove(Board board, Position source, Position destination) {
        int dx = destination.x() - source.x();
        int dy = destination.y() - source.y();
        if (!((Math.abs(dx) == 1 && Math.abs(dy) == 2) || (Math.abs(dx) == 2 && Math.abs(dy) == 1))) {
            return false;
        }
        Position leg = Math.abs(dx) == 2
                ? new Position(source.x() + Integer.signum(dx), source.y())
                : new Position(source.x(), source.y() + Integer.signum(dy));
        return board.isEmpty(leg);
    }

    private boolean canCannonMove(Board board, Position source, Position destination, boolean isCapture) {
        if (!sameLine(source, destination)) {
            return false;
        }
        int between = board.countBetween(source, destination);
        return isCapture ? between == 1 : between == 0;
    }

    private boolean canPawnMove(Position source, Position destination, PlayerColor color) {
        int dx = destination.x() - source.x();
        int dy = destination.y() - source.y();
        if (dx == 0 && dy == color.forwardDirection()) {
            return true;
        }
        boolean crossedRiver = color == PlayerColor.RED ? source.y() >= 5 : source.y() <= 4;
        return crossedRiver && Math.abs(dx) == 1 && dy == 0;
    }

    private boolean canGuardMove(Position source, Position destination, boolean visible, PlayerColor color) {
        boolean diagonalOne = Math.abs(destination.x() - source.x()) == 1
                && Math.abs(destination.y() - source.y()) == 1;
        return diagonalOne && (visible || inPalace(destination, color));
    }

    private boolean canBishopMove(Board board, Position source, Position destination, boolean visible, PlayerColor color) {
        int dx = destination.x() - source.x();
        int dy = destination.y() - source.y();
        if (Math.abs(dx) != 2 || Math.abs(dy) != 2) {
            return false;
        }
        if (!visible) {
            boolean staysOwnSide = color == PlayerColor.RED ? destination.y() <= 4 : destination.y() >= 5;
            if (!staysOwnSide) {
                return false;
            }
        }
        Position eye = new Position(source.x() + dx / 2, source.y() + dy / 2);
        return board.isEmpty(eye);
    }

    private boolean kingsFace(Board board) {
        Position redKing = board.findKing(PlayerColor.RED);
        Position blackKing = board.findKing(PlayerColor.BLACK);
        if (redKing == null || blackKing == null || redKing.x() != blackKing.x()) {
            return false;
        }
        return board.countBetween(redKing, blackKing) == 0;
    }

    private boolean sameLine(Position a, Position b) {
        return a.x() == b.x() || a.y() == b.y();
    }

    private int distance(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private boolean inPalace(Position position, PlayerColor color) {
        boolean xOk = position.x() >= 3 && position.x() <= 5;
        boolean yOk = color == PlayerColor.RED
                ? position.y() >= 0 && position.y() <= 2
                : position.y() >= 7 && position.y() <= 9;
        return xOk && yOk;
    }
}
