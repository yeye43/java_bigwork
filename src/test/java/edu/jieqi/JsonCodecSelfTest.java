package edu.jieqi;

import edu.jieqi.message.ErrorMessage;
import edu.jieqi.message.GameStartMessage;
import edu.jieqi.message.JsonCodec;
import edu.jieqi.message.BoardMessage;
import edu.jieqi.message.MoveMessage;
import edu.jieqi.message.MoveResultMessage;
import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.Position;

public class JsonCodecSelfTest {
    private final JsonCodec codec = new JsonCodec();

    public static void main(String[] args) {
        JsonCodecSelfTest test = new JsonCodecSelfTest();
        test.runAll();
        System.out.println("JSON 自测试通过。");
    }

    private void runAll() {
        moveToJson();
        jsonToMove();
        moveResultToJson();
        moveResultWithFlipResultToJson();
        gameStartToJson();
        errorToJson();
        boardJsonDecodesNewlines();
    }

    private void moveToJson() {
        Move move = Move.move(Position.parse("b2"), Position.parse("b9"), 0);
        String json = codec.toJson(MoveMessage.fromMove(move));
        check(json.contains("\"messageType\":\"move\""), "走子 JSON 应包含 messageType");
        check(json.contains("\"fromX\":\"b\""), "走子 JSON 应包含 fromX");
        check(json.contains("\"fromY\":2"), "走子 JSON 应包含 fromY");
        check(json.contains("\"toX\":\"b\""), "走子 JSON 应包含 toX");
        check(json.contains("\"toY\":9"), "走子 JSON 应包含 toY");
        check(json.contains("\"isFlip\":false"), "走子 JSON 应包含 isFlip=false");
    }

    private void jsonToMove() {
        MoveMessage message = codec.parseMoveMessage("""
                {"messageType":"move","fromX":"b","fromY":2,"toX":"b","toY":9,"isFlip":false}
                """);
        Move move = message.toMove(123);
        check(move.source().equals(Position.parse("b2")), "JSON 应解析出 source=b2");
        check(move.destination().equals(Position.parse("b9")), "JSON 应解析出 destination=b9");
        check(!move.flipOnly(), "JSON 应解析出非翻子走法");
    }

    private void moveResultToJson() {
        MoveMessage move = new MoveMessage("b", 2, "b", 9, false);
        MoveResult result = MoveResult.success("OK", true, false);
        String json = codec.toJson(MoveResultMessage.fromResult(result, move));
        check(json.contains("\"messageType\":\"moveResult\""), "结果 JSON 应包含 moveResult");
        check(json.contains("\"success\":true"), "结果 JSON 应包含 success");
        check(json.contains("\"valid\":true"), "结果 JSON 应包含 valid");
        check(json.contains("\"move\""), "结果 JSON 应包含 move 对象");
        check(!json.contains("\"flipResult\""), "未翻子时结果 JSON 不应包含 flipResult");
    }

    private void moveResultWithFlipResultToJson() {
        MoveMessage move = new MoveMessage("a", 0, "a", 0, true);
        MoveResult result = MoveResult.success("翻开", false, false, PieceType.ROOK);
        String json = codec.toJson(MoveResultMessage.fromResult(result, move));
        check(json.contains("\"flipResult\":\"rook\""), "翻子结果 JSON 应包含 flipResult");
    }

    private void gameStartToJson() {
        String json = codec.toJson(new GameStartMessage("u1", "u2", "red", true));
        check(json.contains("\"messageType\":\"gameStart\""), "开始 JSON 应包含 gameStart");
        check(json.contains("\"yourColor\":\"red\""), "开始 JSON 应包含颜色");
    }

    private void errorToJson() {
        String json = codec.toJson(new ErrorMessage(2001, "非法走子"));
        check(json.contains("\"messageType\":\"error\""), "错误 JSON 应包含 error");
        check(json.contains("\"code\":2001"), "错误 JSON 应包含 code");
        check(json.contains("非法走子"), "错误 JSON 应包含 message");
    }

    private void boardJsonDecodesNewlines() {
        String board = "a b c" + System.lineSeparator() + "1 2 3";
        String json = codec.toJson(new BoardMessage(board));
        String decoded = codec.readStringField(json, "board");
        check(decoded.equals(board), "棋盘 JSON 应能把换行反转义回来");
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
