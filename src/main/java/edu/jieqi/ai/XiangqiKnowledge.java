package edu.jieqi.ai;

import edu.jieqi.engine.Board;
import edu.jieqi.engine.MoveGenerator;
import edu.jieqi.engine.RuleEngine;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

public class XiangqiKnowledge {
    public int score(Board board, PlayerColor color, RuleEngine rules, MoveGenerator generator) {
        int phase = endgamePhase(board);
        int score = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            int side = piece.color() == color ? 1 : -1;
            // Higher hidden weight in early/mid game — hidden pieces have significant potential value
            int visibleWeight = piece.visible() ? 100 : (60 + (100 - phase) / 4);
            int pieceScore = pieceSquare(piece, position, phase);
            pieceScore += structure(board, piece, position, phase);
            score += side * pieceScore * visibleWeight / 100;
        }
        score += kingHunt(board, color, phase, rules);
        score -= kingHunt(board, color.opponent(), phase, rules);
        score += openLineControl(board, color, rules);
        score -= openLineControl(board, color.opponent(), rules);
        score += defensiveShape(board, color, phase);
        score -= defensiveShape(board, color.opponent(), phase);
        score += cannonAndRookThreats(board, color);
        score -= cannonAndRookThreats(board, color.opponent());
        score += pinPressure(board, color);
        score -= pinPressure(board, color.opponent());
        score += knightFreedom(board, color, rules);
        score -= knightFreedom(board, color.opponent(), rules);
        score += (generator.generateActions(board, color, 0).size()
                - generator.generateActions(board, color.opponent(), 0).size()) * (2 + phase / 35);
        // Bonus for hidden pieces near enemy king in midgame — potential for surprise attacks
        score += hiddenPieceInvasionPressure(board, color, phase);
        score -= hiddenPieceInvasionPressure(board, color.opponent(), phase);
        return score;
    }

    private int pieceSquare(Piece piece, Position position, int phase) {
        int fileCenter = 4 - Math.abs(position.x() - 4);
        int rank = forwardRank(piece.color(), position);
        int center = fileCenter * 6 + Math.max(0, 5 - Math.abs(rank - 5)) * 4;
        return switch (knownType(piece)) {
            case ROOK -> center + rank * 5 + phase * 2;
            case CANNON -> center + (rank >= 4 ? 26 : 10) + (100 - phase) / 4;
            case KNIGHT -> center + (rank >= 3 && rank <= 7 ? 26 : 4) + phase / 3;
            case PAWN -> pawnSquare(rank, fileCenter, phase);
            case GUARD -> guardSquare(piece.color(), position, phase);
            case BISHOP -> bishopSquare(piece.color(), position, phase);
            case KING -> kingSquare(piece.color(), position, phase);
        };
    }

    private int pawnSquare(int rank, int fileCenter, int phase) {
        int crossed = rank >= 5 ? 70 : 0;
        int advanced = rank * (13 + phase / 6);
        return crossed + advanced + fileCenter * (rank >= 5 ? 10 : 3);
    }

    private int guardSquare(PlayerColor color, Position position, int phase) {
        return inPalace(position, color) ? 45 + phase / 2 : 12;
    }

    private int bishopSquare(PlayerColor color, Position position, int phase) {
        boolean ownSide = color == PlayerColor.RED ? position.y() <= 4 : position.y() >= 5;
        return ownSide ? 36 + phase / 3 : 22 + phase / 2;
    }

    private int kingSquare(PlayerColor color, Position position, int phase) {
        int palaceCenter = Math.abs(position.x() - 4);
        int homeRank = color == PlayerColor.RED ? position.y() : 9 - position.y();
        return phase < 45 ? -(palaceCenter * 12 + homeRank * 8) : -palaceCenter * 8;
    }

    private int structure(Board board, Piece piece, Position position, int phase) {
        PieceType type = knownType(piece);
        if (type == PieceType.PAWN) {
            return connectedPawnBonus(board, piece, position) + passedPawnBonus(board, piece, position, phase);
        }
        if (type == PieceType.ROOK || type == PieceType.CANNON) {
            return lineMobilityBonus(board, position);
        }
        return 0;
    }

    private int connectedPawnBonus(Board board, Piece pawn, Position position) {
        int bonus = 0;
        for (int dx : new int[]{-1, 1}) {
            int x = position.x() + dx;
            if (Position.isInside(x, position.y())) {
                Piece neighbor = board.get(new Position(x, position.y()));
                if (neighbor != null && neighbor.color() == pawn.color() && knownType(neighbor) == PieceType.PAWN) {
                    bonus += 18;
                }
            }
        }
        return bonus;
    }

    private int passedPawnBonus(Board board, Piece pawn, Position position, int phase) {
        int direction = pawn.color().forwardDirection();
        for (int y = position.y() + direction; y >= 0 && y < Position.HEIGHT; y += direction) {
            Piece blocker = board.get(new Position(position.x(), y));
            if (blocker != null && blocker.color() != pawn.color()) {
                return 0;
            }
        }
        return 35 + phase;
    }

    private int lineMobilityBonus(Board board, Position position) {
        int bonus = 0;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] direction : directions) {
            int x = position.x() + direction[0];
            int y = position.y() + direction[1];
            while (Position.isInside(x, y) && board.isEmpty(new Position(x, y))) {
                bonus += 4;
                x += direction[0];
                y += direction[1];
            }
        }
        return bonus;
    }

    private int kingHunt(Board board, PlayerColor attacker, int phase, RuleEngine rules) {
        Position king = board.findKing(attacker.opponent());
        if (king == null) {
            return 250_000;
        }
        int pressure = 0;
        int attackers = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != attacker || !piece.visible()) {
                continue;
            }
            int distance = Math.abs(source.x() - king.x()) + Math.abs(source.y() - king.y());
            int unit = switch (piece.type()) {
                case ROOK -> 26;
                case CANNON -> 21;
                case KNIGHT -> 18;
                case PAWN -> 9;
                case GUARD, BISHOP -> 3;
                case KING -> 0;
            };
            pressure += Math.max(0, 12 - distance) * unit * (60 + phase) / 100;
            if (rules.canMove(board, source, king, attacker)) {
                attackers++;
            }
        }
        if (attackers >= 2) {
            pressure += 260 + phase * 4;
        } else if (attackers == 1) {
            pressure += 110 + phase * 2;
        }
        return pressure;
    }

    private int openLineControl(Board board, PlayerColor color, RuleEngine rules) {
        Position enemyKing = board.findKing(color.opponent());
        if (enemyKing == null) {
            return 200_000;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || !piece.visible()) {
                continue;
            }
            if ((piece.type() == PieceType.ROOK || piece.type() == PieceType.CANNON)
                    && (source.x() == enemyKing.x() || source.y() == enemyKing.y())) {
                int between = board.countBetween(source, enemyKing);
                if (piece.type() == PieceType.ROOK && between <= 1) {
                    score += between == 0 ? 320 : 90;
                }
                if (piece.type() == PieceType.CANNON && between <= 2) {
                    score += between == 1 ? 260 : 65;
                }
            }
            if (rules.canMove(board, source, enemyKing, color)) {
                score += 180;
            }
        }
        return score;
    }

    private int defensiveShape(Board board, PlayerColor color, int phase) {
        Position king = board.findKing(color);
        if (king == null) {
            return -200_000;
        }
        int score = inPalace(king, color) ? 35 : -180;
        int homeRank = color == PlayerColor.RED ? king.y() : 9 - king.y();
        if (king.x() == 4) {
            score += 24;
        }
        score -= homeRank * (phase < 55 ? 18 : 8);

        int guards = 0;
        int bishops = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color || !piece.visible()) {
                continue;
            }
            if (piece.type() == PieceType.GUARD) {
                guards++;
                score += inPalace(position, color) ? 70 : -45;
                if (Math.abs(position.x() - king.x()) + Math.abs(position.y() - king.y()) <= 2) {
                    score += 24;
                }
            } else if (piece.type() == PieceType.BISHOP) {
                bishops++;
                boolean ownSide = color == PlayerColor.RED ? position.y() <= 4 : position.y() >= 5;
                score += ownSide ? 48 : -25;
            }
        }
        if (guards == 2) {
            score += 45;
        }
        if (bishops == 2) {
            score += 35;
        }
        return score;
    }

    private int cannonAndRookThreats(Board board, PlayerColor attacker) {
        Position king = board.findKing(attacker.opponent());
        if (king == null) {
            return 200_000;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != attacker || !piece.visible()) {
                continue;
            }
            PieceType type = piece.type();
            if (type != PieceType.ROOK && type != PieceType.CANNON) {
                continue;
            }
            if (source.x() != king.x() && source.y() != king.y()) {
                continue;
            }
            int between = board.countBetween(source, king);
            if (type == PieceType.ROOK) {
                if (between == 0) {
                    score += 360;
                } else if (between == 1) {
                    score += 95;
                }
            } else {
                if (between == 1) {
                    score += 420;
                } else if (between == 0) {
                    score += 70;
                } else if (between == 2) {
                    score += 85;
                }
            }
        }
        return score;
    }

    private int pinPressure(Board board, PlayerColor attacker) {
        Position king = board.findKing(attacker.opponent());
        if (king == null) {
            return 200_000;
        }
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != attacker || !piece.visible()) {
                continue;
            }
            PieceType type = piece.type();
            if (type != PieceType.ROOK && type != PieceType.CANNON) {
                continue;
            }
            if (source.x() != king.x() && source.y() != king.y() || board.countBetween(source, king) != 1) {
                continue;
            }
            Piece pinned = singlePieceBetween(board, source, king);
            if (pinned != null && pinned.color() != attacker && pinned.type() != PieceType.KING) {
                score += material(pinned.type()) / (type == PieceType.ROOK ? 3 : 4);
            }
        }
        return score;
    }

    private Piece singlePieceBetween(Board board, Position source, Position destination) {
        int dx = Integer.compare(destination.x(), source.x());
        int dy = Integer.compare(destination.y(), source.y());
        Piece found = null;
        int x = source.x() + dx;
        int y = source.y() + dy;
        while (x != destination.x() || y != destination.y()) {
            Piece piece = board.get(new Position(x, y));
            if (piece != null) {
                if (found != null) {
                    return null;
                }
                found = piece;
            }
            x += dx;
            y += dy;
        }
        return found;
    }

    private int knightFreedom(Board board, PlayerColor color, RuleEngine rules) {
        int score = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color || !piece.visible() || piece.type() != PieceType.KNIGHT) {
                continue;
            }
            int blockedLegs = blockedKnightLegs(board, source);
            int mobility = 0;
            for (int y = 0; y < Position.HEIGHT; y++) {
                for (int x = 0; x < Position.WIDTH; x++) {
                    if (rules.canMove(board, source, new Position(x, y), color)) {
                        mobility++;
                    }
                }
            }
            score += mobility * 18 - blockedLegs * 42;
            if (mobility <= 1) {
                score -= 120;
            }
        }
        return score;
    }

    private int blockedKnightLegs(Board board, Position source) {
        int blocked = 0;
        int[][] legs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] leg : legs) {
            int x = source.x() + leg[0];
            int y = source.y() + leg[1];
            if (Position.isInside(x, y) && !board.isEmpty(new Position(x, y))) {
                blocked++;
            }
        }
        return blocked;
    }

    private int endgamePhase(Board board) {
        int visibleMaterial = 0;
        int visiblePieces = 0;
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece != null && piece.visible() && piece.type() != PieceType.KING) {
                visibleMaterial += material(piece.type());
                visiblePieces++;
            }
        }
        int materialPhase = Math.max(0, 100 - visibleMaterial / 55);
        int piecePhase = Math.max(0, 100 - visiblePieces * 5);
        return Math.min(100, (materialPhase + piecePhase) / 2);
    }

    private int material(PieceType type) {
        return switch (type) {
            case KING -> 0;
            case ROOK -> 950;
            case CANNON -> 520;
            case KNIGHT -> 450;
            case GUARD -> 220;
            case BISHOP -> 200;
            case PAWN -> 120;
        };
    }

    private int forwardRank(PlayerColor color, Position position) {
        return color == PlayerColor.RED ? position.y() : 9 - position.y();
    }

    private PieceType knownType(Piece piece) {
        return piece.visible() ? piece.type() : piece.hiddenMoveType();
    }

    private boolean inPalace(Position position, PlayerColor color) {
        boolean xOk = position.x() >= 3 && position.x() <= 5;
        boolean yOk = color == PlayerColor.RED
                ? position.y() >= 0 && position.y() <= 2
                : position.y() >= 7 && position.y() <= 9;
        return xOk && yOk;
    }

    // In Jieqi, hidden guards and bishops can cross the river — this creates
    // invasion pressure that standard xiangqi evaluation doesn't capture.
    private int hiddenPieceInvasionPressure(Board board, PlayerColor color, int phase) {
        if (phase >= 75) {
            return 0; // late endgame — hidden pieces already accounted for
        }
        int score = 0;
        Position king = board.findKing(color.opponent());
        if (king == null) {
            return 0;
        }
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.get(position);
            if (piece == null || piece.color() != color || piece.visible()) {
                continue;
            }
            PieceType moveType = piece.hiddenMoveType();
            int rank = forwardRank(color, position);
            int fileCenter = 4 - Math.abs(position.x() - 4);
            int distToKing = Math.abs(position.x() - king.x()) + Math.abs(position.y() - king.y());
            int proximityBonus = Math.max(0, 14 - distToKing) * 12;

            // Hidden pieces on the opponent's side are more threatening
            boolean onOpponentSide = color == PlayerColor.RED ? position.y() >= 5 : position.y() <= 4;
            int invasionBonus = onOpponentSide ? 85 + rank * 6 : 0;

            // Specific bonuses based on what the hidden piece could be
            int pieceTypeBonus = switch (moveType) {
                case ROOK -> 180 + proximityBonus + rank * 14;
                case CANNON -> 145 + proximityBonus + rank * 10;
                case KNIGHT -> 120 + proximityBonus;
                case PAWN -> 70 + (rank >= 5 ? rank * 8 : 0);
                case GUARD, BISHOP -> onOpponentSide ? 65 : 15;
                case KING -> 0;
            };

            score += pieceTypeBonus + invasionBonus;
        }
        return score * (110 - phase) / 120;
    }
}
