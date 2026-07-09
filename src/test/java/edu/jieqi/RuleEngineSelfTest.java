package edu.jieqi;

import edu.jieqi.ai.ExperienceMemory;
import edu.jieqi.ai.SearchAi;
import edu.jieqi.engine.Board;
import edu.jieqi.engine.Game;
import edu.jieqi.engine.GameStatus;
import edu.jieqi.engine.MoveGenerator;
import edu.jieqi.engine.RuleEngine;
import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.List;

public class RuleEngineSelfTest {
    private final RuleEngine rules = new RuleEngine();

    public static void main(String[] args) {
        RuleEngineSelfTest test = new RuleEngineSelfTest();
        test.runAll();
        System.out.println("全部自测试通过。");
    }

    private void runAll() {
        rookCannotJump();
        cannonNeedsOneScreenToCapture();
        cannonCannotMoveWithScreen();
        knightLegBlocksMove();
        bishopEyeBlocksMove();
        visibleBishopCanCrossRiverButHiddenBishopCannot();
        visibleGuardCanLeavePalaceButHiddenGuardCannot();
        pawnMovementBeforeAndAfterRiver();
        kingStaysInPalaceExceptFlyingCapture();
        hiddenPieceUsesOriginalSquareRule();
        hiddenPieceRevealAfterSimulation();
        cannotCaptureOwnPiece();
        moveGeneratorIncludesOnlyMove();
        boardKeyTracksHiddenState();
        boardCopyKeepsPositionKey();
        basicMoveAllowsSelfCheckButSafeMoveRejectsIt();
        flyingKingGivesCheck();
        gameRejectsSelfCheckAfterMove();
        gameCanStartWithBlack();
        gameEndsImmediatelyOnCheckmate();
        searchAiReturnsNoMoveWhenCheckmated();
        searchAiPrefersImmediateWin();
        searchAiAvoidsObviousHangingMove();
        searchAiAvoidsRookForPawnTrade();
        searchAiAnswersThreatenedCannon();
        searchAiRecapturesInvadingCannon();
        searchAiDoesNotDonateGuardToBishop();
        searchAiTakesAvailableBishop();
        searchAiPrioritizesNearbyCannonCapture();
        searchAiMovesThreatenedCannon();
        searchAiAvoidsIdleInvadingMajorMove();
        searchAiClearsDangerousHomePawnWithMajor();
        searchAiClearsPawnNearPalaceBeforeMateNet();
        searchAiUsesPawnToSupportKingAttack();
        searchAiAvoidsForcedRepeatedCheckLoss();
        gamePenalizesRepeatedCheckBeforeRuleLoss();
        searchAiAvoidsRookCheckThatKingCanEat();
        searchAiAvoidsCheckThatLosesRookAfterEscape();
        searchAiMovesCannonThreatenedByRookLine();
        searchAiAvoidsPassiveKingDrift();
        searchAiAvoidsBlockingOwnKingEscape();
        searchAiKnownKeyMasksHiddenIdentity();
        gameChaseTargetsMaskHiddenIdentity();
        gameReportsRepeatedChaseRisk();
        experienceMemoryPersistsLearning();
        gameRejectsFlipAndReturnsFlipResultForHiddenMove();
    }

    private void rookCannotJump() {
        Board board = new Board();
        board.set(pos("a0"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("a1"), visible(PlayerColor.RED, PieceType.PAWN));
        check(!rules.canMove(board, pos("a0"), pos("a3"), PlayerColor.RED), "车不能越子");
    }

    private void cannonNeedsOneScreenToCapture() {
        Board board = new Board();
        board.set(pos("b2"), visible(PlayerColor.RED, PieceType.CANNON));
        board.set(pos("b7"), visible(PlayerColor.BLACK, PieceType.ROOK));
        check(!rules.canMove(board, pos("b2"), pos("b7"), PlayerColor.RED), "炮无炮架不能吃子");

        board.set(pos("b5"), visible(PlayerColor.RED, PieceType.PAWN));
        check(rules.canMove(board, pos("b2"), pos("b7"), PlayerColor.RED), "炮隔一个棋子可以吃子");
    }

    private void cannonCannotMoveWithScreen() {
        Board board = new Board();
        board.set(pos("b2"), visible(PlayerColor.RED, PieceType.CANNON));
        board.set(pos("b5"), visible(PlayerColor.RED, PieceType.PAWN));
        check(!rules.canMove(board, pos("b2"), pos("b6"), PlayerColor.RED), "炮移动不能隔子");
    }

    private void knightLegBlocksMove() {
        Board board = new Board();
        board.set(pos("b0"), visible(PlayerColor.RED, PieceType.KNIGHT));
        check(rules.canMove(board, pos("b0"), pos("c2"), PlayerColor.RED), "马腿未堵时可走");
        board.set(pos("b1"), visible(PlayerColor.RED, PieceType.PAWN));
        check(!rules.canMove(board, pos("b0"), pos("c2"), PlayerColor.RED), "蹩马腿时不可走");
    }

    private void bishopEyeBlocksMove() {
        Board board = new Board();
        board.set(pos("c0"), visible(PlayerColor.RED, PieceType.BISHOP));
        check(rules.canMove(board, pos("c0"), pos("e2"), PlayerColor.RED), "象眼未堵时可走");
        board.set(pos("d1"), visible(PlayerColor.RED, PieceType.PAWN));
        check(!rules.canMove(board, pos("c0"), pos("e2"), PlayerColor.RED), "塞象眼时不可走");
    }

    private void visibleBishopCanCrossRiverButHiddenBishopCannot() {
        Board board = new Board();
        board.set(pos("e4"), visible(PlayerColor.RED, PieceType.BISHOP));
        check(rules.canMove(board, pos("e4"), pos("g6"), PlayerColor.RED), "明象可以过河");

        Board hiddenBoard = new Board();
        hiddenBoard.set(pos("e4"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.BISHOP));
        check(!rules.canMove(hiddenBoard, pos("e4"), pos("g6"), PlayerColor.RED), "暗象按原规则不能过河");
    }

    private void visibleGuardCanLeavePalaceButHiddenGuardCannot() {
        Board board = new Board();
        board.set(pos("f2"), visible(PlayerColor.RED, PieceType.GUARD));
        check(rules.canMove(board, pos("f2"), pos("g3"), PlayerColor.RED), "明士可以离开九宫");

        Board hiddenBoard = new Board();
        hiddenBoard.set(pos("f2"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.GUARD));
        check(!rules.canMove(hiddenBoard, pos("f2"), pos("g3"), PlayerColor.RED), "暗士按原规则不能离开九宫");
    }

    private void pawnMovementBeforeAndAfterRiver() {
        Board board = new Board();
        board.set(pos("a3"), visible(PlayerColor.RED, PieceType.PAWN));
        check(rules.canMove(board, pos("a3"), pos("a4"), PlayerColor.RED), "红兵过河前可以向前");
        check(!rules.canMove(board, pos("a3"), pos("b3"), PlayerColor.RED), "红兵过河前不能横走");
        check(!rules.canMove(board, pos("a3"), pos("a2"), PlayerColor.RED), "红兵不能后退");

        Board crossedBoard = new Board();
        crossedBoard.set(pos("a5"), visible(PlayerColor.RED, PieceType.PAWN));
        check(rules.canMove(crossedBoard, pos("a5"), pos("b5"), PlayerColor.RED), "红兵过河后可以横走");

        Board blackBoard = new Board();
        blackBoard.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.PAWN));
        check(rules.canMove(blackBoard, pos("a6"), pos("a5"), PlayerColor.BLACK), "黑卒向下前进");
        check(!rules.canMove(blackBoard, pos("a6"), pos("a7"), PlayerColor.BLACK), "黑卒不能后退");
    }

    private void kingStaysInPalaceExceptFlyingCapture() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        check(rules.canMove(board, pos("e0"), pos("e1"), PlayerColor.RED), "帅可以在九宫内走一格");
        check(!rules.canMove(board, pos("e0"), pos("e3"), PlayerColor.RED), "帅不能普通走出九宫");

        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        check(rules.canMove(board, pos("e0"), pos("e9"), PlayerColor.RED), "将帅照面时可以飞将吃子");
    }

    private void hiddenPieceUsesOriginalSquareRule() {
        Board board = new Board();
        board.set(pos("b2"), hidden(PlayerColor.RED, PieceType.PAWN, PieceType.CANNON));
        board.set(pos("b5"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("b7"), visible(PlayerColor.BLACK, PieceType.ROOK));
        check(rules.canMove(board, pos("b2"), pos("b7"), PlayerColor.RED),
                "b2 暗子真实身份是兵，也应按炮位规则吃子");
    }

    private void hiddenPieceRevealAfterSimulation() {
        Board board = new Board();
        board.set(pos("b2"), hidden(PlayerColor.RED, PieceType.PAWN, PieceType.CANNON));
        MoveGenerator generator = new MoveGenerator(rules);
        List<Move> actions = generator.generateActions(board, PlayerColor.RED, System.currentTimeMillis());
        check(!actions.isEmpty(), "普通走法生成应包含暗子的动作");
        check(!board.get(pos("b2")).visible(), "模拟走法不能改变原棋盘暗子状态");
    }

    private void cannotCaptureOwnPiece() {
        Board board = new Board();
        board.set(pos("a0"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("a3"), visible(PlayerColor.RED, PieceType.PAWN));
        check(!rules.canMove(board, pos("a0"), pos("a3"), PlayerColor.RED), "不能吃己方棋子");
    }

    private void moveGeneratorIncludesOnlyMove() {
        Board board = new Board();
        board.set(pos("a0"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.ROOK));
        MoveGenerator generator = new MoveGenerator(rules);
        List<String> actions = generator.generateActions(board, PlayerColor.RED, 0)
                .stream()
                .map(Move::notation)
                .toList();
        check(!actions.contains("flip a0"), "新规则不应生成原地翻子动作");
        check(actions.contains("move a0 a1"), "暗子应生成按原始车位移动动作");
    }

    private void boardKeyTracksHiddenState() {
        Board board = new Board();
        board.set(pos("a0"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.ROOK));
        String hiddenKey = board.positionKey();
        board.get(pos("a0")).reveal();
        String visibleKey = board.positionKey();
        check(!hiddenKey.equals(visibleKey), "局面签名应区分明暗状态");
    }

    private void boardCopyKeepsPositionKey() {
        Board board = new Board();
        board.set(pos("a0"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.ROOK));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        Board copied = board.copy();
        check(board.positionKey().equals(copied.positionKey()), "复制棋盘应保持相同局面签名");
        copied.get(pos("a0")).reveal();
        check(!board.positionKey().equals(copied.positionKey()), "复制棋盘修改后不应影响原棋盘");
    }

    private void basicMoveAllowsSelfCheckButSafeMoveRejectsIt() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e1"), visible(PlayerColor.RED, PieceType.ROOK));
        check(rules.canMove(board, pos("e1"), pos("a1"), PlayerColor.RED),
                "基础走法按作业要求不禁止不应将");
        check(!rules.canMoveAndKeepKingSafe(board, pos("e1"), pos("a1"), PlayerColor.RED),
                "安全走法应拒绝走后将帅照面的着法");
    }

    private void flyingKingGivesCheck() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        check(rules.isInCheck(board, PlayerColor.RED), "将帅照面时红方应被判为被将军");
        check(rules.isInCheck(board, PlayerColor.BLACK), "将帅照面时黑方也应被判为被将军");
    }

    private void gameRejectsSelfCheckAfterMove() {
        Game game = new Game();
        Board board = game.board();
        for (Position position : board.occupiedPositions()) {
            board.remove(position);
        }
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e1"), visible(PlayerColor.RED, PieceType.ROOK));

        MoveResult result = game.play(Move.move(pos("e1"), pos("a1"), game.turnStartTime()));
        check(!result.success(), "不应将走法应判非法");
        check(result.message().contains("不能走出使自己被将军"), "不应将走法应说明原因");
    }

    private void gameCanStartWithBlack() {
        Game game = new Game(PlayerColor.BLACK);
        check(game.currentPlayer() == PlayerColor.BLACK, "应支持黑方作为本局先手");
        MoveResult redResult = game.play(Move.move(pos("a0"), pos("a1"), game.turnStartTime()));
        check(!redResult.success(), "黑方先手时红方不能先走");
        MoveResult blackResult = game.play(Move.move(pos("a9"), pos("a8"), game.turnStartTime()));
        check(blackResult.success(), "黑方先手时黑方应能走第一步");
    }

    private void gameEndsImmediatelyOnCheckmate() {
        Game game = new Game();
        Board board = game.board();
        for (Position position : board.occupiedPositions()) {
            board.remove(position);
        }
        board.set(pos("a0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e7"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("d9"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("d8"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("f9"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("f8"), visible(PlayerColor.RED, PieceType.ROOK));

        MoveResult result = game.play(Move.move(pos("e7"), pos("e8"), game.turnStartTime()));
        check(result.success(), "将死走法应成功");
        check(result.gameOver(), "将死后应立即结束");
        check(game.status() == GameStatus.RED_WIN, "红方将死黑方后应判红胜");
    }

    private void searchAiReturnsNoMoveWhenCheckmated() {
        Board board = new Board();
        board.set(pos("a0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e8"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("d9"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("d8"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("f9"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("f8"), visible(PlayerColor.RED, PieceType.ROOK));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 300);
        check(move == null, "AI被将死时应返回无可走动作");
    }

    private void searchAiPrefersImmediateWin() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e1"), visible(PlayerColor.BLACK, PieceType.ROOK));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 300);
        check(move != null, "搜索AI应能找到走法");
        check(move.source().equals(pos("e1")) && move.destination().equals(pos("e0")),
                "搜索AI应优先选择一步吃帅获胜");
    }

    private void searchAiAvoidsObviousHangingMove() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a9"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("a8"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("a0"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("b0"), visible(PlayerColor.RED, PieceType.ROOK));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 800);
        check(move != null, "搜索AI应能找到走法");
        check(!(move.source().equals(pos("a9")) && move.destination().equals(pos("a0"))),
                "搜索AI不应贪吃兵而让车马上被红车吃掉");
    }

    private void searchAiAvoidsRookForPawnTrade() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("a9"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("a6"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("b6"), visible(PlayerColor.RED, PieceType.ROOK));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1000);
        check(move != null, "搜索AI应能找到走法");
        check(!(move.source().equals(pos("a9")) && move.destination().equals(pos("a6"))),
                "搜索AI不应用车吃被保护的兵导致车换兵");
    }

    private void searchAiAnswersThreatenedCannon() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a4"), visible(PlayerColor.RED, PieceType.CANNON));
        board.set(pos("b1"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("b7"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("c4"), visible(PlayerColor.RED, PieceType.GUARD));
        board.set(pos("a7"), visible(PlayerColor.BLACK, PieceType.CANNON));
        board.set(pos("a6"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.PAWN));
        board.set(pos("g9"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.BISHOP));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move when cannon is threatened");
        check(!(move.source().equals(pos("g9")) && move.destination().equals(pos("e7"))),
                "AI should not ignore a threatened cannon for an unrelated reveal");
    }

    private void searchAiRecapturesInvadingCannon() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a7"), visible(PlayerColor.RED, PieceType.CANNON));
        board.set(pos("b1"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("b7"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("c4"), visible(PlayerColor.RED, PieceType.GUARD));
        board.set(pos("a6"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.PAWN));
        board.set(pos("a9"), hidden(PlayerColor.BLACK, PieceType.BISHOP, PieceType.ROOK));
        board.set(pos("e8"), visible(PlayerColor.BLACK, PieceType.KNIGHT));
        board.set(pos("e7"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("g9"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.BISHOP));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move after cannon invasion");
        check(move.source().equals(pos("a9")) && move.destination().equals(pos("a7")),
                "AI should recapture an invading cannon instead of choosing " + move.notation());
    }

    private void searchAiDoesNotDonateGuardToBishop() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e4"), visible(PlayerColor.RED, PieceType.BISHOP));
        board.set(pos("b6"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("h6"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("h7"), visible(PlayerColor.BLACK, PieceType.GUARD));
        board.set(pos("g9"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.BISHOP));
        board.set(pos("e7"), visible(PlayerColor.BLACK, PieceType.PAWN));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move when guard can be donated");
        check(!(move.source().equals(pos("h7")) && move.destination().equals(pos("g6"))),
                "AI should not move a guard onto a bishop capture square");
    }

    private void searchAiTakesAvailableBishop() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("f9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e8"), visible(PlayerColor.RED, PieceType.BISHOP));
        board.set(pos("b6"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("c4"), visible(PlayerColor.RED, PieceType.GUARD));
        board.set(pos("d9"), hidden(PlayerColor.BLACK, PieceType.ROOK, PieceType.GUARD));
        board.set(pos("c9"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.BISHOP));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move when bishop can be captured");
        check(move.source().equals(pos("d9")) && move.destination().equals(pos("e8")),
                "AI should take the available bishop instead of choosing " + move.notation());
    }

    private void searchAiPrioritizesNearbyCannonCapture() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("f9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a8"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("e7"), visible(PlayerColor.RED, PieceType.CANNON));
        board.set(pos("h6"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("b9"), visible(PlayerColor.BLACK, PieceType.GUARD));
        board.set(pos("e8"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("f8"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("c7"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("i5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("c4"), visible(PlayerColor.RED, PieceType.GUARD));
        board.set(pos("i3"), hidden(PlayerColor.RED, PieceType.PAWN, PieceType.PAWN));
        board.set(pos("c0"), hidden(PlayerColor.RED, PieceType.BISHOP, PieceType.BISHOP));
        board.set(pos("d0"), hidden(PlayerColor.RED, PieceType.GUARD, PieceType.GUARD));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move when cannon and knight can be captured");
        check(move.source().equals(pos("e8")) && move.destination().equals(pos("e7")),
                "AI should remove the nearby cannon instead of choosing " + move.notation());
    }

    private void searchAiMovesThreatenedCannon() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("f9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a1"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("b8"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.CANNON));
        board.set(pos("c5"), visible(PlayerColor.BLACK, PieceType.KNIGHT));
        board.set(pos("d9"), hidden(PlayerColor.BLACK, PieceType.ROOK, PieceType.GUARD));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move for a threatened cannon");
        check(move.source().equals(pos("a6")),
                "AI should handle the threatened cannon instead of choosing " + move.notation());
    }

    private void searchAiAvoidsIdleInvadingMajorMove() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e3"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("e7"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("c3"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("a9"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.ROOK));
        board.set(pos("i0"), hidden(PlayerColor.RED, PieceType.PAWN, PieceType.ROOK));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move when an invading rook has idle options");
        boolean idleInvadingRookMove = move.source().equals(pos("c3"))
                && board.get(move.destination()) == null
                && move.destination().y() <= 4;
        check(!idleInvadingRookMove,
                "AI should avoid idle invading rook moves when useful alternatives exist, chose " + move.notation());
    }

    private void searchAiClearsDangerousHomePawnWithMajor() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("f6"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("i9"), visible(PlayerColor.BLACK, PieceType.PAWN));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a defensive move against an advanced pawn");
        check(move.source().equals(pos("a6")) && move.destination().equals(pos("f6")),
                "AI should use a major piece to clear a safe advanced pawn near its king, chose " + move.notation());
    }

    private void searchAiClearsPawnNearPalaceBeforeMateNet() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("a7"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("d7"), visible(PlayerColor.RED, PieceType.PAWN));
        board.set(pos("i9"), visible(PlayerColor.BLACK, PieceType.PAWN));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move against a pawn near the palace");
        check(move.source().equals(pos("a7")) && move.destination().equals(pos("d7")),
                "AI should clear a pawn before it becomes a mate net, chose " + move.notation());
    }

    private void searchAiUsesPawnToSupportKingAttack() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("e7"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("d5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("h0"), visible(PlayerColor.RED, PieceType.PAWN));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move when a pawn can support the attack");
        boolean pawnSupport = (move.source().equals(pos("d5")) && move.destination().equals(pos("d4")))
                || (move.source().equals(pos("e5")) && move.destination().equals(pos("e4")));
        check(pawnSupport,
                "AI should use a pawn to support coordinated pressure on the king, chose " + move.notation());
    }

    private void searchAiAvoidsForcedRepeatedCheckLoss() {
        Board board = new Board();
        board.set(pos("e1"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a2"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("h6"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("i3"), visible(PlayerColor.RED, PieceType.PAWN));

        Move losingCheck = Move.move(pos("a2"), pos("a1"), 0);
        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200,
                candidate -> candidate.notation().equals(losingCheck.notation()) ? 900_000 : 0);
        check(move != null, "AI should find a non-losing move when repeated-check loss is avoidable");
        check(!move.notation().equals(losingCheck.notation()),
                "AI should not choose a repeated-check move that is already a forced rules loss");
    }

    private void gamePenalizesRepeatedCheckBeforeRuleLoss() {
        Game game = new Game(PlayerColor.BLACK);
        Board board = game.board();
        for (Position position : board.occupiedPositions()) {
            board.remove(position);
        }
        board.set(pos("f1"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("b0"), visible(PlayerColor.BLACK, PieceType.ROOK));

        Move firstCheck = Move.move(pos("b0"), pos("b1"), game.turnStartTime());
        check(game.repeatedCheckRisk(firstCheck, PlayerColor.BLACK) == 0,
                "first useful check should not be penalized");
        check(game.play(firstCheck).success(), "first check should be legal");
        check(game.play(Move.move(pos("f1"), pos("f0"), game.turnStartTime())).success(),
                "red should escape the first check");

        Move secondCheck = Move.move(pos("b1"), pos("b0"), game.turnStartTime());
        check(game.repeatedCheckRisk(secondCheck, PlayerColor.BLACK) == 0,
                "second check should not be treated as repeated yet");
        check(game.play(secondCheck).success(), "second check should be legal");
        check(game.play(Move.move(pos("f0"), pos("f1"), game.turnStartTime())).success(),
                "red should escape the second check");

        Move thirdCheck = Move.move(pos("b0"), pos("b1"), game.turnStartTime());
        check(game.repeatedCheckRisk(thirdCheck, PlayerColor.BLACK) >= 18_000,
                "third consecutive check should already carry a planning penalty");
        check(game.play(thirdCheck).success(), "third check should still be legal");
        check(game.play(Move.move(pos("f1"), pos("f0"), game.turnStartTime())).success(),
                "red should escape the third check");

        Move fourthCheck = Move.move(pos("b1"), pos("b0"), game.turnStartTime());
        check(game.repeatedCheckRisk(fourthCheck, PlayerColor.BLACK) >= 70_000,
                "fourth consecutive check should be strongly discouraged before rule loss");
    }

    private void searchAiAvoidsRookCheckThatKingCanEat() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("d3"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("h6"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("g4"), visible(PlayerColor.RED, PieceType.PAWN));

        Move badCheck = Move.move(pos("d3"), pos("d0"), 0);
        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move instead of a suicidal checking rook move");
        check(!move.notation().equals(badCheck.notation()),
                "AI should not check by moving a rook next to the king where it can be captured");
    }

    private void searchAiAvoidsCheckThatLosesRookAfterEscape() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("d8"), visible(PlayerColor.BLACK, PieceType.ROOK));
        board.set(pos("i0"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.ROOK));
        board.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.PAWN));

        Move badCheck = Move.move(pos("d8"), pos("d0"), 0);
        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move instead of a check that loses the rook after escape");
        check(!move.notation().equals(badCheck.notation()),
                "AI should not make a check that lets an opponent hidden rook recapture next, chose " + move.notation());
    }

    private void searchAiMovesCannonThreatenedByRookLine() {
        Board board = new Board();
        board.set(pos("e1"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("e5"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("d0"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("b0"), visible(PlayerColor.BLACK, PieceType.CANNON));
        board.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.PAWN));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move for a cannon attacked by a rook line");
        check(move.source().equals(pos("b0")),
                "AI should move or trade the cannon threatened by the rook line, chose " + move.notation());
    }

    private void searchAiAvoidsPassiveKingDrift() {
        Board board = new Board();
        board.set(pos("d0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("i3"), visible(PlayerColor.RED, PieceType.PAWN));

        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move without drifting with the king");
        check(!move.source().equals(pos("e9")),
                "AI should not move the king when it is not in check and useful alternatives exist");
    }

    private void searchAiAvoidsBlockingOwnKingEscape() {
        Board board = new Board();
        board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("f8"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("d9"), hidden(PlayerColor.BLACK, PieceType.PAWN, PieceType.GUARD));
        board.set(pos("a6"), visible(PlayerColor.BLACK, PieceType.PAWN));
        board.set(pos("c5"), visible(PlayerColor.RED, PieceType.KNIGHT));
        board.set(pos("a5"), visible(PlayerColor.RED, PieceType.ROOK));

        Move badBlock = Move.move(pos("d9"), pos("e8"), 0);
        Move move = new SearchAi().chooseMove(board, PlayerColor.BLACK, 0, 1200);
        check(move != null, "AI should find a move without blocking its king escape");
        check(!move.notation().equals(badBlock.notation()),
                "AI should not move a non-king piece into its palace when that blocks the king escape");
    }

    private void searchAiKnownKeyMasksHiddenIdentity() {
        Board first = new Board();
        first.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        first.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        first.set(pos("a0"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.ROOK));

        Board second = new Board();
        second.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        second.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        second.set(pos("a0"), hidden(PlayerColor.RED, PieceType.PAWN, PieceType.ROOK));

        check(!first.positionKey().equals(second.positionKey()), "raw key should include hidden identity");
        check(SearchAi.knownPositionKey(first).equals(SearchAi.knownPositionKey(second)),
                "AI known key should mask hidden identity");
    }

    private void gameChaseTargetsMaskHiddenIdentity() {
        Game first = new Game();
        Board firstBoard = first.board();
        for (Position position : firstBoard.occupiedPositions()) {
            firstBoard.remove(position);
        }
        firstBoard.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        firstBoard.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        firstBoard.set(pos("a9"), visible(PlayerColor.BLACK, PieceType.ROOK));
        firstBoard.set(pos("a0"), hidden(PlayerColor.RED, PieceType.ROOK, PieceType.PAWN));

        Game second = new Game();
        Board secondBoard = second.board();
        for (Position position : secondBoard.occupiedPositions()) {
            secondBoard.remove(position);
        }
        secondBoard.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
        secondBoard.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        secondBoard.set(pos("a9"), visible(PlayerColor.BLACK, PieceType.ROOK));
        secondBoard.set(pos("a0"), hidden(PlayerColor.RED, PieceType.CANNON, PieceType.PAWN));

        try {
            Method chaseTargets = Game.class.getDeclaredMethod("chaseTargets", Position.class, PlayerColor.class);
            chaseTargets.setAccessible(true);
            String firstTargets = (String) chaseTargets.invoke(first, pos("a9"), PlayerColor.BLACK);
            String secondTargets = (String) chaseTargets.invoke(second, pos("a9"), PlayerColor.BLACK);
            check(firstTargets.equals(secondTargets), "chase targets should mask hidden identity");
            check(firstTargets.contains("PAWN") && !firstTargets.contains("ROOK") && !firstTargets.contains("CANNON"),
                    "chase targets should use hidden move type for unrevealed pieces");
        } catch (Exception e) {
            throw new AssertionError("chase target hidden identity test failed: " + e.getMessage(), e);
        }
    }

    private void gameReportsRepeatedChaseRisk() {
        Game game = new Game();
        Board board = game.board();
        for (Position position : board.occupiedPositions()) {
            board.remove(position);
        }
        board.set(pos("d0"), visible(PlayerColor.RED, PieceType.KING));
        board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
        board.set(pos("a0"), visible(PlayerColor.RED, PieceType.ROOK));
        board.set(pos("a4"), visible(PlayerColor.BLACK, PieceType.KNIGHT));
        board.set(pos("i9"), visible(PlayerColor.BLACK, PieceType.PAWN));

        Move chase = Move.move(pos("a0"), pos("a1"), game.turnStartTime());
        check(game.repeatedTacticalRisk(chase, PlayerColor.RED) == 0,
                "first chase should not be treated as repeated");

        for (int i = 0; i < 4; i++) {
            Move redMove = Move.move(pos("a0"), pos("a1"), game.turnStartTime());
            MoveResult red = game.play(redMove);
            check(red.success(), "red chase setup move should succeed");
            Move blackMove = Move.move(pos("i9"), pos("i8"), game.turnStartTime());
            MoveResult black = game.play(blackMove);
            check(black.success(), "black waiting move should succeed");
            board.remove(pos("a1"));
            board.set(pos("a0"), visible(PlayerColor.RED, PieceType.ROOK));
            board.remove(pos("i8"));
            board.set(pos("i9"), visible(PlayerColor.BLACK, PieceType.PAWN));
        }

        check(game.repeatedTacticalRisk(chase, PlayerColor.RED) > 0,
                "repeated chase should receive a tactical risk penalty");
    }

    private void experienceMemoryPersistsLearning() {
        try {
            Path file = Files.createTempFile("jieqi-ai-learning", ".tsv");
            Board board = new Board();
            board.set(pos("e0"), visible(PlayerColor.RED, PieceType.KING));
            board.set(pos("e9"), visible(PlayerColor.BLACK, PieceType.KING));
            board.set(pos("a9"), visible(PlayerColor.BLACK, PieceType.ROOK));
            Move move = Move.move(pos("a9"), pos("a8"), 0);

            ExperienceMemory memory = new ExperienceMemory(file);
            memory.learn(List.of(new ExperienceMemory.Decision(board.positionKey(), move.notation())), 100);
            ExperienceMemory reloaded = new ExperienceMemory(file);
            check(reloaded.bonus(board.positionKey(), move) > 0, "AI学习文件重载后应保留正向经验");
            Files.deleteIfExists(file);
        } catch (Exception e) {
            throw new AssertionError("AI学习文件测试失败: " + e.getMessage(), e);
        }
    }

    private void gameRejectsFlipAndReturnsFlipResultForHiddenMove() {
        Game flipGame = new Game();
        MoveResult flipResult = flipGame.play(Move.flip(Position.parse("a0"), flipGame.turnStartTime()));
        check(!flipResult.success(), "新规则应拒绝原地翻子");

        Game moveGame = new Game();
        MoveResult moveResult = moveGame.play(Move.move(Position.parse("a0"), Position.parse("a1"), moveGame.turnStartTime()));
        check(moveResult.flipResult() != null, "暗子移动后应返回 flipResult");
    }

    private Piece visible(PlayerColor color, PieceType type) {
        return new Piece(color, type, type, true);
    }

    private Piece hidden(PlayerColor color, PieceType realType, PieceType hiddenMoveType) {
        return new Piece(color, realType, hiddenMoveType, false);
    }

    private Position pos(String notation) {
        return Position.parse(notation);
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
