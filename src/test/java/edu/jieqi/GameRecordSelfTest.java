package edu.jieqi;

import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.engine.Board;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;
import edu.jieqi.record.GameRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GameRecordSelfTest {
    public static void main(String[] args) throws Exception {
        GameRecordSelfTest test = new GameRecordSelfTest();
        test.recordInitialBoardSnapshot();
        test.recordMoveWithFlipType();
        test.recordMoveWithoutFlipType();
        System.out.println("棋谱自测试通过。");
    }

    private void recordInitialBoardSnapshot() throws Exception {
        Path directory = Path.of("target", "test-records");
        GameRecord record = new GameRecord(directory, Board.initialBoard(), PlayerColor.RED);
        record.close();

        String line = Files.readAllLines(record.file()).get(0);
        check(line.contains("\"kind\":\"initial\""), "initial record should be marked");
        check(line.contains("\"firstPlayer\":\"red\""), "initial record should include first player");
        check(line.contains("\"board\""), "initial record should include board");
        check(line.contains("\"type\""), "initial record should include real piece type");
        check(line.contains("\"hiddenMoveType\""), "initial record should include hidden move type");
    }

    private void recordMoveWithFlipType() throws Exception {
        Path directory = Path.of("target", "test-records");
        GameRecord record = new GameRecord(directory);
        record.recordMove(
                PlayerColor.RED,
                Move.move(Position.parse("b2"), Position.parse("b9"), 0),
                MoveResult.success("OK", true, false, PieceType.ROOK));
        record.close();

        List<String> lines = Files.readAllLines(record.file());
        check(lines.size() == 1, "棋谱应写入一行");
        String line = lines.get(0);
        check(line.contains("\"turn\":1"), "棋谱应记录 turn");
        check(line.contains("\"color\":\"red\""), "棋谱应记录 color");
        check(line.contains("\"source\":\"b2\""), "棋谱应记录 source");
        check(line.contains("\"destination\":\"b9\""), "棋谱应记录 destination");
        check(line.contains("\"type\":\"rook\""), "暗子第一次移动后应记录 type");
        check(line.contains("\"timestamp\":"), "棋谱应记录 timestamp");
    }

    private void recordMoveWithoutFlipType() throws Exception {
        Path directory = Path.of("target", "test-records");
        GameRecord record = new GameRecord(directory);
        record.recordMove(
                PlayerColor.BLACK,
                Move.move(Position.parse("e9"), Position.parse("e8"), 0),
                MoveResult.success("OK", false, false));
        record.close();

        String line = Files.readAllLines(record.file()).get(0);
        check(line.contains("\"color\":\"black\""), "棋谱应记录黑方");
        check(!line.contains("\"type\""), "明子普通移动不应记录 type");
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
