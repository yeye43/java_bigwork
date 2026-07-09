package edu.jieqi.engine;

import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    public static final long TURN_LIMIT_MILLIS = 5 * 60_000;
    private static final int DRAW_AFTER_NON_CAPTURE_PLIES = 80;
    private static final int MAX_REPEATED_TACTICAL_PLIES = 6;

    private final Board board;
    private final RuleEngine ruleEngine;
    private final MoveGenerator moveGenerator;
    private final Map<PlayerColor, Integer> consecutiveChecks = new EnumMap<>(PlayerColor.class);
    private final Map<PlayerColor, Integer> consecutiveChases = new EnumMap<>(PlayerColor.class);
    private final Map<PlayerColor, Integer> recentCheckFatigue = new EnumMap<>(PlayerColor.class);
    private final Map<PlayerColor, Map<String, Integer>> repeatedCheckStates = new EnumMap<>(PlayerColor.class);
    private final Map<PlayerColor, Map<String, Integer>> repeatedChaseStates = new EnumMap<>(PlayerColor.class);

    private PlayerColor currentPlayer;
    private GameStatus status = GameStatus.PLAYING;
    private int nonCapturePlies = 0;
    private final long gameStartTime = System.currentTimeMillis();
    private long gameEndTime = 0;
    private long turnStartTime = System.currentTimeMillis();

    public Game() {
        this(PlayerColor.RED);
    }

    public Game(PlayerColor firstPlayer) {
        this.board = Board.initialBoard();
        this.ruleEngine = new RuleEngine();
        this.moveGenerator = new MoveGenerator(ruleEngine);
        this.currentPlayer = firstPlayer;
        consecutiveChecks.put(PlayerColor.RED, 0);
        consecutiveChecks.put(PlayerColor.BLACK, 0);
        consecutiveChases.put(PlayerColor.RED, 0);
        consecutiveChases.put(PlayerColor.BLACK, 0);
        recentCheckFatigue.put(PlayerColor.RED, 0);
        recentCheckFatigue.put(PlayerColor.BLACK, 0);
        repeatedCheckStates.put(PlayerColor.RED, new HashMap<>());
        repeatedCheckStates.put(PlayerColor.BLACK, new HashMap<>());
        repeatedChaseStates.put(PlayerColor.RED, new HashMap<>());
        repeatedChaseStates.put(PlayerColor.BLACK, new HashMap<>());
    }

    public Board board() {
        return board;
    }

    public PlayerColor currentPlayer() {
        return currentPlayer;
    }

    public GameStatus status() {
        return status;
    }

    public long turnStartTime() {
        return turnStartTime;
    }

    public long gameStartTime() {
        return gameStartTime;
    }

    public long gameEndTime() {
        return gameEndTime;
    }

    public long gameElapsedMillis(long now) {
        return (gameEndTime > 0 ? gameEndTime : now) - gameStartTime;
    }

    public MoveResult checkTimeout() {
        if (status != GameStatus.PLAYING) {
            return null;
        }
        if (System.currentTimeMillis() - turnStartTime <= TURN_LIMIT_MILLIS) {
            return null;
        }
        setStatus(winStatus(currentPlayer.opponent()));
        return MoveResult.success(currentPlayer.displayName() + "超时，"
                + currentPlayer.opponent().displayName() + "获胜。", false, true);
    }

    public List<Move> currentActions() {
        return moveGenerator.generateActions(board, currentPlayer, turnStartTime);
    }

    public int repeatedTacticalRisk(Move move, PlayerColor color) {
        return Math.max(repeatedCheckRisk(move, color), repeatedChaseRisk(move, color));
    }

    public int repeatedCheckRisk(Move move, PlayerColor color) {
        if (status != GameStatus.PLAYING || color != currentPlayer
                || !ruleEngine.canMove(board, move.source(), move.destination(), color)) {
            return 0;
        }
        Board copied = board.copy();
        Piece mover = copied.remove(move.source());
        copied.set(move.destination(), mover);
        if (mover != null && !mover.visible()) {
            mover.reveal();
        }
        PlayerColor opponent = color.opponent();
        if (!ruleEngine.isInCheck(copied, opponent) || !moveGenerator.hasCheckEscape(copied, opponent)) {
            return 0;
        }
        String signature = "CHECK|" + publicPositionKey(copied);
        int consecutive = consecutiveChecks.get(color);
        int fatigue = recentCheckFatigue.get(color);
        int repeated = repeatedCheckStates.get(color).getOrDefault(signature, 0);
        int risk = 0;
        if (fatigue >= MAX_REPEATED_TACTICAL_PLIES) {
            risk = 520_000 + fatigue * 45_000 + repeated * 70_000;
        } else if (fatigue >= MAX_REPEATED_TACTICAL_PLIES - 1) {
            risk = 260_000 + fatigue * 36_000 + repeated * 60_000;
        } else if (fatigue >= 3) {
            risk = 90_000 + fatigue * 24_000 + repeated * 45_000;
        } else if (fatigue >= 2) {
            risk = 48_000 + fatigue * 18_000 + repeated * 32_000;
        }
        if (consecutive >= MAX_REPEATED_TACTICAL_PLIES && repeated > 0) {
            risk = Math.max(risk, 900_000);
        }
        if (consecutive >= MAX_REPEATED_TACTICAL_PLIES - 1) {
            risk = Math.max(risk, repeated > 0 ? 760_000 : 420_000);
        }
        if (consecutive >= 4) {
            risk = Math.max(risk, 300_000 + repeated * 80_000);
        }
        if (consecutive >= 3) {
            risk = Math.max(risk, 130_000 + repeated * 45_000);
        }
        if (consecutive >= 2 || repeated > 0) {
            risk = Math.max(risk, 42_000 + consecutive * 18_000 + repeated * 32_000);
        }
        Piece captured = board.get(move.destination());
        if (captured != null) {
            int capturedValue = value(knownType(captured));
            if (capturedValue >= value(PieceType.KNIGHT)) {
                risk /= 5;
            } else if (capturedValue >= value(PieceType.GUARD)) {
                risk /= 2;
            }
        }
        return risk;
    }

    public int repeatedChaseRisk(Move move, PlayerColor color) {
        if (status != GameStatus.PLAYING || color != currentPlayer
                || !ruleEngine.canMove(board, move.source(), move.destination(), color)) {
            return 0;
        }
        Board copied = board.copy();
        Piece mover = copied.remove(move.source());
        copied.set(move.destination(), mover);
        if (mover != null && !mover.visible()) {
            mover.reveal();
        }
        PlayerColor opponent = color.opponent();
        if (ruleEngine.isInCheck(copied, opponent) || mover == null) {
            return 0;
        }
        PieceType moverKnownType = knownType(board.get(move.source()));
        String targets = chaseTargets(copied, move.destination(), color);
        if (targets.isEmpty()) {
            return 0;
        }
        String signature = "CHASE|" + targets + "|" + publicPositionKey(copied);
        int consecutive = consecutiveChases.get(color);
        int repeated = repeatedChaseStates.get(color).getOrDefault(signature, 0);
        if (consecutive >= MAX_REPEATED_TACTICAL_PLIES && repeated > 0) {
            return moverKnownType == PieceType.PAWN ? 240_000 : 900_000;
        }
        if (consecutive >= MAX_REPEATED_TACTICAL_PLIES - 1 && repeated > 0) {
            return moverKnownType == PieceType.PAWN ? 70_000 : 180_000;
        }
        if (consecutive >= 3 || repeated > 0) {
            int base = moverKnownType == PieceType.PAWN ? 10_000 : 25_000;
            return base + consecutive * 3_000 + repeated * 6_000;
        }
        return 0;
    }

    public MoveResult adjudicateNoLegalResponseForCurrentPlayer() {
        if (status != GameStatus.PLAYING) {
            return null;
        }
        boolean currentInCheck = ruleEngine.isInCheck(board, currentPlayer);
        if (currentInCheck && !moveGenerator.hasCheckEscape(board, currentPlayer)) {
            setStatus(winStatus(currentPlayer.opponent()));
            return MoveResult.success(currentPlayer.displayName() + "方被将死，"
                    + currentPlayer.opponent().displayName() + "获胜。", false, true);
        }
        if (!currentInCheck && !moveGenerator.hasAnyAction(board, currentPlayer)) {
            setStatus(winStatus(currentPlayer.opponent()));
            return MoveResult.success(currentPlayer.displayName() + "方困毙，"
                    + currentPlayer.opponent().displayName() + "获胜。", false, true);
        }
        return null;
    }

    public MoveResult play(Move move) {
        if (status != GameStatus.PLAYING) {
            return MoveResult.failure("对局已经结束。");
        }
        MoveResult timeout = checkTimeout();
        if (timeout != null) {
            return timeout;
        }
        if (move.flipOnly()) {
            return MoveResult.failure("新规则不允许原地翻子，暗子只能在移动或吃子后翻开。");
        }
        return movePiece(move);
    }

    public MoveResult resign() {
        return resign(currentPlayer);
    }

    public MoveResult resign(PlayerColor loser) {
        if (status != GameStatus.PLAYING) {
            return MoveResult.failure("对局已经结束。");
        }
        setStatus(winStatus(loser.opponent()));
        return MoveResult.success(loser.displayName() + "认输，"
                + loser.opponent().displayName() + "获胜。", false, true);
    }

    private MoveResult movePiece(Move move) {
        Piece mover = board.get(move.source());
        if (mover == null) {
            return MoveResult.failure("原位置没有棋子。");
        }
        if (mover.color() != currentPlayer) {
            return MoveResult.failure("现在轮到" + currentPlayer.displayName() + "方。");
        }
        if (!ruleEngine.canMove(board, move.source(), move.destination(), currentPlayer)) {
            return MoveResult.failure("非法走法。");
        }
        if (!ruleEngine.canMoveAndKeepKingSafe(board, move.source(), move.destination(), currentPlayer)) {
            return MoveResult.failure("非法走法：不能走出使自己被将军的棋。");
        }

        Piece captured = board.get(move.destination());
        boolean capture = captured != null;
        boolean moverWasHidden = !mover.visible();
        board.remove(move.source());
        board.set(move.destination(), mover);
        if (!mover.visible()) {
            mover.reveal();
        }

        if (capture) {
            nonCapturePlies = 0;
        } else {
            nonCapturePlies++;
        }

        if (captured != null && captured.type() == PieceType.KING) {
            setStatus(winStatus(currentPlayer));
            return MoveResult.success(currentPlayer.displayName() + "吃掉对方将帅，获胜。", true, true,
                    moverWasHidden ? mover.type() : null);
        }
        if (nonCapturePlies >= DRAW_AFTER_NON_CAPTURE_PLIES) {
            setStatus(GameStatus.DRAW);
            return MoveResult.success("双方连续40回合无吃子，判和。", capture, true,
                    moverWasHidden ? mover.type() : null);
        }

        String message = currentPlayer.displayName() + "从 " + move.source().notation()
                + " 走到 " + move.destination().notation();
        if (capture) {
            message += "，吃掉" + captured.type().displayName(captured.color());
        }
        boolean opponentInCheck = ruleEngine.isInCheck(board, currentPlayer.opponent());
        if (opponentInCheck) {
            message += "，将军";
        }

        MoveResult ending = checkEndingAfterMove(message + "。", capture);
        if (ending != null) {
            return withFlipResultIfNeeded(ending, moverWasHidden ? mover.type() : null);
        }

        MoveResult tacticalEnding = updateTacticalCounters(move.destination(), message + "。", capture);
        if (tacticalEnding != null) {
            return withFlipResultIfNeeded(tacticalEnding, moverWasHidden ? mover.type() : null);
        }

        finishTurn();
        return MoveResult.success(message + "。", capture, false, moverWasHidden ? mover.type() : null);
    }

    private MoveResult withFlipResultIfNeeded(MoveResult result, PieceType flipResult) {
        if (flipResult == null || result.flipResult() != null) {
            return result;
        }
        return MoveResult.success(result.message(), result.capture(), result.gameOver(), flipResult);
    }

    private MoveResult checkEndingAfterMove(String message, boolean capture) {
        PlayerColor opponent = currentPlayer.opponent();
        boolean opponentInCheck = ruleEngine.isInCheck(board, opponent);
        if (opponentInCheck && !moveGenerator.hasCheckEscape(board, opponent)) {
            setStatus(winStatus(currentPlayer));
            return MoveResult.success(message + currentPlayer.displayName() + "将死对方，获胜。", capture, true);
        }
        if (!opponentInCheck && !moveGenerator.hasAnyAction(board, opponent)) {
            setStatus(winStatus(currentPlayer));
            return MoveResult.success(message + opponent.displayName() + "方困毙，"
                    + currentPlayer.displayName() + "获胜。", capture, true);
        }
        return null;
    }

    private MoveResult updateTacticalCounters(Position movedTo, String message, boolean capture) {
        PlayerColor opponent = currentPlayer.opponent();
        if (ruleEngine.isInCheck(board, opponent)) {
            int count = increase(consecutiveChecks, currentPlayer);
            recentCheckFatigue.put(currentPlayer, Math.min(12, recentCheckFatigue.get(currentPlayer) + 1));
            consecutiveChases.put(currentPlayer, 0);
            repeatedChaseStates.get(currentPlayer).clear();
            String signature = "CHECK|" + publicPositionKey(board);
            int repeated = increase(repeatedCheckStates.get(currentPlayer), signature);
            if (count > MAX_REPEATED_TACTICAL_PLIES && repeated > 1) {
                setStatus(winStatus(opponent));
                return MoveResult.success(message + currentPlayer.displayName()
                        + "方连续用对方可化解的将军重复逼迫超过6回合，按长将规则判负。"
                        + opponent.displayName() + "方获胜。", capture, true);
            }
            return null;
        }

        Piece mover = board.get(movedTo);
        if (mover != null && attacksOpponentNonKing(movedTo, currentPlayer)) {
            int count = increase(consecutiveChases, currentPlayer);
            consecutiveChecks.put(currentPlayer, 0);
            decayCheckFatigue(currentPlayer);
            repeatedCheckStates.get(currentPlayer).clear();
            String signature = "CHASE|" + chaseTargets(movedTo, currentPlayer) + "|" + publicPositionKey(board);
            int repeated = increase(repeatedChaseStates.get(currentPlayer), signature);
            if (count > MAX_REPEATED_TACTICAL_PLIES && repeated > 1) {
                if (mover.type() == PieceType.PAWN) {
                    setStatus(GameStatus.DRAW);
                    return MoveResult.success(message + "兵卒长捉超过6回合，判和。", capture, true);
                }
                setStatus(winStatus(opponent));
                return MoveResult.success(message + currentPlayer.displayName()
                        + "方连续捉子超过6回合，判负。"
                        + opponent.displayName() + "方获胜。", capture, true);
            }
            return null;
        }

        resetTacticalCounters(currentPlayer);
        return null;
    }

    private boolean attacksOpponentNonKing(Position source, PlayerColor attacker) {
        return !chaseTargets(source, attacker).isEmpty();
    }

    private String chaseTargets(Position source, PlayerColor attacker) {
        return chaseTargets(board, source, attacker);
    }

    private String chaseTargets(Board targetBoard, Position source, PlayerColor attacker) {
        StringBuilder targets = new StringBuilder();
        for (Position target : targetBoard.occupiedPositions()) {
            Piece piece = targetBoard.get(target);
            if (piece == null || piece.color() == attacker || knownType(piece) == PieceType.KING) {
                continue;
            }
            if (ruleEngine.canMove(targetBoard, source, target, attacker)) {
                targets.append(target.notation()).append(":").append(knownType(piece).name()).append(",");
            }
        }
        return targets.toString();
    }

    private String publicPositionKey(Board targetBoard) {
        StringBuilder sb = new StringBuilder();
        for (int y = 0; y < Position.HEIGHT; y++) {
            for (int x = 0; x < Position.WIDTH; x++) {
                Piece piece = targetBoard.get(new Position(x, y));
                if (piece == null) {
                    sb.append(".");
                } else if (piece.visible()) {
                    sb.append(piece.key());
                } else {
                    sb.append(piece.color().name())
                            .append(":HIDDEN:")
                            .append(piece.hiddenMoveType().name())
                            .append(":0");
                }
                sb.append("|");
            }
        }
        return sb.toString();
    }

    private PieceType knownType(Piece piece) {
        return piece.visible() ? piece.type() : piece.hiddenMoveType();
    }

    private int value(PieceType type) {
        return switch (type) {
            case KING -> 100_000;
            case ROOK -> 950;
            case CANNON -> 520;
            case KNIGHT -> 450;
            case GUARD -> 220;
            case BISHOP -> 200;
            case PAWN -> 120;
        };
    }

    private int increase(Map<PlayerColor, Integer> counters, PlayerColor color) {
        int next = counters.get(color) + 1;
        counters.put(color, next);
        return next;
    }

    private int increase(Map<String, Integer> counters, String key) {
        int next = counters.getOrDefault(key, 0) + 1;
        counters.put(key, next);
        return next;
    }

    private void resetTacticalCounters(PlayerColor color) {
        consecutiveChecks.put(color, 0);
        consecutiveChases.put(color, 0);
        decayCheckFatigue(color);
        repeatedCheckStates.get(color).clear();
        repeatedChaseStates.get(color).clear();
    }

    private void decayCheckFatigue(PlayerColor color) {
        recentCheckFatigue.put(color, Math.max(0, recentCheckFatigue.get(color) - 1));
    }

    private void finishTurn() {
        currentPlayer = currentPlayer.opponent();
        turnStartTime = System.currentTimeMillis();
    }

    private GameStatus winStatus(PlayerColor winner) {
        return winner == PlayerColor.RED ? GameStatus.RED_WIN : GameStatus.BLACK_WIN;
    }

    private void setStatus(GameStatus status) {
        this.status = status;
        if (status != GameStatus.PLAYING && gameEndTime == 0) {
            gameEndTime = System.currentTimeMillis();
        }
    }
}
