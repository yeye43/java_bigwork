package edu.jieqi.ai;

import edu.jieqi.engine.Game;
import edu.jieqi.engine.GameStatus;
import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.PlayerColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class SelfPlayTrainer {
    private static final int DEFAULT_GAMES = 50;
    private static final int DEFAULT_MAX_PLIES = 220;
    private static final long DEFAULT_THINK_MILLIS = 180;
    private static final Path DEFAULT_TRAINING_FILE = Path.of("records", "ai-training.tsv");
    private static final Path DEFAULT_LIVE_FILE = Path.of("records", "ai-learning.tsv");

    public static void main(String[] args) {
        int games = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_GAMES;
        int maxPlies = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_MAX_PLIES;
        long thinkMillis = args.length > 2 ? Long.parseLong(args[2]) : DEFAULT_THINK_MILLIS;
        Path trainingFile = args.length > 3 ? Path.of(args[3]) : DEFAULT_TRAINING_FILE;
        Path liveFile = args.length > 4 ? Path.of(args[4]) : DEFAULT_LIVE_FILE;

        ExperienceMemory memory = new ExperienceMemory(trainingFile);
        ExperienceMemory liveMemory = new ExperienceMemory(liveFile);
        SearchAi ai = new SearchAi(memory);
        int redWins = 0;
        int blackWins = 0;
        int draws = 0;

        for (int gameIndex = 1; gameIndex <= games; gameIndex++) {
            Game game = new Game();
            Map<PlayerColor, List<ExperienceMemory.Decision>> decisions = new EnumMap<>(PlayerColor.class);
            decisions.put(PlayerColor.RED, new ArrayList<>());
            decisions.put(PlayerColor.BLACK, new ArrayList<>());

            int plies = 0;
            while (game.status() == GameStatus.PLAYING && plies < maxPlies) {
                PlayerColor color = game.currentPlayer();
                String positionKey = SearchAi.knownPositionKey(game.board());
                Move move = ai.chooseMove(game.board(), color, game.turnStartTime(), thinkMillis,
                        candidate -> game.repeatedTacticalRisk(candidate, color));
                if (move == null) {
                    break;
                }
                MoveResult result = game.play(move);
                if (!result.success()) {
                    break;
                }
                decisions.get(color).add(new ExperienceMemory.Decision(positionKey, move.notation()));
                plies++;
            }

            GameStatus status = game.status();
            if (status == GameStatus.PLAYING) {
                status = GameStatus.DRAW;
            }
            Path gameMemoryFile = Path.of("records", "ai-last-game-" + gameIndex + ".tmp.tsv");
            try {
                Files.deleteIfExists(gameMemoryFile);
            } catch (Exception ignored) {
            }
            ExperienceMemory gameMemory = new ExperienceMemory(gameMemoryFile);
            gameMemory.learn(decisions.get(PlayerColor.RED), ExperienceMemory.rewardFor(PlayerColor.RED, status));
            gameMemory.learn(decisions.get(PlayerColor.BLACK), ExperienceMemory.rewardFor(PlayerColor.BLACK, status));
            memory.mergeFrom(gameMemory);
            liveMemory.mergeFrom(gameMemory);
            try {
                Files.deleteIfExists(gameMemoryFile);
            } catch (Exception ignored) {
            }

            switch (status) {
                case RED_WIN -> redWins++;
                case BLACK_WIN -> blackWins++;
                case DRAW, PLAYING -> draws++;
            }

            System.out.println("self-play " + gameIndex + "/" + games
                    + " result=" + status
                    + " plies=" + plies
                    + " total(red=" + redWins + ", black=" + blackWins + ", draw=" + draws + ")"
                    + " trainingFile=" + trainingFile
                    + " liveFile=" + liveFile);
        }
    }
}
