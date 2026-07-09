package edu.jieqi.ai;

import edu.jieqi.engine.Board;
import edu.jieqi.engine.RuleEngine;
import edu.jieqi.model.Move;
import edu.jieqi.model.Piece;
import edu.jieqi.model.Position;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class XiangqiRecordTrainer {
    private static final Pattern COORDINATE_MOVE =
            Pattern.compile("\\b([a-i][0-9])\\s*(?:-|:|,|\\s)?\\s*([a-i][0-9])\\b", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("用法: XiangqiRecordTrainer 棋谱文件或目录 [学习文件] [每步奖励]");
            System.out.println("支持坐标格式: h2e2、h2-e2、move h2 e2。中文记谱需先转换成坐标。");
            return;
        }
        Path source = Path.of(args[0]);
        Path target = args.length > 1 ? Path.of(args[1]) : Path.of("records", "ai-learning.tsv");
        int reward = args.length > 2 ? Integer.parseInt(args[2]) : 30;
        ExperienceMemory memory = new ExperienceMemory(target);
        int games = 0;
        int moves = 0;
        for (Path file : inputFiles(source)) {
            int imported = importFile(file, memory, reward);
            if (imported > 0) {
                games++;
                moves += imported;
                System.out.println("imported " + imported + " moves from " + file);
            }
        }
        System.out.println("done. games=" + games + " moves=" + moves + " target=" + target);
    }

    private static int importFile(Path file, ExperienceMemory memory, int reward) {
        try {
            Board board = Board.standardVisibleBoard();
            RuleEngine rules = new RuleEngine();
            List<ExperienceMemory.Decision> decisions = new ArrayList<>();
            String text = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = COORDINATE_MOVE.matcher(text);
            while (matcher.find()) {
                Position source = Position.parse(matcher.group(1));
                Position destination = Position.parse(matcher.group(2));
                Piece mover = board.get(source);
                if (mover == null || !rules.canMoveAndKeepKingSafe(board, source, destination, mover.color())) {
                    continue;
                }
                Move move = Move.move(source, destination, 0);
                decisions.add(new ExperienceMemory.Decision(board.positionKey(), move.notation()));
                board.remove(source);
                board.set(destination, mover);
            }
            if (!decisions.isEmpty()) {
                memory.learn(decisions, reward);
            }
            return decisions.size();
        } catch (Exception e) {
            System.out.println("导入棋谱失败 " + file + ": " + e.getMessage());
            return 0;
        }
    }

    private static List<Path> inputFiles(Path source) throws IOException {
        if (Files.isRegularFile(source)) {
            return List.of(source);
        }
        try (Stream<Path> stream = Files.walk(source)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".txt") || name.endsWith(".iccs")
                                || name.endsWith(".pgn") || name.endsWith(".xq");
                    })
                    .toList();
        }
    }
}
