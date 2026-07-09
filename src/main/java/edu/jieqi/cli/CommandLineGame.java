package edu.jieqi.cli;

import edu.jieqi.engine.Game;
import edu.jieqi.engine.GameStatus;
import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.Position;

import java.util.List;
import java.util.Scanner;

public class CommandLineGame {
    private final Game game = new Game();

    public void run() {
        Scanner scanner = new Scanner(System.in);
        printWelcome();
        System.out.println(game.board().render());

        while (game.status() == GameStatus.PLAYING) {
            System.out.print(System.lineSeparator() + game.currentPlayer().displayName() + "方> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if ("quit".equalsIgnoreCase(line)) {
                System.out.println("已退出。");
                return;
            }
            handle(line);
        }

        System.out.println("对局结束：" + game.status());
    }

    private void handle(String line) {
        try {
            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();
            switch (command) {
                case "board" -> System.out.println(game.board().render());
                case "moves" -> printMoves();
                case "help" -> printHelp();
                case "resign" -> printResult(game.resign());
                case "flip" -> System.out.println("新规则不允许原地翻子，暗子只能在移动或吃子后翻开。");
                case "move" -> {
                    Move move = parseMoveArgument(parts);
                    printResult(game.play(Move.move(
                            move.source(),
                            move.destination(),
                            game.turnStartTime())));
                }
                default -> System.out.println("未知命令。输入 help 查看帮助。");
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private void printResult(MoveResult result) {
        System.out.println(result.message());
        if (result.success()) {
            System.out.println(game.board().render());
        }
    }

    private void printMoves() {
        List<Move> actions = game.currentActions();
        System.out.println(game.currentPlayer().displayName() + "方共有 " + actions.size() + " 个可行动作。");
        int limit = Math.min(actions.size(), 40);
        for (int i = 0; i < limit; i++) {
            System.out.println("  " + actions.get(i).notation());
        }
        if (actions.size() > limit) {
            System.out.println("  ...");
        }
    }

    private void requireLength(String[] parts, int expected, String message) {
        if (parts.length != expected) {
            throw new IllegalArgumentException(message);
        }
    }

    private Move parseMoveArgument(String[] parts) {
        if (parts.length == 3) {
            return Move.move(Position.parse(parts[1]), Position.parse(parts[2]), game.turnStartTime());
        }
        throw new IllegalArgumentException("用法: move b2 b9");
    }

    private void printWelcome() {
        System.out.println("揭棋命令行双人对战");
        printHelp();
    }

    private void printHelp() {
        System.out.println("""
                命令:
                  board        显示棋盘
                  moves        显示当前方可行动作
                  move b2 b9   移动或吃子
                  resign       认输
                  help         显示帮助
                  quit         退出
                """);
    }
}
