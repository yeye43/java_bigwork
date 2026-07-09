package edu.jieqi.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.jieqi.ai.ExperienceMemory;
import edu.jieqi.ai.SearchAi;
import edu.jieqi.engine.Board;
import edu.jieqi.engine.Game;
import edu.jieqi.engine.GameStatus;
import edu.jieqi.engine.MoveGenerator;
import edu.jieqi.engine.RuleEngine;
import edu.jieqi.message.JsonCodec;
import edu.jieqi.message.MoveMessage;
import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PieceType;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.model.Position;
import edu.jieqi.record.GameRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class JieqiWebServer {
    private static final int DEFAULT_PORT = 8080;
    private static final String MODE_ONLINE = "online";
    private static final String MODE_AI = "ai";
    private static final long AI_MOVE_DELAY_MILLIS = 0;
    private static final long AI_THINK_MILLIS = 12_000;
    private static final Path RECORDS_DIR = Path.of("records");
    private static final Path HIDDEN_RECORDS_FILE = RECORDS_DIR.resolve("hidden-records.txt");
    private static final String RECORD_DELETE_PASSWORD = "131415";
    private static final DateTimeFormatter RECORD_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final JsonCodec codec = new JsonCodec();
    private final ExperienceMemory experienceMemory = new ExperienceMemory(Path.of("records", "ai-learning.tsv"));
    private final RuleEngine ruleEngine = new RuleEngine();
    private final MoveGenerator moveGenerator = new MoveGenerator(ruleEngine);
    private final ExecutorService aiExecutor = Executors.newFixedThreadPool(3);
    private final Map<PlayMode, List<Room>> rooms = new EnumMap<>(PlayMode.class);
    private final Random random = new Random();
    private PlayerColor nextAiFirstPlayer;

    public JieqiWebServer() throws IOException {
        rooms.put(PlayMode.ONLINE, new ArrayList<>());
        rooms.put(PlayMode.AI, new ArrayList<>());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (List<Room> roomList : rooms.values()) {
                for (Room room : roomList) {
                    try {
                        if (room.record != null) {
                            room.record.close();
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
            aiExecutor.shutdownNow();
        }));
    }

    public static void main(String[] args) throws IOException {
        int port = resolvePort(args);
        new JieqiWebServer().start(port);
    }

    private static int resolvePort(String[] args) {
        if (args.length > 0 && !args[0].isBlank()) {
            return Integer.parseInt(args[0]);
        }
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isBlank()) {
            return Integer.parseInt(envPort);
        }
        return DEFAULT_PORT;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/api/rooms", this::handleRooms);
        server.createContext("/api/join", this::handleJoin);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/move", this::handleMove);
        server.createContext("/api/resign", this::handleResign);
        server.createContext("/api/restart", this::handleRestart);
        server.createContext("/api/replay", this::handleReplay);
        server.createContext("/api/records", this::handleRecords);
        server.createContext("/api/record-replay", this::handleRecordReplay);
        server.createContext("/api/delete-record", this::handleDeleteRecord);
        server.createContext("/api/leave", this::handleLeave);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("揭棋网页服务器启动，端口 " + port);
        printUrls(port);
    }

    private synchronized void handleRooms(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 GET"));
            return;
        }
        PlayMode mode = parseMode(queryParams(exchange).get("mode"));
        syncRooms(mode);
        send(exchange, 200, roomsJson(mode));
    }

    private synchronized void handleJoin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 POST"));
            return;
        }
        String body = readBody(exchange);
        PlayMode requestedMode = parseMode(readOptional(body, "mode"));
        String action = readOptional(body, "action");
        String roomId = readOptional(body, "roomId");
        String existingId = readOptional(body, "playerId");

        Room room = selectJoinRoom(requestedMode, roomId, action, existingId);
        if (room == null) {
            send(exchange, 404, jsonError("房间不存在。"));
            return;
        }

        if (existingId != null && room.players.containsKey(existingId)) {
            send(exchange, 200, joinJson(existingId, room.players.get(existingId), requestedMode, room, "欢迎回来。"));
            return;
        }

        PlayerColor color = null;
        boolean spectate = "spectate".equalsIgnoreCase(action);
        if (!spectate) {
            color = seatColorForJoin(room, requestedMode);
        }

        String playerId = UUID.randomUUID().toString();
        if (color != null) {
            room.players.put(playerId, color);
            room.playerIds.put(color, playerId);
            room.inbox.put(playerId, new ArrayList<>());
            addSeatInfoMessage(room, playerId, color);
            send(exchange, 200, joinJson(playerId, color, requestedMode, room, "加入成功。"));
        } else {
            room.players.put(playerId, null);
            room.inbox.put(playerId, new ArrayList<>());
            addMessage(room, playerId, requestedMode == PlayMode.AI ? "人机对战已有玩家，你正在观战。" : "你正在观战。");
            send(exchange, 200, joinJson(playerId, null, requestedMode, room, "以观战身份加入。"));
        }
    }

    private synchronized void handleState(HttpExchange exchange) throws IOException {
        Map<String, String> params = queryParams(exchange);
        String playerId = params.get("playerId");
        PlayMode mode = parseMode(params.get("mode"));
        Room room = roomForRequest(mode, params.get("roomId"), playerId);
        if (room == null) {
            send(exchange, 404, jsonError("房间不存在。"));
            return;
        }
        syncGameState(room, mode);
        send(exchange, 200, stateJson(room, mode, playerId));
    }

    private synchronized void handleMove(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 POST"));
            return;
        }
        String body = readBody(exchange);
        PlayMode mode = parseMode(readOptional(body, "mode"));
        String playerId = readRequired(body, "playerId");
        Room room = roomForRequest(mode, readOptional(body, "roomId"), playerId);
        if (room == null) {
            send(exchange, 404, jsonError("房间不存在。"));
            return;
        }
        PlayerColor color = room.players.get(playerId);
        syncGameState(room, mode);
        if (color == null) {
            send(exchange, 403, jsonError("观战者不能走子。"));
            return;
        }
        if (color != room.game.currentPlayer()) {
            send(exchange, 400, jsonError("未轮到本方走子。"));
            return;
        }

        MoveMessage moveMessage = codec.parseMoveMessage(body);
        Move move = moveMessage.toMove(room.game.turnStartTime());
        Piece capturedBeforeMove = room.game.board().get(move.destination());
        boolean capturedHiddenOpponentPiece = capturedBeforeMove != null
                && capturedBeforeMove.color() != color
                && !capturedBeforeMove.visible();
        String learningPosition = SearchAi.knownPositionKey(room.game.board());

        MoveResult result = room.game.play(move);
        if (result.success()) {
            room.record.recordMove(color, move, result);
            appendReplayStep(room, color, move, result, capturedBeforeMove);
            recordDecision(room, color, learningPosition, move);
            sendVisibleMoveMessages(room, playerId, color, moveMessage, result, capturedHiddenOpponentPiece);
            scheduleAiTurnIfNeeded(room, mode);
            updateScoreIfEnded(room);
            send(exchange, 200, stateJson(room, mode, playerId));
        } else {
            addMessage(room, playerId, result.message());
            send(exchange, 400, stateJson(room, mode, playerId));
        }
    }

    private synchronized void handleResign(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 POST"));
            return;
        }
        String body = readBody(exchange);
        PlayMode mode = parseMode(readOptional(body, "mode"));
        String playerId = readRequired(body, "playerId");
        Room room = roomForRequest(mode, readOptional(body, "roomId"), playerId);
        if (room == null) {
            send(exchange, 404, jsonError("房间不存在。"));
            return;
        }
        PlayerColor color = room.players.get(playerId);
        syncGameState(room, mode);
        if (color == null) {
            send(exchange, 403, jsonError("观战者不能认输。"));
            return;
        }
        MoveResult result = room.game.resign(color);
        cancelAiTurn(room);
        broadcast(room, result.message());
        finishReplayIfNeeded(room, result.message());
        updateScoreIfEnded(room);
        send(exchange, 200, stateJson(room, mode, playerId));
    }

    private synchronized void handleRestart(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 POST"));
            return;
        }
        String body = readBody(exchange);
        PlayMode mode = parseMode(readOptional(body, "mode"));
        String playerId = readRequired(body, "playerId");
        Room room = roomForRequest(mode, readOptional(body, "roomId"), playerId);
        if (room == null) {
            send(exchange, 404, jsonError("房间不存在。"));
            return;
        }
        if (room.players.get(playerId) == null) {
            send(exchange, 403, jsonError("观战者不能重新对弈。"));
            return;
        }
        if (room.record != null) {
            room.record.close();
        }
        finishReplayIfNeeded(room, "本局结束，开始新一局。");
        configureNextRoundSeats(room);
        room.game = new Game(room.firstPlayer);
        room.record = new GameRecord(RECORDS_DIR, room.game.board(), room.firstPlayer, modeName(room.mode));
        room.currentReplay = new ReplaySnapshot(room.game.board().copy(), room.firstPlayer, room.mode);
        room.scoreRecordedForCurrentGame = false;
        cancelAiTurn(room);
        room.learningRecorded = false;
        room.learningDecisions.get(PlayerColor.RED).clear();
        room.learningDecisions.get(PlayerColor.BLACK).clear();
        for (List<String> messages : room.inbox.values()) {
            messages.clear();
        }
        announceRoundStart(room);
        scheduleAiTurnIfNeeded(room, mode);
        send(exchange, 200, stateJson(room, mode, playerId));
    }

    private synchronized void handleLeave(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 POST"));
            return;
        }
        String body = readBody(exchange);
        PlayMode mode = parseMode(readOptional(body, "mode"));
        String playerId = readOptional(body, "playerId");
        Room room = roomForRequest(mode, readOptional(body, "roomId"), playerId);
        if (room != null && playerId != null && !playerId.isBlank()) {
            leaveRoom(mode, room, playerId);
        }
        send(exchange, 200, "{\"success\":true}");
    }

    private synchronized void handleReplay(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 GET"));
            return;
        }
        Map<String, String> params = queryParams(exchange);
        PlayMode mode = parseMode(params.get("mode"));
        String playerId = params.get("playerId");
        Room room = roomForRequest(mode, params.get("roomId"), playerId);
        if (room == null || playerId == null || !room.players.containsKey(playerId)) {
            send(exchange, 404, jsonError("房间不存在。"));
            return;
        }
        send(exchange, 200, replayJson(room));
    }

    private synchronized void handleRecords(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 GET"));
            return;
        }
        send(exchange, 200, recordsJson());
    }

    private synchronized void handleRecordReplay(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 GET"));
            return;
        }
        String fileName = queryParams(exchange).get("file");
        Path file = safeRecordPath(fileName);
        if (file == null || !Files.isRegularFile(file)) {
            send(exchange, 404, jsonError("棋谱不存在。"));
            return;
        }
        ReplaySnapshot replay = replayFromRecord(file);
        if (replay == null || replay.steps.isEmpty()) {
            send(exchange, 404, jsonError("棋谱没有可回放的走子。"));
            return;
        }
        send(exchange, 200, replayJson(replay));
    }

    private synchronized void handleDeleteRecord(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, jsonError("只支持 POST"));
            return;
        }
        String body = readBody(exchange);
        String password = stringField(body, "password");
        if (!RECORD_DELETE_PASSWORD.equals(password)) {
            send(exchange, 403, jsonError("密码错误。"));
            return;
        }
        List<String> fileNames = readStringArrayField(body, "files");
        String singleFile = stringField(body, "file");
        if (fileNames.isEmpty() && singleFile != null && !singleFile.isBlank()) {
            fileNames.add(singleFile);
        }
        if (fileNames.isEmpty()) {
            send(exchange, 400, jsonError("没有选择棋谱。"));
            return;
        }
        List<Path> filesToDelete = new ArrayList<>();
        for (String fileName : fileNames) {
            Path file = safeRecordPath(fileName);
            if (file == null || !Files.isRegularFile(file)) {
                send(exchange, 404, jsonError("棋谱不存在：" + fileName));
                return;
            }
            filesToDelete.add(file);
        }
        List<String> hiddenRecords = hiddenRecordNames();
        int deleted = 0;
        for (Path file : filesToDelete) {
            String fileName = file.getFileName().toString();
            if (!hiddenRecords.contains(fileName)) {
                hiddenRecords.add(fileName);
                deleted++;
            }
        }
        saveHiddenRecordNames(hiddenRecords);
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        pair(sb, "success", true).append(",");
        pair(sb, "deleted", deleted);
        sb.append("}");
        send(exchange, 200, sb.toString());
    }

    private void handleIndex(HttpExchange exchange) throws IOException {
        if (!"/".equals(exchange.getRequestURI().getPath())) {
            send(exchange, 404, "Not found", "text/plain; charset=utf-8");
            return;
        }
        send(exchange, 200, html(), "text/html; charset=utf-8");
    }

    private void sendVisibleMoveMessages(
            Room room,
            String actorId,
            PlayerColor actorColor,
            MoveMessage moveMessage,
            MoveResult result,
            boolean capturedHiddenOpponentPiece) {
        String opponentId = room.playerIds.get(actorColor.opponent());
        if (actorId != null) {
            addMessage(room, actorId, result.message());
        }
        if (opponentId != null) {
            if (capturedHiddenOpponentPiece) {
                addMessage(room, opponentId, actorColor.displayName() + "从 "
                        + moveMessage.fromX() + moveMessage.fromY()
                        + " 走到 " + moveMessage.toX() + moveMessage.toY()
                        + "，吃掉一个暗子。");
            } else {
                addMessage(room, opponentId, result.message());
            }
        }
        for (Map.Entry<String, PlayerColor> entry : room.players.entrySet()) {
            if (entry.getValue() == null) {
                addMessage(room, entry.getKey(), result.message());
            }
        }
    }

    private String stateJson(Room room, PlayMode mode, String playerId) {
        syncGameState(room, mode);
        long now = System.currentTimeMillis();
        PlayerColor yourColor = room.players.get(playerId);
        List<String> messages = room.inbox.computeIfAbsent(playerId == null ? "" : playerId, ignored -> new ArrayList<>());
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        pair(sb, "playerId", playerId == null ? "" : playerId).append(",");
        pair(sb, "roomId", room.id).append(",");
        pair(sb, "roomName", room.name).append(",");
        pair(sb, "yourColor", colorName(yourColor)).append(",");
        pair(sb, "aiColor", mode == PlayMode.AI ? colorName(room.aiColor) : "").append(",");
        pair(sb, "firstPlayer", colorName(room.firstPlayer)).append(",");
        pair(sb, "mode", modeName(mode)).append(",");
        pair(sb, "canRestart", yourColor != null).append(",");
        pair(sb, "currentPlayer", colorName(room.game.currentPlayer())).append(",");
        pair(sb, "currentInCheck", room.game.status() == GameStatus.PLAYING
                && ruleEngine.isInCheck(room.game.board(), room.game.currentPlayer())).append(",");
        pair(sb, "status", room.game.status().name()).append(",");
        pair(sb, "hasReplay", room.lastReplay != null).append(",");
        pair(sb, "redScore", room.redScore).append(",");
        pair(sb, "blackScore", room.blackScore).append(",");
        pair(sb, "drawScore", room.drawScore).append(",");
        pair(sb, "serverTime", now).append(",");
        pair(sb, "turnStartTime", room.game.turnStartTime()).append(",");
        pair(sb, "turnLimitMillis", Game.TURN_LIMIT_MILLIS).append(",");
        pair(sb, "gameStartTime", room.game.gameStartTime()).append(",");
        pair(sb, "gameElapsedMillis", room.game.gameElapsedMillis(now)).append(",");
        pair(sb, "recordFile", room.record.file().toString()).append(",");
        sb.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            quote(sb, messages.get(i));
        }
        sb.append("],");
        sb.append("\"board\":[");
        boolean first = true;
        for (int y = Position.HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < Position.WIDTH; x++) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                appendCell(sb, room, new Position(x, y));
            }
        }
        sb.append("],");
        appendLegalMoves(sb, room, yourColor);
        sb.append("}");
        return sb.toString();
    }

    private void syncGameState(Room room, PlayMode mode) {
        MoveResult timeout = room.game.checkTimeout();
        if (timeout != null) {
            broadcast(room, timeout.message());
            finishReplayIfNeeded(room, timeout.message());
        }
        MoveResult noLegalResponse = room.game.adjudicateNoLegalResponseForCurrentPlayer();
        if (noLegalResponse != null) {
            broadcast(room, noLegalResponse.message());
            finishReplayIfNeeded(room, noLegalResponse.message());
        }
        updateScoreIfEnded(room);
        startAiTurnIfReady(room, mode);
        updateScoreIfEnded(room);
    }

    private void scheduleAiTurnIfNeeded(Room room, PlayMode mode) {
        if (mode != PlayMode.AI || room.game.status() != GameStatus.PLAYING
                || room.game.currentPlayer() != room.aiColor) {
            if (!room.aiThinking) {
                room.aiMoveReadyAt = 0;
            }
            return;
        }
        if (room.aiMoveReadyAt == 0) {
            room.aiMoveReadyAt = System.currentTimeMillis() + AI_MOVE_DELAY_MILLIS;
        }
    }

    private void startAiTurnIfReady(Room room, PlayMode mode) {
        scheduleAiTurnIfNeeded(room, mode);
        if (room.aiMoveReadyAt == 0 || room.aiThinking || System.currentTimeMillis() < room.aiMoveReadyAt) {
            return;
        }
        PlayerColor aiColor = room.aiColor;
        long taskId = ++room.aiTaskId;
        long turnStartTime = room.game.turnStartTime();
        String positionKey = SearchAi.knownPositionKey(room.game.board());
        Map<String, Integer> tacticalPenalties = new HashMap<>();
        for (Move candidate : room.game.currentActions()) {
            tacticalPenalties.put(candidate.notation(), room.game.repeatedTacticalRisk(candidate, aiColor));
        }
        Board searchBoard = room.game.board().copy();
        room.aiThinking = true;
        room.aiMoveReadyAt = 0;
        room.aiFuture = aiExecutor.submit(() -> {
            synchronized (this) {
                if (room.aiTaskId != taskId || mode != PlayMode.AI
                        || room.game.status() != GameStatus.PLAYING
                        || room.game.currentPlayer() != aiColor
                        || !positionKey.equals(SearchAi.knownPositionKey(room.game.board()))) {
                    return;
                }
            }
            Move move;
            try {
                move = room.searchAi.chooseMove(searchBoard, aiColor,
                        turnStartTime, AI_THINK_MILLIS,
                        candidate -> tacticalPenalties.getOrDefault(candidate.notation(), 0));
            } catch (RuntimeException e) {
                synchronized (this) {
                    if (room.aiTaskId == taskId) {
                        room.aiThinking = false;
                        room.aiFuture = null;
                        broadcast(room, aiColor.displayName() + "方AI计算失败：" + e.getMessage());
                    }
                }
                return;
            }
            finishAiTurn(room, mode, aiColor, taskId, positionKey, move);
        });
    }

    private void cancelAiTurn(Room room) {
        room.aiTaskId++;
        room.aiMoveReadyAt = 0;
        room.aiThinking = false;
        Future<?> future = room.aiFuture;
        room.aiFuture = null;
        if (future != null) {
            future.cancel(true);
        }
    }

    private synchronized void finishAiTurn(
            Room room,
            PlayMode mode,
            PlayerColor aiColor,
            long taskId,
            String positionKey,
            Move move) {
        if (room.aiTaskId != taskId) {
            return;
        }
        room.aiFuture = null;
        if (mode != PlayMode.AI || room.game.status() != GameStatus.PLAYING
                || room.game.currentPlayer() != aiColor
                || !positionKey.equals(SearchAi.knownPositionKey(room.game.board()))) {
            room.aiThinking = false;
            return;
        }
        if (move == null) {
            room.aiThinking = false;
            MoveResult noLegalResponse = room.game.adjudicateNoLegalResponseForCurrentPlayer();
            if (noLegalResponse != null) {
                broadcast(room, noLegalResponse.message());
                finishReplayIfNeeded(room, noLegalResponse.message());
                updateScoreIfEnded(room);
            }
            return;
        }
        String learningPosition = SearchAi.knownPositionKey(room.game.board());
        Piece capturedBeforeMove = room.game.board().get(move.destination());
        boolean capturedHiddenOpponentPiece = capturedBeforeMove != null
                && capturedBeforeMove.color() != aiColor
                && !capturedBeforeMove.visible();
        MoveResult result = room.game.play(move);
        room.aiThinking = false;
        if (result.success()) {
            try {
                room.record.recordMove(aiColor, move, result);
            } catch (IOException e) {
                System.out.println("记录AI走子失败: " + e.getMessage());
            }
            appendReplayStep(room, aiColor, move, result, capturedBeforeMove);
            sendVisibleMoveMessages(room, null, aiColor, MoveMessage.fromMove(move),
                    result, capturedHiddenOpponentPiece);
            recordDecision(room, aiColor, learningPosition, move);
            updateScoreIfEnded(room);
        } else {
            broadcast(room, aiColor.displayName() + "方AI走子失败：" + result.message());
        }
    }

    private void updateScoreIfEnded(Room room) {
        if (room.scoreRecordedForCurrentGame || room.game.status() == GameStatus.PLAYING) {
            return;
        }
        switch (room.game.status()) {
            case RED_WIN -> room.redScore++;
            case BLACK_WIN -> room.blackScore++;
            case DRAW -> room.drawScore++;
            case PLAYING -> {
            }
        }
        recordAiLearning(room);
        room.scoreRecordedForCurrentGame = true;
    }

    private void recordAiLearning(Room room) {
        if (room.learningRecorded) {
            return;
        }
        if (room.mode == PlayMode.AI) {
            PlayerColor humanColor = room.aiColor.opponent();
            List<ExperienceMemory.Decision> humanDecisions = room.learningDecisions.get(humanColor);
            if (!humanDecisions.isEmpty()) {
                experienceMemory.learn(humanDecisions, humanExperienceReward(room, humanColor));
            }
            room.learningRecorded = true;
            return;
        }
        for (PlayerColor color : PlayerColor.values()) {
            List<ExperienceMemory.Decision> decisions = room.learningDecisions.get(color);
            if (!decisions.isEmpty()) {
                experienceMemory.learn(decisions, ExperienceMemory.rewardFor(color, room.game.status()));
            }
        }
        room.learningRecorded = true;
    }

    private int humanExperienceReward(Room room, PlayerColor humanColor) {
        if (room.game.status() == GameStatus.DRAW) {
            return 25;
        }
        boolean humanWon = (humanColor == PlayerColor.RED && room.game.status() == GameStatus.RED_WIN)
                || (humanColor == PlayerColor.BLACK && room.game.status() == GameStatus.BLACK_WIN);
        return humanWon ? 220 : -35;
    }

    private void recordDecision(Room room, PlayerColor color, String positionKey, Move move) {
        room.learningDecisions.get(color).add(new ExperienceMemory.Decision(positionKey, move.notation()));
    }

    private void appendCell(StringBuilder sb, Room room, Position position) {
        Piece piece = room.game.board().get(position);
        sb.append("{");
        pair(sb, "x", String.valueOf((char) ('a' + position.x()))).append(",");
        pair(sb, "y", position.y()).append(",");
        if (piece == null) {
            pair(sb, "empty", true);
        } else {
            pair(sb, "empty", false).append(",");
            pair(sb, "color", colorName(piece.color())).append(",");
            pair(sb, "visible", piece.visible()).append(",");
            pair(sb, "piece", piece.visible() ? piece.type().displayName(piece.color()) : "■").append(",");
            pair(sb, "pieceType", piece.visible() ? pieceName(piece.type()) : "hidden");
        }
        sb.append("}");
    }

    private void appendLegalMoves(StringBuilder sb, Room room, PlayerColor yourColor) {
        sb.append("\"legalMoves\":[");
        boolean first = true;
        if (room.game.status() == GameStatus.PLAYING && yourColor == room.game.currentPlayer()) {
            for (Move move : room.game.currentActions()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("{");
                pair(sb, "source", move.source().notation()).append(",");
                pair(sb, "destination", move.destination().notation());
                sb.append("}");
            }
        }
        sb.append("]");
    }

    private void appendReplayStep(Room room, PlayerColor color, Move move, MoveResult result, Piece capturedBeforeMove) {
        if (room.currentReplay == null) {
            return;
        }
        ReplayStep step = new ReplayStep(
                color,
                move,
                result.message(),
                result.capture(),
                capturedBeforeMove == null ? null : capturedBeforeMove.copy(),
                room.game.board().copy(),
                room.game.currentPlayer(),
                ruleEngine.isInCheck(room.game.board(), room.game.currentPlayer()),
                room.game.status(),
                List.of());
        room.currentReplay.steps.add(step);
        if (result.gameOver()) {
            finishReplayIfNeeded(room, result.message());
        }
    }

    private void finishReplayIfNeeded(Room room, String resultMessage) {
        if (room.currentReplay == null || room.currentReplay.steps.isEmpty()) {
            return;
        }
        room.currentReplay.resultMessage = resultMessage;
        room.currentReplay.status = room.game.status();
        room.lastReplay = room.currentReplay;
        room.currentReplay = null;
    }

    private String replayJson(Room room) {
        return replayJson(room.lastReplay);
    }

    private String replayJson(ReplaySnapshot replay) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        pair(sb, "available", replay != null);
        if (replay == null) {
            sb.append("}");
            return sb.toString();
        }
        sb.append(",");
        pair(sb, "mode", modeName(replay.mode)).append(",");
        pair(sb, "firstPlayer", colorName(replay.firstPlayer)).append(",");
        pair(sb, "status", replay.status.name()).append(",");
        pair(sb, "resultMessage", replay.resultMessage == null ? "" : replay.resultMessage).append(",");
        sb.append("\"initialBoard\":");
        appendReplayBoard(sb, replay.initialBoard);
        sb.append(",\"steps\":[");
        for (int i = 0; i < replay.steps.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendReplayStepJson(sb, replay.steps.get(i), i + 1);
        }
        sb.append("]}");
        return sb.toString();
    }

    private String recordsJson() throws IOException {
        List<RecordSummary> summaries = recordSummaries();
        summaries.sort(Comparator.comparingLong((RecordSummary summary) -> summary.startTimestamp).reversed());
        StringBuilder sb = new StringBuilder();
        sb.append("{\"records\":[");
        for (int i = 0; i < summaries.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendRecordSummary(sb, summaries.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    private List<RecordSummary> recordSummaries() throws IOException {
        List<RecordSummary> summaries = new ArrayList<>();
        if (!Files.isDirectory(RECORDS_DIR)) {
            return summaries;
        }
        List<String> hiddenRecords = hiddenRecordNames();
        try (var files = Files.list(RECORDS_DIR)) {
            for (Path file : files.toList()) {
                if (hiddenRecords.contains(file.getFileName().toString())) {
                    continue;
                }
                RecordSummary summary = recordSummary(file);
                if (summary != null && summary.moveCount > 0) {
                    summaries.add(summary);
                }
            }
        }
        return summaries;
    }

    private RecordSummary recordSummary(Path file) {
        String fileName = file.getFileName().toString();
        if (!fileName.startsWith("game-") || !fileName.endsWith(".jsonl")) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty() || !"initial".equals(stringField(lines.get(0), "kind"))) {
                return null;
            }
            long start = longField(lines.get(0), "timestamp", Files.getLastModifiedTime(file).toMillis());
            long end = start;
            int moves = 0;
            boolean gameOver = false;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                moves++;
                end = longField(line, "timestamp", end);
                gameOver = booleanField(line, "gameOver", false);
            }
            String mode = stringField(lines.get(0), "mode");
            if (mode == null || mode.isBlank()) {
                mode = "unknown";
            }
            return new RecordSummary(fileName, mode, start, end, moves, gameOver);
        } catch (Exception e) {
            return null;
        }
    }

    private void appendRecordSummary(StringBuilder sb, RecordSummary summary) {
        sb.append("{");
        pair(sb, "file", summary.fileName).append(",");
        pair(sb, "mode", summary.mode).append(",");
        pair(sb, "modeLabel", recordModeLabel(summary.mode)).append(",");
        pair(sb, "startTime", formatRecordTime(summary.startTimestamp)).append(",");
        pair(sb, "endTime", formatRecordTime(summary.endTimestamp)).append(",");
        pair(sb, "title", formatRecordTime(summary.startTimestamp) + " - " + formatRecordTime(summary.endTimestamp)).append(",");
        pair(sb, "moveCount", summary.moveCount).append(",");
        pair(sb, "finished", summary.finished);
        sb.append("}");
    }

    private ReplaySnapshot replayFromRecord(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return null;
            }
            String initial = lines.get(0);
            PlayerColor firstPlayer = parseColor(stringField(initial, "firstPlayer"));
            PlayMode mode = parseMode(stringField(initial, "mode"));
            Board board = parseInitialRecordBoard(initial);
            ReplaySnapshot replay = new ReplaySnapshot(board.copy(), firstPlayer, mode);
            PlayerColor currentPlayer = firstPlayer;
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) {
                    continue;
                }
                PlayerColor color = parseColor(stringField(line, "color"));
                Position source = Position.parse(stringField(line, "source"));
                Position destination = Position.parse(stringField(line, "destination"));
                Move move = Move.move(source, destination, 0);
                Board before = board.copy();
                Piece captured = board.get(destination);
                Piece mover = board.remove(source);
                if (mover == null) {
                    continue;
                }
                boolean capture = captured != null;
                board.set(destination, mover);
                if (!mover.visible()) {
                    mover.reveal();
                }
                boolean gameOver = booleanField(line, "gameOver", false);
                GameStatus status = inferRecordStatus(board, color, captured, gameOver);
                currentPlayer = status == GameStatus.PLAYING ? color.opponent() : currentPlayer;
                String message = recordMoveMessage(color, move, captured, board, status);
                ReplayStep step = new ReplayStep(
                        color,
                        move,
                        message,
                        capture,
                        captured == null ? null : captured.copy(),
                        board.copy(),
                        currentPlayer,
                        status == GameStatus.PLAYING && ruleEngine.isInCheck(board, currentPlayer),
                        status,
                        analyzeReplayMove(before, board, move, color, captured));
                replay.steps.add(step);
                if (status != GameStatus.PLAYING) {
                    replay.status = status;
                    replay.resultMessage = message;
                }
            }
            if (replay.status == GameStatus.PLAYING && !replay.steps.isEmpty()) {
                replay.resultMessage = "本局记录到这里结束。";
            }
            return replay;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> analyzeReplayMove(Board before, Board after, Move move, PlayerColor color, Piece captured) {
        List<String> tags = new ArrayList<>();
        Piece moverBefore = before.get(move.source());
        Piece moverAfter = after.get(move.destination());
        if (moverBefore == null || moverAfter == null) {
            return tags;
        }
        int movedValue = replayPieceValue(moverAfter);
        boolean opponentCanTake = canCapture(after, move.destination(), color.opponent());
        boolean defended = canCaptureIgnoringOwn(after, move.destination(), color);
        if (opponentCanTake && !defended && movedValue >= 420) {
            tags.add("疑似送子：落子后重要棋子可被对手白吃");
        }
        if (captured == null && bestReplayCaptureValue(before, color) >= 520) {
            tags.add("疑似漏吃：本手前存在可吃的大子");
        }
        if (captured == null
                && ruleEngine.isInCheck(after, color.opponent())
                && moveGenerator.hasCheckEscape(after, color.opponent())) {
            tags.add("非杀将军：对方有解法，需要确认是否有后续收益");
        }
        if (moverBefore.visible() && moverBefore.type() == PieceType.KING
                && !ruleEngine.isInCheck(before, color)
                && replayKingDanger(after, color) >= replayKingDanger(before, color)) {
            tags.add("疑似将帅空走：未明显降低自身危险");
        }
        PieceType moveType = moverBefore.moveType();
        if (captured == null
                && (moveType == PieceType.ROOK || moveType == PieceType.CANNON || moveType == PieceType.KNIGHT)
                && !ruleEngine.isInCheck(after, color.opponent())
                && bestReplayCaptureValue(after, color) < 420
                && replayKingPressure(after, color) <= replayKingPressure(before, color) + 80) {
            tags.add("疑似大子空转：车炮马未形成吃子或将帅压力");
        }
        if (captured != null && captured.visible() && replayPieceValue(captured) + 160 < movedValue
                && canCapture(after, move.destination(), color.opponent())) {
            tags.add("交换存疑：用高价值棋子吃低价值棋子后可能被反吃");
        }
        int ownPressureGain = replayKingPressure(after, color) - replayKingPressure(before, color);
        int opponentPressureGain = replayKingPressure(after, color.opponent()) - replayKingPressure(before, color.opponent());
        if (ownPressureGain >= 900 && !ruleEngine.isInCheck(after, color.opponent())) {
            tags.add("好布局：本手增强了对将帅的车炮马兵协同压力");
        }
        if (opponentPressureGain >= 900 && !ruleEngine.isInCheck(before, color)) {
            tags.add("防守风险：本手后对方杀网压力上升，需要检查是否漏防");
        }
        if (captured != null && !captured.visible() && canCapture(after, move.destination(), color.opponent())) {
            tags.add("暗子风险：吃暗子后落点可被反吃，需要按最坏合理情况评估");
        }
        if (captured != null && replayPieceValue(captured) >= 420
                && replayKingDanger(after, color) + 800 < replayKingDanger(before, color)) {
            tags.add("好防守：吃掉关键攻击子或拆掉对方杀网");
        }
        if (captured != null && replayPieceValue(captured) >= 420
                && movedValue <= replayPieceValue(captured) + 160
                && !canCapture(after, move.destination(), color.opponent())) {
            tags.add("好换子：以可接受代价换掉对方关键大子");
        }
        return tags;
    }

    private int bestReplayCaptureValue(Board board, PlayerColor color) {
        int best = 0;
        for (Move action : moveGenerator.generateActions(board, color, 0)) {
            Piece target = board.get(action.destination());
            if (target == null || target.color() == color) {
                continue;
            }
            if (ruleEngine.canMoveAndKeepKingSafe(board, action.source(), action.destination(), color)) {
                best = Math.max(best, replayPieceValue(target));
            }
        }
        return best;
    }

    private boolean canCapture(Board board, Position target, PlayerColor color) {
        for (Move action : moveGenerator.generateActions(board, color, 0)) {
            if (action.destination().equals(target)
                    && ruleEngine.canMoveAndKeepKingSafe(board, action.source(), action.destination(), color)) {
                return true;
            }
        }
        return false;
    }

    private boolean canCaptureIgnoringOwn(Board board, Position target, PlayerColor color) {
        Piece protectedPiece = board.get(target);
        if (protectedPiece == null || protectedPiece.color() != color) {
            return false;
        }
        Board copy = board.copy();
        copy.remove(target);
        for (Position source : board.occupiedPositions()) {
            if (source.equals(target)) {
                continue;
            }
            Piece piece = board.get(source);
            if (piece != null && piece.color() == color
                    && ruleEngine.canMoveAndKeepKingSafe(copy, source, target, color)) {
                return true;
            }
        }
        return false;
    }

    private int replayKingDanger(Board board, PlayerColor color) {
        int danger = ruleEngine.isInCheck(board, color) ? 3_000 : 0;
        danger += replayKingPressure(board, color.opponent());
        return danger;
    }

    private int replayKingPressure(Board board, PlayerColor color) {
        Position king = board.findKing(color.opponent());
        if (king == null) {
            return 10_000;
        }
        int pressure = 0;
        for (Position source : board.occupiedPositions()) {
            Piece piece = board.get(source);
            if (piece == null || piece.color() != color) {
                continue;
            }
            int distance = Math.abs(source.x() - king.x()) + Math.abs(source.y() - king.y());
            pressure += Math.max(0, 8 - distance) * switch (piece.moveType()) {
                case ROOK -> 90;
                case CANNON -> 75;
                case KNIGHT -> 65;
                case PAWN -> 35;
                case GUARD, BISHOP -> 15;
                case KING -> 0;
            };
        }
        return pressure;
    }

    private int replayPieceValue(Piece piece) {
        return switch (piece.visible() ? piece.type() : piece.moveType()) {
            case KING -> 100_000;
            case ROOK -> 950;
            case CANNON -> 520;
            case KNIGHT -> 450;
            case GUARD -> 220;
            case BISHOP -> 200;
            case PAWN -> 120;
        };
    }

    private Board parseInitialRecordBoard(String initialLine) {
        Board board = new Board();
        String boardArray = arrayField(initialLine, "board");
        for (String object : objectItems(boardArray)) {
            Position position = Position.parse(stringField(object, "position"));
            PlayerColor color = parseColor(stringField(object, "color"));
            PieceType type = parsePieceType(stringField(object, "type"));
            PieceType hiddenMoveType = parsePieceType(stringField(object, "hiddenMoveType"));
            boolean visible = booleanField(object, "visible", false);
            board.set(position, new Piece(color, type, hiddenMoveType, visible));
        }
        return board;
    }

    private GameStatus inferRecordStatus(Board board, PlayerColor mover, Piece captured, boolean gameOver) {
        if (!gameOver) {
            return GameStatus.PLAYING;
        }
        if (captured != null && captured.type() == PieceType.KING) {
            return mover == PlayerColor.RED ? GameStatus.RED_WIN : GameStatus.BLACK_WIN;
        }
        PlayerColor opponent = mover.opponent();
        if (ruleEngine.isInCheck(board, opponent)
                && !new edu.jieqi.engine.MoveGenerator(ruleEngine).hasCheckEscape(board, opponent)) {
            return mover == PlayerColor.RED ? GameStatus.RED_WIN : GameStatus.BLACK_WIN;
        }
        return GameStatus.DRAW;
    }

    private String recordMoveMessage(PlayerColor color, Move move, Piece captured, Board board, GameStatus status) {
        StringBuilder message = new StringBuilder();
        message.append(color.displayName()).append("从 ")
                .append(move.source().notation()).append(" 走到 ")
                .append(move.destination().notation());
        if (captured != null) {
            message.append("，吃掉").append(captured.type().displayName(captured.color()));
        }
        PlayerColor opponent = color.opponent();
        if (status == GameStatus.PLAYING && ruleEngine.isInCheck(board, opponent)) {
            message.append("，将军");
        }
        if (status == GameStatus.RED_WIN || status == GameStatus.BLACK_WIN) {
            message.append("。").append(color.displayName()).append("获胜。");
        } else if (status == GameStatus.DRAW) {
            message.append("。本局判和。");
        } else {
            message.append("。");
        }
        return message.toString();
    }

    private Path safeRecordPath(String fileName) {
        if (fileName == null || fileName.isBlank()
                || fileName.contains("/") || fileName.contains("\\")
                || !fileName.startsWith("game-") || !fileName.endsWith(".jsonl")) {
            return null;
        }
        return RECORDS_DIR.resolve(fileName).normalize();
    }

    private List<String> hiddenRecordNames() throws IOException {
        List<String> names = new ArrayList<>();
        if (!Files.isRegularFile(HIDDEN_RECORDS_FILE)) {
            return names;
        }
        for (String line : Files.readAllLines(HIDDEN_RECORDS_FILE, StandardCharsets.UTF_8)) {
            String fileName = line.trim();
            if (safeRecordPath(fileName) != null && !names.contains(fileName)) {
                names.add(fileName);
            }
        }
        return names;
    }

    private void saveHiddenRecordNames(List<String> names) throws IOException {
        Files.createDirectories(RECORDS_DIR);
        List<String> safeNames = new ArrayList<>();
        for (String name : names) {
            String fileName = name == null ? "" : name.trim();
            if (safeRecordPath(fileName) != null && !safeNames.contains(fileName)) {
                safeNames.add(fileName);
            }
        }
        Files.write(HIDDEN_RECORDS_FILE, safeNames, StandardCharsets.UTF_8);
    }

    private String formatRecordTime(long timestamp) {
        return RECORD_TIME_FORMAT.format(Instant.ofEpochMilli(timestamp));
    }

    private String recordModeLabel(String mode) {
        if (MODE_AI.equals(mode)) {
            return "人机对战";
        }
        if (MODE_ONLINE.equals(mode)) {
            return "真人对战";
        }
        return "历史未标记";
    }

    private PlayerColor parseColor(String color) {
        return "black".equalsIgnoreCase(color) ? PlayerColor.BLACK : PlayerColor.RED;
    }

    private PieceType parsePieceType(String type) {
        return switch (type == null ? "" : type.toLowerCase()) {
            case "king" -> PieceType.KING;
            case "rook" -> PieceType.ROOK;
            case "knight" -> PieceType.KNIGHT;
            case "cannon" -> PieceType.CANNON;
            case "guard" -> PieceType.GUARD;
            case "bishop" -> PieceType.BISHOP;
            default -> PieceType.PAWN;
        };
    }

    private String stringField(String json, String key) {
        String value = rawField(json, key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return unescapeJson(value.substring(1, value.length() - 1));
        }
        return value;
    }

    private long longField(String json, String key, long fallback) {
        try {
            String value = stringField(json, key);
            return value == null || value.isBlank() ? fallback : Long.parseLong(value);
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private boolean booleanField(String json, String key, boolean fallback) {
        String value = stringField(json, key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private List<String> readStringArrayField(String json, String key) {
        List<String> values = new ArrayList<>();
        String arrayText = arrayField(json, key);
        int index = 0;
        while (index < arrayText.length()) {
            while (index < arrayText.length()
                    && (Character.isWhitespace(arrayText.charAt(index)) || arrayText.charAt(index) == ',')) {
                index++;
            }
            if (index >= arrayText.length()) {
                break;
            }
            if (arrayText.charAt(index) != '"') {
                break;
            }
            int start = index + 1;
            index = start;
            boolean escaped = false;
            StringBuilder sb = new StringBuilder();
            while (index < arrayText.length()) {
                char ch = arrayText.charAt(index);
                if (escaped) {
                    sb.append(switch (ch) {
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case '"' -> '"';
                        case '\\' -> '\\';
                        default -> ch;
                    });
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    values.add(sb.toString());
                    index++;
                    break;
                } else {
                    sb.append(ch);
                }
                index++;
            }
        }
        return values;
    }

    private String rawField(String json, String key) {
        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex + needle.length());
        if (colon < 0) {
            return null;
        }
        int index = colon + 1;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        if (index >= json.length()) {
            return null;
        }
        char first = json.charAt(index);
        if (first == '"') {
            int end = index + 1;
            boolean escaped = false;
            while (end < json.length()) {
                char ch = json.charAt(end);
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    return json.substring(index, end + 1);
                }
                end++;
            }
            return null;
        }
        int end = index;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        return json.substring(index, end);
    }

    private String arrayField(String json, String key) {
        String needle = "\"" + key + "\"";
        int keyIndex = json.indexOf(needle);
        int start = keyIndex < 0 ? -1 : json.indexOf('[', keyIndex + needle.length());
        if (start < 0) {
            return "";
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '[') {
                depth++;
            } else if (ch == ']') {
                depth--;
                if (depth == 0) {
                    return json.substring(start + 1, i);
                }
            }
        }
        return "";
    }

    private List<String> objectItems(String arrayText) {
        List<String> items = new ArrayList<>();
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < arrayText.length(); i++) {
            char ch = arrayText.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    items.add(arrayText.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return items;
    }

    private String unescapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (!escaped) {
                if (ch == '\\') {
                    escaped = true;
                } else {
                    sb.append(ch);
                }
                continue;
            }
            sb.append(switch (ch) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                case '"' -> '"';
                case '\\' -> '\\';
                default -> ch;
            });
            escaped = false;
        }
        if (escaped) {
            sb.append('\\');
        }
        return sb.toString();
    }

    private void appendReplayStepJson(StringBuilder sb, ReplayStep step, int turn) {
        sb.append("{");
        pair(sb, "turn", turn).append(",");
        pair(sb, "color", colorName(step.color)).append(",");
        pair(sb, "source", step.move.source().notation()).append(",");
        pair(sb, "destination", step.move.destination().notation()).append(",");
        pair(sb, "message", step.message).append(",");
        pair(sb, "capture", step.capture).append(",");
        pair(sb, "capturedType", step.captured == null ? "" : pieceName(step.captured.type())).append(",");
        pair(sb, "capturedColor", step.captured == null ? "" : colorName(step.captured.color())).append(",");
        pair(sb, "capturedHidden", step.captured != null && !step.captured.visible()).append(",");
        pair(sb, "currentPlayer", colorName(step.currentPlayer)).append(",");
        pair(sb, "currentInCheck", step.currentInCheck).append(",");
        pair(sb, "status", step.status.name()).append(",");
        sb.append("\"analysis\":");
        appendStringArray(sb, step.analysis);
        sb.append(",");
        sb.append("\"board\":");
        appendReplayBoard(sb, step.board);
        sb.append("}");
    }

    private void appendStringArray(StringBuilder sb, List<String> values) {
        sb.append("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            quote(sb, values.get(i));
        }
        sb.append("]");
    }

    private void appendReplayBoard(StringBuilder sb, Board board) {
        sb.append("[");
        boolean first = true;
        for (int y = Position.HEIGHT - 1; y >= 0; y--) {
            for (int x = 0; x < Position.WIDTH; x++) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                appendReplayCell(sb, board, new Position(x, y));
            }
        }
        sb.append("]");
    }

    private void appendReplayCell(StringBuilder sb, Board board, Position position) {
        Piece piece = board.get(position);
        sb.append("{");
        pair(sb, "x", String.valueOf((char) ('a' + position.x()))).append(",");
        pair(sb, "y", position.y()).append(",");
        if (piece == null) {
            pair(sb, "empty", true);
        } else {
            pair(sb, "empty", false).append(",");
            pair(sb, "color", colorName(piece.color())).append(",");
            pair(sb, "visible", piece.visible()).append(",");
            pair(sb, "wasHidden", !piece.visible()).append(",");
            pair(sb, "piece", piece.visible() ? piece.type().displayName(piece.color()) : "■").append(",");
            pair(sb, "pieceType", piece.visible() ? pieceName(piece.type()) : "hidden").append(",");
            pair(sb, "hiddenMoveType", pieceName(piece.hiddenMoveType()));
        }
        sb.append("}");
    }

    private String joinJson(String playerId, PlayerColor color, PlayMode mode, Room room, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        pair(sb, "playerId", playerId).append(",");
        pair(sb, "roomId", room.id).append(",");
        pair(sb, "roomName", room.name).append(",");
        pair(sb, "color", colorName(color)).append(",");
        pair(sb, "mode", modeName(mode)).append(",");
        pair(sb, "message", message);
        sb.append("}");
        return sb.toString();
    }

    private void broadcast(Room room, String message) {
        for (String playerId : room.players.keySet()) {
            addMessage(room, playerId, message);
        }
    }

    private void addMessage(Room room, String playerId, String message) {
        room.inbox.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(message);
    }

    private void leaveRoom(PlayMode mode, Room room, String playerId) throws IOException {
        PlayerColor color = room.players.remove(playerId);
        room.inbox.remove(playerId);
        if (color == null) {
            destroyRoomIfEmpty(mode, room);
            return;
        }
        room.playerIds.remove(color);
        destroyRoom(mode, room);
    }

    private void destroyRoomIfEmpty(PlayMode mode, Room room) throws IOException {
        if (room.players.isEmpty()) {
            destroyRoom(mode, room);
        }
    }

    private void destroyRoom(PlayMode mode, Room room) throws IOException {
        cancelAiTurn(room);
        finishReplayIfNeeded(room, "本局结束。");
        if (room.record != null) {
            room.record.close();
        }
        rooms.get(mode).remove(room);
    }

    private Room selectJoinRoom(PlayMode mode, String roomId, String action, String existingPlayerId) throws IOException {
        if (mode == PlayMode.AI) {
            Room existing = findPlayerRoom(mode, existingPlayerId);
            if (existing != null && !"create".equalsIgnoreCase(action)) {
                return existing;
            }
            if (!"create".equalsIgnoreCase(action) && roomId != null && !roomId.isBlank()) {
                return findRoom(mode, roomId);
            }
            return createRoom(mode);
        }

        if ("create".equalsIgnoreCase(action)) {
            return createRoom(mode);
        }
        if (roomId != null && !roomId.isBlank()) {
            return findRoom(mode, roomId);
        }
        Room existing = findPlayerRoom(mode, existingPlayerId);
        if (existing != null) {
            return existing;
        }
        for (Room room : rooms.get(mode)) {
            if (room.game.status() == GameStatus.PLAYING
                    && (!room.playerIds.containsKey(PlayerColor.RED)
                    || !room.playerIds.containsKey(PlayerColor.BLACK))) {
                return room;
            }
        }
        return createRoom(mode);
    }

    private Room roomForRequest(PlayMode mode, String roomId, String playerId) {
        Room room = findRoom(mode, roomId);
        if (room != null) {
            return room;
        }
        return findPlayerRoom(mode, playerId);
    }

    private Room findRoom(PlayMode mode, String roomId) {
        if (roomId == null || roomId.isBlank()) {
            return null;
        }
        for (Room room : rooms.get(mode)) {
            if (room.id.equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    private Room findPlayerRoom(PlayMode mode, String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        for (Room room : rooms.get(mode)) {
            if (room.players.containsKey(playerId)) {
                return room;
            }
        }
        return null;
    }

    private Room createRoom(PlayMode mode) throws IOException {
        int number = nextRoomNumber(mode);
        PlayerColor aiColor = mode == PlayMode.AI ? PlayerColor.BLACK : randomColor();
        PlayerColor firstPlayer = mode == PlayMode.AI ? nextAiFirstPlayer() : PlayerColor.RED;
        Room room = new Room("room-" + number, "房间 " + number, mode, aiColor, firstPlayer, randomColor());
        rooms.get(mode).add(room);
        return room;
    }

    private int nextRoomNumber(PlayMode mode) {
        int number = 1;
        while (findRoom(mode, "room-" + number) != null) {
            number++;
        }
        return number;
    }

    private PlayerColor nextAiFirstPlayer() {
        if (nextAiFirstPlayer == null) {
            nextAiFirstPlayer = randomColor();
        }
        PlayerColor color = nextAiFirstPlayer;
        nextAiFirstPlayer = nextAiFirstPlayer.opponent();
        return color;
    }

    private PlayerColor seatColorForJoin(Room room, PlayMode mode) {
        if (mode == PlayMode.AI) {
            PlayerColor humanColor = room.aiColor.opponent();
            return room.playerIds.containsKey(humanColor) ? null : humanColor;
        }
        if (room.playerIds.isEmpty() && !room.playerIds.containsKey(room.firstOnlinePlayerColor)) {
            return room.firstOnlinePlayerColor;
        }
        if (!room.playerIds.containsKey(PlayerColor.RED)) {
            return PlayerColor.RED;
        }
        if (!room.playerIds.containsKey(PlayerColor.BLACK)) {
            return PlayerColor.BLACK;
        }
        return null;
    }

    private void configureNextRoundSeats(Room room) {
        if (room.game.status() == GameStatus.PLAYING) {
            return;
        }
        if (room.mode == PlayMode.AI) {
            configureNextAiRoundSeats(room);
        } else {
            configureNextOnlineRoundSeats(room);
        }
    }

    private void configureNextAiRoundSeats(Room room) {
        GameStatus status = room.game.status();
        if (status == GameStatus.DRAW) {
            room.firstPlayer = room.firstPlayer.opponent();
        } else {
            PlayerColor winner = winnerColor(status);
            room.firstPlayer = winner.opponent();
        }
        room.aiColor = PlayerColor.BLACK;
        String humanId = room.playerIds.get(PlayerColor.RED);
        room.playerIds.clear();
        if (humanId != null) {
            room.playerIds.put(PlayerColor.RED, humanId);
            room.players.put(humanId, PlayerColor.RED);
        }
    }

    private void configureNextOnlineRoundSeats(Room room) {
        String previousRedId = room.playerIds.get(PlayerColor.RED);
        String previousBlackId = room.playerIds.get(PlayerColor.BLACK);
        if (previousRedId == null || previousBlackId == null) {
            room.firstOnlinePlayerColor = randomColor();
            return;
        }
        room.playerIds.clear();
        if (room.game.status() == GameStatus.DRAW) {
            putPlayerSeat(room, previousRedId, PlayerColor.BLACK);
            putPlayerSeat(room, previousBlackId, PlayerColor.RED);
            return;
        }
        PlayerColor winner = winnerColor(room.game.status());
        String winnerId = winner == PlayerColor.RED ? previousRedId : previousBlackId;
        String loserId = winner == PlayerColor.RED ? previousBlackId : previousRedId;
        putPlayerSeat(room, loserId, PlayerColor.RED);
        putPlayerSeat(room, winnerId, PlayerColor.BLACK);
    }

    private void putPlayerSeat(Room room, String playerId, PlayerColor color) {
        room.playerIds.put(color, playerId);
        room.players.put(playerId, color);
    }

    private PlayerColor winnerColor(GameStatus status) {
        return status == GameStatus.RED_WIN ? PlayerColor.RED : PlayerColor.BLACK;
    }

    private PlayerColor randomColor() {
        return random.nextBoolean() ? PlayerColor.RED : PlayerColor.BLACK;
    }

    private void announceRoundStart(Room room) {
        broadcast(room, "新一局开始。" + room.firstPlayer.displayName() + "方先手。");
        if (room.mode == PlayMode.AI) {
            broadcast(room, "本局" + room.aiColor.displayName() + "方由AI控制。");
        }
        for (Map.Entry<String, PlayerColor> entry : room.players.entrySet()) {
            if (entry.getValue() != null) {
                addSeatInfoMessage(room, entry.getKey(), entry.getValue());
            }
        }
    }

    private void addSeatInfoMessage(Room room, String playerId, PlayerColor color) {
        addMessage(room, playerId, "本局" + room.firstPlayer.displayName()
                + "方先手，你是" + color.displayName() + "方。");
        if (room.mode == PlayMode.AI) {
            String firstMoverName = room.aiColor == room.firstPlayer ? "AI" : "你";
            addMessage(room, playerId, room.aiColor.displayName() + "方由AI控制，本局由" + firstMoverName + "先走。");
        }
    }

    private void syncRooms(PlayMode mode) {
        for (Room room : rooms.get(mode)) {
            syncGameState(room, mode);
        }
    }

    private String roomsJson(PlayMode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        pair(sb, "mode", modeName(mode)).append(",");
        sb.append("\"rooms\":[");
        List<Room> roomList = rooms.get(mode);
        for (int i = 0; i < roomList.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            appendRoomSummary(sb, roomList.get(i));
        }
        sb.append("]}");
        return sb.toString();
    }

    private void appendRoomSummary(StringBuilder sb, Room room) {
        int spectators = 0;
        for (PlayerColor color : room.players.values()) {
            if (color == null) {
                spectators++;
            }
        }
        sb.append("{");
        pair(sb, "id", room.id).append(",");
        pair(sb, "name", room.name).append(",");
        pair(sb, "status", room.game.status().name()).append(",");
        pair(sb, "redOccupied", room.playerIds.containsKey(PlayerColor.RED)).append(",");
        pair(sb, "blackOccupied", room.playerIds.containsKey(PlayerColor.BLACK)).append(",");
        pair(sb, "spectators", spectators).append(",");
        boolean canJoin = room.game.status() == GameStatus.PLAYING
                && (room.mode == PlayMode.AI
                ? !room.playerIds.containsKey(room.aiColor.opponent())
                : (!room.playerIds.containsKey(PlayerColor.RED)
                || !room.playerIds.containsKey(PlayerColor.BLACK)));
        pair(sb, "canJoin", canJoin).append(",");
        pair(sb, "canSpectate", true);
        sb.append("}");
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream inputStream = exchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readRequired(String json, String key) {
        String value = readOptional(json, key);
        if (value == null) {
            throw new IllegalArgumentException("缺少字段: " + key);
        }
        return value;
    }

    private String readOptional(String json, String key) {
        try {
            return codec.readStringField(json, key);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, String> queryParams(HttpExchange exchange) {
        Map<String, String> params = new HashMap<>();
        String query = exchange.getRequestURI().getRawQuery();
        if (query == null || query.isBlank()) {
            return params;
        }
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length == 2 ? decode(parts[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        send(exchange, status, body, "application/json; charset=utf-8");
    }

    private void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String jsonError(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        pair(sb, "success", false).append(",");
        pair(sb, "message", message);
        sb.append("}");
        return sb.toString();
    }

    private StringBuilder pair(StringBuilder sb, String key, String value) {
        quote(sb, key).append(":");
        quote(sb, value);
        return sb;
    }

    private StringBuilder pair(StringBuilder sb, String key, int value) {
        quote(sb, key).append(":").append(value);
        return sb;
    }

    private StringBuilder pair(StringBuilder sb, String key, long value) {
        quote(sb, key).append(":").append(value);
        return sb;
    }

    private StringBuilder pair(StringBuilder sb, String key, boolean value) {
        quote(sb, key).append(":").append(value);
        return sb;
    }

    private StringBuilder quote(StringBuilder sb, String value) {
        sb.append("\"").append(escape(value == null ? "" : value)).append("\"");
        return sb;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String colorName(PlayerColor color) {
        if (color == null) {
            return "spectator";
        }
        return color == PlayerColor.RED ? "red" : "black";
    }

    private PlayMode parseMode(String mode) {
        return MODE_AI.equalsIgnoreCase(mode) ? PlayMode.AI : PlayMode.ONLINE;
    }

    private String modeName(PlayMode mode) {
        return mode == PlayMode.AI ? MODE_AI : MODE_ONLINE;
    }

    private String modeDisplayName(PlayMode mode) {
        return mode == PlayMode.AI ? "人机对战" : "真人在线对战";
    }

    private String pieceName(PieceType pieceType) {
        return switch (pieceType) {
            case KING -> "king";
            case ROOK -> "rook";
            case KNIGHT -> "knight";
            case CANNON -> "cannon";
            case PAWN -> "pawn";
            case GUARD -> "guard";
            case BISHOP -> "bishop";
        };
    }

    private void printUrls(int port) {
        System.out.println("本机访问: http://127.0.0.1:" + port + "/");
        for (String address : localIpv4Addresses()) {
            System.out.println("局域网访问: http://" + address + ":" + port + "/");
        }
    }

    private List<String> localIpv4Addresses() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                for (InetAddress address : Collections.list(networkInterface.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("读取本机 IP 地址失败: " + e.getMessage());
        }
        return addresses;
    }

    private enum PlayMode {
        ONLINE,
        AI
    }

    private static class Room {
        private final String id;
        private final String name;
        private final PlayMode mode;
        private Game game;
        private GameRecord record;
        private final Map<String, PlayerColor> players = new HashMap<>();
        private final Map<PlayerColor, String> playerIds = new HashMap<>();
        private final Map<String, List<String>> inbox = new HashMap<>();
        private final Map<PlayerColor, List<ExperienceMemory.Decision>> learningDecisions =
                new EnumMap<>(PlayerColor.class);
        private PlayerColor aiColor;
        private PlayerColor firstPlayer;
        private PlayerColor firstOnlinePlayerColor;
        private int redScore = 0;
        private int blackScore = 0;
        private int drawScore = 0;
        private boolean scoreRecordedForCurrentGame = false;
        private boolean learningRecorded = false;
        private long aiMoveReadyAt = 0;
        private boolean aiThinking = false;
        private long aiTaskId = 0;
        private Future<?> aiFuture;
        private final SearchAi searchAi = new SearchAi();
        private ReplaySnapshot currentReplay;
        private ReplaySnapshot lastReplay;

        private Room(
                String id,
                String name,
                PlayMode mode,
                PlayerColor aiColor,
                PlayerColor firstPlayer,
                PlayerColor firstOnlinePlayerColor) throws IOException {
            this.id = id;
            this.name = name;
            this.mode = mode;
            this.aiColor = aiColor;
            this.firstPlayer = firstPlayer;
            this.firstOnlinePlayerColor = firstOnlinePlayerColor;
            this.game = new Game(firstPlayer);
            learningDecisions.put(PlayerColor.RED, new ArrayList<>());
            learningDecisions.put(PlayerColor.BLACK, new ArrayList<>());
            record = new GameRecord(RECORDS_DIR, game.board(), firstPlayer,
                    mode == PlayMode.AI ? MODE_AI : MODE_ONLINE);
            currentReplay = new ReplaySnapshot(game.board().copy(), firstPlayer, mode);
        }
    }

    private static class ReplaySnapshot {
        private final Board initialBoard;
        private final PlayerColor firstPlayer;
        private final PlayMode mode;
        private final List<ReplayStep> steps = new ArrayList<>();
        private GameStatus status = GameStatus.PLAYING;
        private String resultMessage = "";

        private ReplaySnapshot(Board initialBoard, PlayerColor firstPlayer, PlayMode mode) {
            this.initialBoard = initialBoard;
            this.firstPlayer = firstPlayer;
            this.mode = mode;
        }
    }

    private static class ReplayStep {
        private final PlayerColor color;
        private final Move move;
        private final String message;
        private final boolean capture;
        private final Piece captured;
        private final Board board;
        private final PlayerColor currentPlayer;
        private final boolean currentInCheck;
        private final GameStatus status;
        private final List<String> analysis;

        private ReplayStep(
                PlayerColor color,
                Move move,
                String message,
                boolean capture,
                Piece captured,
                Board board,
                PlayerColor currentPlayer,
                boolean currentInCheck,
                GameStatus status,
                List<String> analysis) {
            this.color = color;
            this.move = move;
            this.message = message;
            this.capture = capture;
            this.captured = captured;
            this.board = board;
            this.currentPlayer = currentPlayer;
            this.currentInCheck = currentInCheck;
            this.status = status;
            this.analysis = analysis;
        }
    }

    private static class RecordSummary {
        private final String fileName;
        private final String mode;
        private final long startTimestamp;
        private final long endTimestamp;
        private final int moveCount;
        private final boolean finished;

        private RecordSummary(
                String fileName,
                String mode,
                long startTimestamp,
                long endTimestamp,
                int moveCount,
                boolean finished) {
            this.fileName = fileName;
            this.mode = mode;
            this.startTimestamp = startTimestamp;
            this.endTimestamp = endTimestamp;
            this.moveCount = moveCount;
            this.finished = finished;
        }
    }

    private String html() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>揭棋对弈</title>
                  <style>
                    :root { color-scheme: dark; font-family: "Microsoft YaHei", system-ui, sans-serif; }
                    body { margin: 0; background: #101214; color: #e8eaed; }
                    main { max-width: 980px; margin: 0 auto; padding: 24px; }
                    .hidden-screen { display: none !important; }
                    .mode-screen { min-height: calc(100vh - 48px); display: grid; align-content: center; gap: 18px; }
                    .mode-title { max-width: 620px; }
                    .mode-title h1 { font-size: 34px; margin: 0 0 10px; }
                    .mode-title p { margin: 0; color: #b7c0cc; line-height: 1.6; }
                    .mode-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; max-width: 760px; }
                    .mode-button {
                      text-align: left; border: 1px solid #30363d; border-radius: 8px; padding: 18px;
                      min-height: 128px; background: #171a1f; color: #e8eaed;
                      box-shadow: 0 12px 28px rgba(0,0,0,.28);
                    }
                    .mode-button strong { display: block; font-size: 22px; margin-bottom: 10px; }
                    .mode-button span { display: block; color: #aeb8c6; line-height: 1.55; }
                    .mode-button:hover { border-color: #58a6ff; background: #1b2028; }
                    .room-screen { display: grid; gap: 16px; }
                    .room-toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
                    .room-list { display: grid; gap: 10px; }
                    .room-row {
                      border: 1px solid #30363d; background: #171a1f; border-radius: 6px; padding: 12px;
                      display: grid; grid-template-columns: 1fr auto; gap: 12px; align-items: center;
                    }
                    .room-title { font-weight: 800; margin-bottom: 5px; }
                    .room-meta { color: #9aa4b2; font-size: 13px; line-height: 1.5; }
                    .empty-room { border: 1px dashed #30363d; border-radius: 6px; padding: 20px; color: #9aa4b2; }
                    .record-tabs { display: flex; flex-wrap: wrap; gap: 8px; }
                    .record-tabs button.active { background: #2f81f7; }
                    .record-bulk { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; color: #b7c0cc; }
                    .record-list { display: grid; gap: 10px; }
                    .record-row {
                      border: 1px solid #30363d; background: #171a1f; border-radius: 6px; padding: 12px;
                      display: grid; grid-template-columns: 1fr auto; gap: 12px; align-items: center;
                    }
                    .record-info { display: flex; gap: 10px; align-items: flex-start; }
                    .record-select { width: 18px; height: 18px; margin: 2px 0 0; flex: 0 0 auto; }
                    .record-title { font-weight: 800; margin-bottom: 5px; }
                    .record-meta { color: #9aa4b2; font-size: 13px; line-height: 1.5; }
                    header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 16px; }
                    h1 { font-size: 24px; margin: 0; }
                    .status { color: #b7c0cc; font-size: 14px; }
                    .check-banner {
                      display: none; margin-top: 8px; width: fit-content;
                      border: 1px solid rgba(244, 211, 94, .58); border-radius: 6px;
                      background: rgba(244, 211, 94, .12); color: #f4d35e;
                      padding: 5px 10px; font-weight: 900; letter-spacing: 2px;
                    }
                    .check-banner.visible { display: inline-flex; }
                    .metrics { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-bottom: 18px; }
                    .metric { border: 1px solid #30363d; background: #171a1f; border-radius: 6px; padding: 10px 12px; }
                    .metric .label { color: #8b949e; font-size: 12px; margin-bottom: 4px; }
                    .metric .value { font-size: 20px; font-weight: 700; }
                    .metric.warning .value { color: #f4d35e; }
                    .metric.danger .value { color: #ff4d5e; }
                    .layout { display: grid; grid-template-columns: minmax(620px, 1fr) 300px; gap: 20px; align-items: start; }
                    .board { position: relative; width: 650px; height: 710px; margin: 0 auto; user-select: none; touch-action: manipulation; }
                    .xiangqi-surface {
                      position: absolute; left: 54px; top: 48px; width: 540px; height: 598px;
                      border: 5px solid #8b521b; border-radius: 9px;
                      background:
                        radial-gradient(ellipse at 18% 13%, rgba(255,255,255,.30), transparent 24%),
                        radial-gradient(ellipse at 74% 78%, rgba(79,39,7,.25), transparent 34%),
                        repeating-linear-gradient(0deg, rgba(91,52,15,.20) 0 1px, transparent 1px 13px),
                        repeating-linear-gradient(7deg, rgba(139,83,25,.20) 0 2px, transparent 2px 17px),
                        linear-gradient(90deg, #c98937 0%, #f1c679 18%, #d49a47 38%, #f0c371 61%, #bd7a2d 100%);
                      box-shadow:
                        inset 0 0 0 2px rgba(255,219,139,.35),
                        inset 0 0 30px rgba(86,43,9,.52),
                        inset 0 8px 22px rgba(255,236,171,.20),
                        0 14px 34px rgba(0,0,0,.52);
                      overflow: hidden;
                    }
                    .xiangqi-surface::before {
                      content: ""; position: absolute; inset: 0; pointer-events: none; z-index: 0;
                      background:
                        radial-gradient(ellipse at 24% 22%, transparent 0 26%, rgba(84,45,12,.14) 27%, transparent 31%),
                        radial-gradient(ellipse at 58% 58%, transparent 0 31%, rgba(94,52,15,.12) 32%, transparent 36%),
                        radial-gradient(ellipse at 88% 18%, rgba(255,234,166,.22), transparent 18%),
                        linear-gradient(90deg, rgba(255,255,255,.10), transparent 12% 86%, rgba(88,43,8,.22));
                      mix-blend-mode: multiply;
                    }
                    .xiangqi-surface::after {
                      content: ""; position: absolute; inset: 8px; border-radius: 5px; pointer-events: none; z-index: 0;
                      box-shadow:
                        inset 0 0 0 1px rgba(70,38,12,.38),
                        inset 0 0 28px rgba(74,37,9,.28);
                    }
                    .xiangqi-svg { position: absolute; left: 22px; top: 22px; width: 464px; height: 522px; pointer-events: none; z-index: 0; filter: drop-shadow(0 1px 0 rgba(255,231,168,.26)); }
                    .file-label, .rank-label { position: absolute; color: #9fb7d7; font-weight: 700; font-size: 19px; text-align: center; line-height: 24px; }
                    .file-label { width: 44px; height: 24px; transform: translateX(-50%); }
                    .rank-label { width: 24px; height: 24px; transform: translateY(-50%); }
                    .point { position: absolute; width: 70px; height: 70px; border: 0; border-radius: 50%; background: transparent; transform: translate(-50%, -50%); cursor: pointer; z-index: 1; }
                    .point:hover { background: rgba(38, 185, 83, 0.16); }
                    .point.target { background: rgba(47, 129, 247, .13); box-shadow: inset 0 0 0 2px rgba(47, 129, 247, .28); }
                    .point.legal-target { background: rgba(220, 38, 38, .23); box-shadow: inset 0 0 0 3px rgba(248, 113, 113, .62), 0 0 0 5px rgba(127, 29, 29, .12); }
                    .piece {
                      position: absolute; width: 58px; height: 58px; border-radius: 50%; transform: translate(-50%, -50%);
                      z-index: 2; display: block; cursor: pointer; overflow: visible;
                      font-size: 30px; font-weight: 900; font-family: "STKaiti", "KaiTi", "SimSun", "Microsoft YaHei", serif;
                      background:
                        radial-gradient(circle at 31% 24%, rgba(255,255,255,.70) 0 8%, transparent 9%),
                        radial-gradient(circle at 38% 30%, rgba(255,237,163,.76) 0 28%, transparent 44%),
                        repeating-linear-gradient(16deg, rgba(118,69,17,.16) 0 2px, transparent 2px 8px),
                        radial-gradient(circle at 52% 47%, #f6cf76 0 43%, #cd8426 57%, #81470f 74%, #5a2e09 100%);
                      border: 1px solid #57300d;
                      box-shadow:
                        0 4px 0 #6b3a10,
                        0 9px 16px rgba(0,0,0,.55),
                        inset 0 0 0 3px #dfa94c,
                        inset 0 0 0 5px #2db45c,
                        inset 0 0 0 8px rgba(89,48,12,.42),
                        inset 0 3px 7px rgba(255,246,192,.62),
                        inset 0 -5px 9px rgba(94,44,6,.36);
                    }
                    .piece::before {
                      content: ""; position: absolute; inset: 6px; border-radius: 50%; pointer-events: none;
                      border: 2px solid rgba(111,64,16,.64);
                      box-shadow:
                        inset 0 2px 3px rgba(255,244,183,.55),
                        inset 0 -2px 4px rgba(82,38,7,.38);
                    }
                    .piece::after {
                      content: ""; position: absolute; inset: 10px; border-radius: 50%; pointer-events: none;
                      background:
                        linear-gradient(145deg, rgba(255,247,204,.22), transparent 35%),
                        repeating-linear-gradient(10deg, transparent 0 5px, rgba(111,65,16,.10) 5px 7px);
                    }
                    .piece:hover { filter: brightness(1.06); }
                    .piece:active { transform: translate(-50%, -47%); box-shadow: 0 2px 0 #6b3a10, 0 5px 10px rgba(0,0,0,.5), inset 0 0 0 3px #dfa94c, inset 0 0 0 5px #2db45c, inset 0 0 0 8px rgba(89,48,12,.42); }
                    .piece.piece-enter { animation: pieceDrop .24s ease-out; }
                    .piece.red { color: #c91e18; text-shadow: 0 1px 0 rgba(255,242,189,.50), 0 -1px 0 rgba(91,18,7,.35); }
                    .piece.black { color: #14234e; text-shadow: 0 1px 0 rgba(255,242,189,.50), 0 -1px 0 rgba(1,7,20,.32); }
                    .piece.hidden { font-size: 22px; }
                    .piece.red.hidden { color: #c91e18; }
                    .piece.black.hidden { color: #14234e; }
                    .piece-text {
                      position: absolute; left: 50%; top: 50%;
                      transform: translate(-50%, -51%);
                      width: 40px; height: 40px; line-height: 40px;
                      text-align: center; z-index: 1;
                    }
                    .piece.hidden .piece-text { transform: translate(-50%, -50%); font-family: "Microsoft YaHei", system-ui, sans-serif; }
                    .piece.selected { outline: 4px solid rgba(44,255,98,.82); outline-offset: 4px; box-shadow: 0 4px 0 #6b3a10, 0 0 0 5px rgba(44,255,98,.18), 0 10px 18px rgba(0,0,0,.55), inset 0 0 0 3px #dfa94c, inset 0 0 0 5px #2db45c, inset 0 0 0 8px rgba(89,48,12,.42), inset 0 3px 7px rgba(255,246,192,.62); }
                    .piece.capture-target { outline: 3px solid rgba(244, 211, 94, .82); outline-offset: 3px; }
                    .piece.legal-target { outline: 4px solid rgba(248, 113, 113, .92); outline-offset: 5px; box-shadow: 0 4px 0 #6b3a10, 0 0 0 7px rgba(220, 38, 38, .22), 0 10px 18px rgba(0,0,0,.55), inset 0 0 0 3px #dfa94c, inset 0 0 0 5px #2db45c, inset 0 0 0 8px rgba(89,48,12,.42), inset 0 3px 7px rgba(255,246,192,.62); }
                    .river-text { fill: rgba(197, 34, 28, .66); font: 900 28px "STKaiti", "KaiTi", "SimSun", serif; letter-spacing: 16px; filter: drop-shadow(0 1px 0 rgba(255,231,168,.45)); }
                    .panel { border: 1px solid #30363d; background: #171a1f; border-radius: 6px; padding: 14px; }
                    .panel h2 { margin: 0 0 10px; font-size: 16px; }
                    input { width: 100%; box-sizing: border-box; background: #0f1115; color: #e8eaed; border: 1px solid #30363d; border-radius: 4px; padding: 9px; margin: 6px 0; }
                    button { background: #2f81f7; color: white; border: 0; border-radius: 4px; padding: 9px 12px; cursor: pointer; margin: 4px 4px 4px 0; }
                    button.secondary { background: #30363d; }
                    button:disabled { opacity: .45; cursor: not-allowed; }
                    .note { color: #9aa4b2; font-size: 13px; line-height: 1.5; margin: 8px 0 12px; }
                    .log {
                      height: 220px; overflow-y: auto; overflow-x: hidden; overscroll-behavior: contain;
                      font-size: 13px; line-height: 1.5; color: #c7d0db; scrollbar-gutter: stable;
                      padding-right: 6px;
                    }
                    .log::-webkit-scrollbar { width: 9px; }
                    .log::-webkit-scrollbar-track { background: rgba(15,17,21,.7); border-radius: 999px; }
                    .log::-webkit-scrollbar-thumb { background: #3f4752; border-radius: 999px; }
                    .log::-webkit-scrollbar-thumb:hover { background: #566170; }
                    .log-line { padding: 4px 0; border-bottom: 1px solid rgba(48, 54, 61, .55); overflow-wrap: anywhere; }
                    .log-line.waiting { font-weight: 800; }
                    .log-line.red-msg { color: #ff6b78; }
                    .log-line.black-msg { color: #6ec6ff; }
                    .log-line.system-msg { color: #b7c0cc; }
                    .log-line.check-msg { color: #f4d35e; font-weight: 900; text-align: center; letter-spacing: 4px; }
                    .log-line.result-msg {
                      margin-top: 8px; padding: 8px 10px; border: 1px solid rgba(244,211,94,.42);
                      background: rgba(244,211,94,.08); border-radius: 4px; color: #f4d35e;
                      font-weight: 900; text-align: center;
                    }
                    .replay-controls { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
                    .replay-meta { color: #b7c0cc; font-size: 13px; line-height: 1.6; margin-bottom: 10px; }
                    .replay-panel .log { height: 380px; }
                    @keyframes pieceDrop {
                      from { opacity: .55; transform: translate(-50%, -64%) scale(.92); }
                      65% { opacity: 1; transform: translate(-50%, -47%) scale(1.04); }
                      to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
                    }
                    @media (max-width: 980px) { .layout { grid-template-columns: 1fr; } .metrics, .mode-actions { grid-template-columns: 1fr; } main { padding: 12px; } .board { transform: scale(.86); transform-origin: top left; height: 590px; } }
                  </style>
                </head>
                <body>
                <main>
                  <section id="modeScreen" class="mode-screen">
                    <div class="mode-title">
                      <h1>揭棋对弈</h1>
                      <p>请选择本局模式。人机对战适合单人练习和测试规则；真人在线对战适合两台电脑通过网页进入同一局。</p>
                    </div>
                    <div class="mode-actions">
                      <button class="mode-button" onclick="startMode('ai')">
                        <strong>人机对战</strong>
                        <span>你执红方，AI执黑方；第一局随机先后手，之后败方先走。</span>
                      </button>
                      <button class="mode-button" onclick="startMode('online')">
                        <strong>真人在线对战</strong>
                        <span>第一位玩家为红方，第二位玩家为黑方，之后进入者观战。</span>
                      </button>
                      <button class="mode-button" onclick="openRecords()">
                        <strong>棋谱</strong>
                        <span>查看已保存的人机对战、真人对战棋局，按时间选择任意一局回溯复盘。</span>
                      </button>
                    </div>
                  </section>

                  <section id="roomScreen" class="room-screen hidden-screen">
                    <div class="room-toolbar">
                      <div>
                        <h1 id="roomScreenTitle">真人在线房间</h1>
                        <div class="status" id="roomScreenStatus">选择一个房间加入、观战，或新建房间等待对手。</div>
                      </div>
                      <div>
                        <button class="secondary" onclick="backToMode()">模式选择</button>
                        <button onclick="createRoom()">新建房间</button>
                      </div>
                    </div>
                    <div id="roomList" class="room-list"></div>
                  </section>

                  <section id="recordScreen" class="room-screen hidden-screen">
                    <div class="room-toolbar">
                      <div>
                        <h1>棋谱</h1>
                        <div class="status">所有走过至少一步的棋局都会保存在这里，最新棋局排在最前。</div>
                      </div>
                      <div>
                        <button class="secondary" onclick="backToMode()">模式选择</button>
                      </div>
                    </div>
                    <div class="record-tabs">
                      <button id="recordTabAll" class="secondary active" onclick="setRecordFilter('all')">全部</button>
                      <button id="recordTabAi" class="secondary" onclick="setRecordFilter('ai')">人机对战</button>
                      <button id="recordTabOnline" class="secondary" onclick="setRecordFilter('online')">真人对战</button>
                      <button id="recordTabUnknown" class="secondary" onclick="setRecordFilter('unknown')">历史未标记</button>
                    </div>
                    <div class="record-bulk">
                      <button class="secondary" onclick="selectVisibleRecords()">全选当前</button>
                      <button class="secondary" onclick="clearRecordSelection()">取消选择</button>
                      <button onclick="deleteSelectedRecords()">删除所选</button>
                      <span id="recordSelectionStatus">已选 0 局</span>
                    </div>
                    <div id="recordList" class="record-list"></div>
                  </section>

                  <section id="gameScreen" class="game-screen hidden-screen">
                    <header>
                      <div>
                        <h1>揭棋对弈</h1>
                        <div class="status" id="status">正在加入...</div>
                        <div class="check-banner" id="checkBanner">将军</div>
                      </div>
                      <div>
                        <button class="secondary" onclick="backToRooms()">房间大厅</button>
                        <button class="secondary" onclick="backToMode()">模式选择</button>
                        <button class="secondary" onclick="exitRoom()">退出房间</button>
                        <button id="restartButton" onclick="restartGame()">重新对弈</button>
                      </div>
                    </header>
                    <section class="metrics">
                      <div class="metric"><div class="label">比分</div><div class="value" id="score">红 0 : 0 黑</div></div>
                      <div class="metric" id="turnTimerBox"><div class="label">本步剩余</div><div class="value" id="turnTimer">--:--</div></div>
                      <div class="metric"><div class="label">本局用时</div><div class="value" id="gameTimer">00:00</div></div>
                    </section>
                    <section class="layout">
                      <div class="board" id="board"></div>
                      <aside class="panel">
                        <h2>走子</h2>
                        <input id="source" placeholder="起点，例如 b2">
                        <input id="destination" placeholder="终点，例如 b9">
                        <button id="moveButton" onclick="sendMove()">走子</button>
                        <button id="resignButton" class="secondary" onclick="resign()">认输</button>
                        <div class="note">新规则不允许原地翻子。暗子移动或吃子后会自动翻开。</div>
                        <h2>消息</h2>
                        <div class="log" id="log"></div>
                        <button id="replayButton" class="secondary" onclick="openReplay()">棋局回溯</button>
                      </aside>
                    </section>
                  </section>
                  <section id="replayScreen" class="game-screen hidden-screen">
                    <header>
                      <div>
                        <h1>棋局回溯</h1>
                        <div class="status" id="replayStatus">上一局复盘</div>
                        <div class="check-banner" id="replayCheckBanner">将军</div>
                      </div>
                      <div>
                        <button id="replayCloseButton" class="secondary" onclick="closeReplay()">回到对局</button>
                      </div>
                    </header>
                    <section class="layout">
                      <div class="board" id="replayBoard"></div>
                      <aside class="panel replay-panel">
                        <h2>回放</h2>
                        <div class="replay-meta" id="replayMeta">暂无棋局</div>
                        <div class="replay-controls">
                          <button class="secondary" onclick="replayPrev()">上一步</button>
                          <button onclick="replayNext()">下一步</button>
                          <button id="replayPlayButton" class="secondary" onclick="toggleReplayPlay()">播放</button>
                        </div>
                        <h2>消息</h2>
                        <div class="log" id="replayLog"></div>
                      </aside>
                    </section>
                  </section>
                </main>
                <script>
                  let selectedMode = localStorage.getItem("jieqiMode") || "";
                  let selectedRoomId = selectedMode ? (localStorage.getItem(roomIdKey(selectedMode)) || "") : "";
                  let playerId = selectedMode && selectedRoomId ? (localStorage.getItem(playerIdKey(selectedMode, selectedRoomId)) || "") : "";
                  let selected = null;
                  let sendingMove = false;
                  let refreshing = false;
                  let restartInFlight = false;
                  let lastState = null;
                  let lastStateLocalTime = Date.now();
                  let lastMessageSignature = "";
                  let replayData = null;
                  let replayIndex = 0;
                  let replayTimer = null;
                  let replayReturnScreen = "game";
                  let recordsData = [];
                  let recordFilter = "all";
                  let selectedRecordFiles = new Set();
                  const files = ["a","b","c","d","e","f","g","h","i"];
                  const CELL = 58;
                  const OFFSET = 22;

                  function roomIdKey(mode) {
                    return "jieqiRoomId_" + mode;
                  }

                  function playerIdKey(mode, roomId) {
                    return "jieqiPlayerId_" + mode + "_" + roomId;
                  }

                  function clearStoredSession(mode, roomId) {
                    if (mode && roomId) {
                      localStorage.removeItem(playerIdKey(mode, roomId));
                    }
                    if (mode) {
                      localStorage.removeItem(roomIdKey(mode));
                    }
                  }

                  function showModeScreen() {
                    stopReplay();
                    document.getElementById("modeScreen").classList.remove("hidden-screen");
                    document.getElementById("roomScreen").classList.add("hidden-screen");
                    document.getElementById("recordScreen").classList.add("hidden-screen");
                    document.getElementById("gameScreen").classList.add("hidden-screen");
                    document.getElementById("replayScreen").classList.add("hidden-screen");
                  }

                  function showRoomScreen() {
                    stopReplay();
                    document.getElementById("modeScreen").classList.add("hidden-screen");
                    document.getElementById("roomScreen").classList.remove("hidden-screen");
                    document.getElementById("recordScreen").classList.add("hidden-screen");
                    document.getElementById("gameScreen").classList.add("hidden-screen");
                    document.getElementById("replayScreen").classList.add("hidden-screen");
                    document.getElementById("roomScreenTitle").textContent =
                      selectedMode === "ai" ? "人机对战房间" : "真人在线房间";
                    document.getElementById("roomScreenStatus").textContent =
                      selectedMode === "ai"
                        ? "新建一个人机房间开始对战，其他人可以进入观战。"
                        : "选择一个房间加入、观战，或新建房间等待对手。";
                  }

                  function showGameScreen() {
                    document.getElementById("modeScreen").classList.add("hidden-screen");
                    document.getElementById("roomScreen").classList.add("hidden-screen");
                    document.getElementById("recordScreen").classList.add("hidden-screen");
                    document.getElementById("gameScreen").classList.remove("hidden-screen");
                    document.getElementById("replayScreen").classList.add("hidden-screen");
                  }

                  function showRecordScreen() {
                    stopReplay();
                    document.getElementById("modeScreen").classList.add("hidden-screen");
                    document.getElementById("roomScreen").classList.add("hidden-screen");
                    document.getElementById("recordScreen").classList.remove("hidden-screen");
                    document.getElementById("gameScreen").classList.add("hidden-screen");
                    document.getElementById("replayScreen").classList.add("hidden-screen");
                  }

                  async function startMode(mode) {
                    selectedMode = mode;
                    selected = null;
                    lastState = null;
                    lastMessageSignature = "";
                    localStorage.setItem("jieqiMode", selectedMode);
                    selectedRoomId = localStorage.getItem(roomIdKey(selectedMode)) || "";
                    playerId = selectedRoomId ? (localStorage.getItem(playerIdKey(selectedMode, selectedRoomId)) || "") : "";
                    if (selectedRoomId && playerId) {
                      showGameScreen();
                      await refresh();
                      return;
                    }
                    showRoomScreen();
                    await loadRooms();
                  }

                  async function backToMode() {
                    selected = null;
                    lastState = null;
                    lastMessageSignature = "";
                    selectedMode = "";
                    showModeScreen();
                  }

                  async function backToRooms() {
                    selected = null;
                    lastState = null;
                    lastMessageSignature = "";
                    if (selectedMode !== "online" && selectedMode !== "ai") {
                      showModeScreen();
                      return;
                    }
                    showRoomScreen();
                    await loadRooms();
                  }

                  async function openRecords() {
                    selected = null;
                    lastState = null;
                    lastMessageSignature = "";
                    showRecordScreen();
                    await loadRecords();
                  }

                  function setRecordFilter(filter) {
                    recordFilter = filter;
                    renderRecords();
                  }

                  async function loadRecords() {
                    const resp = await fetch("/api/records");
                    if (!resp.ok) return;
                    const data = await resp.json();
                    recordsData = data.records || [];
                    const existingFiles = new Set(recordsData.map(record => record.file));
                    selectedRecordFiles = new Set(Array.from(selectedRecordFiles).filter(file => existingFiles.has(file)));
                    renderRecords();
                  }

                  function renderRecords() {
                    const list = document.getElementById("recordList");
                    if (!list) return;
                    for (const tab of ["All", "Ai", "Online", "Unknown"]) {
                      const button = document.getElementById("recordTab" + tab);
                      if (button) button.classList.remove("active");
                    }
                    const activeTab = recordFilter === "ai" ? "Ai"
                      : recordFilter === "online" ? "Online"
                      : recordFilter === "unknown" ? "Unknown" : "All";
                    const activeButton = document.getElementById("recordTab" + activeTab);
                    if (activeButton) activeButton.classList.add("active");
                    list.innerHTML = "";
                    const filtered = recordsData.filter(record =>
                      recordFilter === "all" ? true : record.mode === recordFilter);
                    if (!filtered.length) {
                      const empty = document.createElement("div");
                      empty.className = "empty-room";
                      empty.textContent = "暂无棋谱。";
                      list.appendChild(empty);
                      updateRecordSelectionStatus();
                      return;
                    }
                    for (const record of filtered) {
                      const row = document.createElement("div");
                      row.className = "record-row";
                      const info = document.createElement("div");
                      info.className = "record-info";
                      const checkbox = document.createElement("input");
                      checkbox.type = "checkbox";
                      checkbox.className = "record-select";
                      checkbox.checked = selectedRecordFiles.has(record.file);
                      checkbox.onchange = () => {
                        if (checkbox.checked) {
                          selectedRecordFiles.add(record.file);
                        } else {
                          selectedRecordFiles.delete(record.file);
                        }
                        updateRecordSelectionStatus();
                      };
                      const textWrap = document.createElement("div");
                      const title = document.createElement("div");
                      title.className = "record-title";
                      title.textContent = record.title;
                      const meta = document.createElement("div");
                      meta.className = "record-meta";
                      meta.textContent = record.modeLabel + "，" + record.moveCount + " 步，"
                        + (record.finished ? "已结束" : "未完局/中途退出");
                      textWrap.appendChild(title);
                      textWrap.appendChild(meta);
                      info.appendChild(checkbox);
                      info.appendChild(textWrap);
                      const actions = document.createElement("div");
                      const openButton = document.createElement("button");
                      openButton.textContent = "回溯";
                      openButton.onclick = () => openRecordReplay(record.file);
                      actions.appendChild(openButton);
                      row.appendChild(info);
                      row.appendChild(actions);
                      list.appendChild(row);
                    }
                    updateRecordSelectionStatus();
                  }

                  function visibleRecordFiles() {
                    return recordsData
                      .filter(record => recordFilter === "all" ? true : record.mode === recordFilter)
                      .map(record => record.file);
                  }

                  function selectVisibleRecords() {
                    for (const file of visibleRecordFiles()) {
                      selectedRecordFiles.add(file);
                    }
                    renderRecords();
                  }

                  function clearRecordSelection() {
                    selectedRecordFiles.clear();
                    renderRecords();
                  }

                  function updateRecordSelectionStatus() {
                    const status = document.getElementById("recordSelectionStatus");
                    if (status) status.textContent = "已选 " + selectedRecordFiles.size + " 局";
                  }

                  async function deleteSelectedRecords() {
                    const filesToDelete = Array.from(selectedRecordFiles);
                    if (!filesToDelete.length) {
                      alert("请先选择要删除的棋谱。");
                      return;
                    }
                    if (!confirm("确定删除已选的 " + filesToDelete.length + " 局棋谱吗？删除后不可恢复。")) {
                      return;
                    }
                    const password = prompt("请输入删除密码");
                    if (password === null) return;
                    const resp = await fetch("/api/delete-record", {
                      method: "POST",
                      headers: {"Content-Type": "application/json"},
                      body: JSON.stringify({files: filesToDelete, password})
                    });
                    if (!resp.ok) {
                      let message = "删除失败。";
                      try {
                        const data = await resp.json();
                        if (data.message) message = data.message;
                      } catch (ignored) {
                      }
                      alert(message);
                      return;
                    }
                    selectedRecordFiles.clear();
                    await loadRecords();
                  }

                  async function openRecordReplay(file) {
                    const resp = await fetch("/api/record-replay?file=" + encodeURIComponent(file));
                    if (!resp.ok) return;
                    const data = await resp.json();
                    if (!data.available) return;
                    replayData = data;
                    replayIndex = 0;
                    replayReturnScreen = "records";
                    stopReplay();
                    document.getElementById("replayCloseButton").textContent = "回到棋谱";
                    document.getElementById("recordScreen").classList.add("hidden-screen");
                    document.getElementById("gameScreen").classList.add("hidden-screen");
                    document.getElementById("replayScreen").classList.remove("hidden-screen");
                    renderReplay();
                  }

                  async function leaveCurrentRoom() {
                    if (!playerId || !selectedMode || !selectedRoomId) return;
                    const leavingMode = selectedMode;
                    const leavingRoomId = selectedRoomId;
                    const leavingPlayerId = playerId;
                    try {
                      await fetch("/api/leave", {
                        method: "POST",
                        headers: {"Content-Type": "application/json"},
                        body: JSON.stringify({playerId: leavingPlayerId, mode: leavingMode, roomId: leavingRoomId})
                      });
                    } catch (ignored) {
                    }
                    clearStoredSession(leavingMode, leavingRoomId);
                    playerId = "";
                    selectedRoomId = "";
                  }

                  async function exitRoom() {
                    const exitingMode = selectedMode;
                    await leaveCurrentRoom();
                    selected = null;
                    lastState = null;
                    lastMessageSignature = "";
                    stopReplay();
                    if (exitingMode === "online" || exitingMode === "ai") {
                      selectedMode = exitingMode;
                      localStorage.setItem("jieqiMode", selectedMode);
                      showRoomScreen();
                      await loadRooms();
                    } else {
                      localStorage.removeItem("jieqiMode");
                      selectedMode = "";
                      showModeScreen();
                    }
                  }

                  async function loadRooms() {
                    if (selectedMode !== "online" && selectedMode !== "ai") return;
                    const resp = await fetch("/api/rooms?mode=" + encodeURIComponent(selectedMode));
                    const data = await resp.json();
                    renderRooms(data.rooms || []);
                  }

                  function renderRooms(rooms) {
                    const list = document.getElementById("roomList");
                    list.innerHTML = "";
                    if (!rooms.length) {
                      const empty = document.createElement("div");
                      empty.className = "empty-room";
                      empty.textContent = "暂无房间。可以新建房间等待对手。";
                      list.appendChild(empty);
                      return;
                    }
                    for (const room of rooms) {
                      const row = document.createElement("div");
                      row.className = "room-row";
                      const info = document.createElement("div");
                      const title = document.createElement("div");
                      title.className = "room-title";
                      title.textContent = room.name + " · " + roomStatusText(room.status);
                      const meta = document.createElement("div");
                      meta.className = "room-meta";
                      meta.textContent = selectedMode === "ai"
                        ? "红方：" + seatText(room.redOccupied) + "，黑方：AI，观战：" + room.spectators
                        : "红方：" + seatText(room.redOccupied)
                          + "，黑方：" + seatText(room.blackOccupied)
                          + "，观战：" + room.spectators;
                      info.appendChild(title);
                      info.appendChild(meta);
                      const actions = document.createElement("div");
                      if (room.canJoin) {
                        const joinButton = document.createElement("button");
                        joinButton.textContent = "加入";
                        joinButton.onclick = () => joinRoom("join", room.id);
                        actions.appendChild(joinButton);
                      }
                      const watchButton = document.createElement("button");
                      watchButton.className = "secondary";
                      watchButton.textContent = "观战";
                      watchButton.onclick = () => joinRoom("spectate", room.id);
                      actions.appendChild(watchButton);
                      row.appendChild(info);
                      row.appendChild(actions);
                      list.appendChild(row);
                    }
                  }

                  async function createRoom() {
                    if (selectedMode !== "online" && selectedMode !== "ai") return;
                    await joinRoom("create", "");
                  }

                  async function joinRoom(action, roomId) {
                    if (!selectedMode) {
                      showModeScreen();
                      return;
                    }
                    if (playerId && selectedRoomId && selectedRoomId !== (roomId || "")) {
                      showGameScreen();
                      await refresh();
                      alert("请先点击“退出房间”，再加入或创建其他房间。");
                      return;
                    }
                    selectedRoomId = roomId || "";
                    playerId = selectedRoomId ? (localStorage.getItem(playerIdKey(selectedMode, selectedRoomId)) || "") : "";
                    const resp = await fetch("/api/join", {
                      method: "POST",
                      headers: {"Content-Type": "application/json"},
                      body: JSON.stringify({playerId, mode: selectedMode, roomId: selectedRoomId, action})
                    });
                    const data = await resp.json();
                    playerId = data.playerId;
                    selectedRoomId = data.roomId;
                    localStorage.setItem(roomIdKey(selectedMode), selectedRoomId);
                    localStorage.setItem(playerIdKey(selectedMode, selectedRoomId), playerId);
                    localStorage.setItem("jieqiMode", selectedMode);
                    showGameScreen();
                    await refresh();
                  }

                  async function refresh() {
                    if (!playerId || !selectedMode || !selectedRoomId) return;
                    if (refreshing) return;
                    refreshing = true;
                    try {
                      const resp = await fetch("/api/state?playerId=" + encodeURIComponent(playerId)
                        + "&mode=" + encodeURIComponent(selectedMode)
                        + "&roomId=" + encodeURIComponent(selectedRoomId));
                      if (!resp.ok) {
                        await handleRoomGone();
                        return;
                      }
                      const data = await resp.json();
                      render(data);
                    } finally {
                      refreshing = false;
                    }
                  }

                  async function handleRoomGone() {
                    const oldMode = selectedMode;
                    clearStoredSession(selectedMode, selectedRoomId);
                    selected = null;
                    playerId = "";
                    selectedRoomId = "";
                    lastState = null;
                    lastMessageSignature = "";
                    if (oldMode === "online" || oldMode === "ai") {
                      selectedMode = oldMode;
                      localStorage.setItem("jieqiMode", selectedMode);
                      showRoomScreen();
                      await loadRooms();
                    } else {
                      selectedMode = "";
                      localStorage.removeItem("jieqiMode");
                      showModeScreen();
                    }
                  }

                  function render(data) {
                    const previousState = lastState;
                    lastState = data;
                    lastStateLocalTime = Date.now();
                    document.getElementById("status").textContent =
                      modeText(data.mode) + " · " + data.roomName + "，你是 " + colorText(data.yourColor)
                      + aiSeatText(data)
                      + "，本局" + colorText(data.firstPlayer) + "先手，当前轮到 "
                      + colorText(data.currentPlayer) + "，状态 " + data.status;
                    document.getElementById("restartButton").disabled = !data.canRestart;
                    document.getElementById("restartButton").classList.toggle("secondary", !data.canRestart);
                    document.getElementById("replayButton").disabled = !data.hasReplay;
                    const isPlayer = data.yourColor === "red" || data.yourColor === "black";
                    const canAct = isPlayer && data.status === "PLAYING" && data.currentPlayer === data.yourColor;
                    if (!canAct) selected = null;
                    document.getElementById("moveButton").disabled = !canAct;
                    document.getElementById("resignButton").disabled = !isPlayer || data.status !== "PLAYING";
                    const checkBanner = document.getElementById("checkBanner");
                    checkBanner.classList.toggle("visible", Boolean(data.currentInCheck));
                    checkBanner.textContent = data.currentInCheck ? colorText(data.currentPlayer) + "被将军" : "将军";
                    document.getElementById("score").textContent =
                      "红 " + data.redScore + " : " + data.blackScore + " 黑" + (data.drawScore ? "，和 " + data.drawScore : "");
                    renderTimers();
                    const board = document.getElementById("board");
                    board.innerHTML = "";
                    drawBoardBase(board, data);
                    const surface = board.querySelector(".xiangqi-surface");
                    const previousBoard = boardMap(previousState ? previousState.board : []);
                    for (const cell of data.board) {
                      surface.appendChild(pointEl(cell));
                      if (!cell.empty) surface.appendChild(pieceEl(cell, previousBoard.get(cell.x + cell.y)));
                    }
                    renderMessages(data.messages, data);
                  }

                  function boardMap(cells) {
                    const map = new Map();
                    for (const cell of cells) {
                      map.set(cell.x + cell.y, cell);
                    }
                    return map;
                  }

                  function renderMessages(messages, data) {
                    const log = document.getElementById("log");
                    const terminalText = terminalResultText(data.status);
                    const signature = data.status + "|" + data.currentPlayer + "|"
                      + data.currentInCheck + "|" + messages.join("\\n") + "|" + terminalText;
                    if (signature === lastMessageSignature) {
                      return;
                    }
                    lastMessageSignature = signature;
                    log.innerHTML = "";
                    messages.forEach(message => appendMessageLines(log, message, data.status));
                    if (data.status === "PLAYING") {
                      if (data.currentInCheck) {
                        const checkLine = document.createElement("div");
                        checkLine.className = "log-line check-msg";
                        checkLine.textContent = colorText(data.currentPlayer) + "被将军";
                        log.appendChild(checkLine);
                      }
                      const waiting = document.createElement("div");
                      waiting.className = "log-line waiting " + (data.currentPlayer === "red" ? "red-msg" : "black-msg");
                      waiting.textContent = (data.currentPlayer === "red" ? "红方：" : "黑方：");
                      log.appendChild(waiting);
                    } else if (terminalText) {
                      const resultLine = document.createElement("div");
                      resultLine.className = "log-line result-msg";
                      resultLine.textContent = terminalText;
                      log.appendChild(resultLine);
                    }
                    log.scrollTop = log.scrollHeight;
                  }

                  function terminalResultText(status) {
                    if (status === "RED_WIN") return "本局结果：红方获胜";
                    if (status === "BLACK_WIN") return "本局结果：黑方获胜";
                    if (status === "DRAW") return "本局结果：和棋";
                    return "";
                  }

                  function appendMessageLines(log, message, status) {
                    let text = message;
                    const isRed = text.startsWith("红");
                    const isBlack = text.startsWith("黑");
                    const check = text.includes("将军");
                    text = text
                      .replace("，被将军", "")
                      .replace("被将军", "")
                      .replace("，将军", "")
                      .replace("将军", "")
                      .trim();
                    const line = document.createElement("div");
                    line.className = "log-line " + messageClass(message);
                    if (isRed) {
                      line.textContent = "红方：" + text.slice(1);
                    } else if (isBlack) {
                      line.textContent = "黑方：" + text.slice(1);
                    } else {
                      line.textContent = text;
                    }
                    if (line.textContent) {
                      log.appendChild(line);
                    }
                    if (check && status === "PLAYING") {
                      const checkLine = document.createElement("div");
                      checkLine.className = "log-line check-msg";
                      checkLine.textContent = "将军";
                      log.appendChild(checkLine);
                    }
                  }

                  function messageClass(message) {
                    if (message.startsWith("红")) return "red-msg";
                    if (message.startsWith("黑")) return "black-msg";
                    return "system-msg";
                  }

                  function drawBoardBase(board, data) {
                    const surface = document.createElement("div");
                    surface.className = "xiangqi-surface";
                    surface.innerHTML = boardSvg();
                    board.appendChild(surface);
                    for (let displayX = 0; displayX < files.length; displayX++) {
                      const file = isBlackPerspective(data) ? files[8 - displayX] : files[displayX];
                      board.appendChild(label(file, 54 + OFFSET + displayX * CELL, 14, "file-label"));
                      board.appendChild(label(file, 54 + OFFSET + displayX * CELL, 626, "file-label"));
                    }
                    for (let displayY = 0; displayY < 10; displayY++) {
                      const rank = isBlackPerspective(data) ? displayY : 9 - displayY;
                      const top = 48 + OFFSET + displayY * CELL;
                      board.appendChild(label(rank, 18, top, "rank-label"));
                      board.appendChild(label(rank, 588, top, "rank-label"));
                    }
                  }

                  function boardSvg() {
                    let lines = "";
                    for (let y = 0; y <= 9; y++) lines += `<line x1="0" y1="${y * CELL}" x2="${8 * CELL}" y2="${y * CELL}"/>`;
                    lines += `<line x1="0" y1="0" x2="0" y2="${9 * CELL}"/>`;
                    lines += `<line x1="${8 * CELL}" y1="0" x2="${8 * CELL}" y2="${9 * CELL}"/>`;
                    for (let x = 1; x <= 7; x++) {
                      lines += `<line x1="${x * CELL}" y1="0" x2="${x * CELL}" y2="${4 * CELL}"/>`;
                      lines += `<line x1="${x * CELL}" y1="${5 * CELL}" x2="${x * CELL}" y2="${9 * CELL}"/>`;
                    }
                    lines += `<line x1="${3 * CELL}" y1="0" x2="${5 * CELL}" y2="${2 * CELL}"/>`;
                    lines += `<line x1="${5 * CELL}" y1="0" x2="${3 * CELL}" y2="${2 * CELL}"/>`;
                    lines += `<line x1="${3 * CELL}" y1="${7 * CELL}" x2="${5 * CELL}" y2="${9 * CELL}"/>`;
                    lines += `<line x1="${5 * CELL}" y1="${7 * CELL}" x2="${3 * CELL}" y2="${9 * CELL}"/>`;
                    return `<svg class="xiangqi-svg" viewBox="0 0 ${8 * CELL} ${9 * CELL}" aria-hidden="true">
                      <g stroke="#3f2a15" stroke-width="2.2">${lines}</g>
                      <text class="river-text" x="${1.2 * CELL}" y="${4.62 * CELL}">楚河</text>
                      <text class="river-text" x="${5.25 * CELL}" y="${4.62 * CELL}">汉界</text>
                    </svg>`;
                  }

                  function pointEl(cell) {
                    const el = document.createElement("button");
                    el.type = "button";
                    el.className = "point";
                    if (selected && cell.empty) el.classList.add("target");
                    if (isLegalTarget(cell)) el.classList.add("legal-target");
                    placeAt(el, cell.x, cell.y);
                    el.onclick = () => {
                      handleBoardClick(cell);
                    };
                    return el;
                  }

                  function pieceEl(cell, previousCell) {
                    const el = document.createElement("button");
                    el.type = "button";
                    el.className = "piece " + cell.color + (cell.visible ? "" : " hidden");
                    if (selected && cell.color !== lastState.yourColor && isLegalTarget(cell)) {
                      el.classList.add("capture-target");
                    }
                    if (isLegalTarget(cell)) el.classList.add("legal-target");
                    if (isNewPieceAtPoint(cell, previousCell)) {
                      el.classList.add("piece-enter");
                    }
                    const text = document.createElement("span");
                    text.className = "piece-text";
                    text.textContent = cell.visible ? cell.piece : "暗";
                    el.appendChild(text);
                    const pos = cell.x + cell.y;
                    if (selected === pos) el.classList.add("selected");
                    placeAt(el, cell.x, cell.y);
                    el.onclick = () => {
                      handleBoardClick(cell);
                    };
                    return el;
                  }

                  function handleBoardClick(cell) {
                    if (!canActNow() || sendingMove) return;
                    const pos = cell.x + cell.y;
                    if (!cell.empty && canSelect(cell)) {
                      selected = selected === pos ? null : pos;
                      document.getElementById("source").value = selected || "";
                      document.getElementById("destination").value = "";
                      render(lastState);
                      return;
                    }
                    if (!selected || selected === pos) return;
                    document.getElementById("source").value = selected;
                    document.getElementById("destination").value = pos;
                    sendMove(selected, pos);
                  }

                  function isLegalTarget(cell) {
                    if (!selected || !lastState || !Array.isArray(lastState.legalMoves)) return false;
                    const pos = cell.x + cell.y;
                    return lastState.legalMoves.some(move => move.source === selected && move.destination === pos);
                  }

                  function canActNow() {
                    return lastState
                      && lastState.status === "PLAYING"
                      && lastState.yourColor === lastState.currentPlayer
                      && (lastState.yourColor === "red" || lastState.yourColor === "black");
                  }

                  function isNewPieceAtPoint(cell, previousCell) {
                    if (!previousCell || previousCell.empty) return Boolean(previousCell);
                    return previousCell.color !== cell.color
                      || previousCell.visible !== cell.visible
                      || previousCell.piece !== cell.piece
                      || previousCell.pieceType !== cell.pieceType;
                  }

                  function canSelect(cell) {
                    return lastState
                      && lastState.status === "PLAYING"
                      && lastState.yourColor === cell.color
                      && lastState.currentPlayer === cell.color;
                  }

                  function placeAt(el, file, rank) {
                    const sourceX = files.indexOf(file);
                    const displayX = isBlackPerspective(lastState) ? 8 - sourceX : sourceX;
                    const displayY = isBlackPerspective(lastState) ? rank : 9 - rank;
                    el.style.left = (OFFSET + displayX * CELL) + "px";
                    el.style.top = (OFFSET + displayY * CELL) + "px";
                  }

                  function isBlackPerspective(data) {
                    return data && data.yourColor === "black";
                  }

                  function label(text, left, top, className) {
                    const el = document.createElement("div");
                    el.className = className;
                    el.textContent = text;
                    el.style.left = left + "px";
                    el.style.top = top + "px";
                    return el;
                  }

                  async function openReplay() {
                    if (!lastState || !lastState.hasReplay) return;
                    const resp = await fetch("/api/replay?playerId=" + encodeURIComponent(playerId)
                      + "&mode=" + encodeURIComponent(selectedMode)
                      + "&roomId=" + encodeURIComponent(selectedRoomId));
                    if (!resp.ok) return;
                    const data = await resp.json();
                    if (!data.available) return;
                    replayData = data;
                    replayIndex = 0;
                    replayReturnScreen = "game";
                    stopReplay();
                    document.getElementById("replayCloseButton").textContent = "回到对局";
                    document.getElementById("gameScreen").classList.add("hidden-screen");
                    document.getElementById("replayScreen").classList.remove("hidden-screen");
                    renderReplay();
                  }

                  function closeReplay() {
                    stopReplay();
                    document.getElementById("replayScreen").classList.add("hidden-screen");
                    if (replayReturnScreen === "records") {
                      showRecordScreen();
                      renderRecords();
                    } else {
                      document.getElementById("gameScreen").classList.remove("hidden-screen");
                    }
                  }

                  function replayPrev() {
                    if (!replayData) return;
                    replayIndex = Math.max(0, replayIndex - 1);
                    renderReplay();
                  }

                  function replayNext() {
                    if (!replayData) return;
                    replayIndex = Math.min(replayData.steps.length, replayIndex + 1);
                    renderReplay();
                    if (replayIndex >= replayData.steps.length) stopReplay();
                  }

                  function toggleReplayPlay() {
                    if (replayTimer) {
                      stopReplay();
                      return;
                    }
                    if (!replayData) return;
                    if (replayIndex >= replayData.steps.length) replayIndex = 0;
                    replayTimer = setInterval(replayNext, 900);
                    document.getElementById("replayPlayButton").textContent = "暂停";
                  }

                  function stopReplay() {
                    if (replayTimer) {
                      clearInterval(replayTimer);
                      replayTimer = null;
                    }
                    const button = document.getElementById("replayPlayButton");
                    if (button) button.textContent = "播放";
                  }

                  function renderReplay() {
                    if (!replayData) return;
                    const frame = replayIndex === 0 ? null : replayData.steps[replayIndex - 1];
                    const boardCells = frame ? frame.board : replayData.initialBoard;
                    document.getElementById("replayStatus").textContent =
                      "第 " + replayIndex + " / " + replayData.steps.length + " 步，状态 " + (frame ? frame.status : "初始局面");
                    const checkBanner = document.getElementById("replayCheckBanner");
                    checkBanner.classList.toggle("visible", Boolean(frame && frame.currentInCheck));
                    checkBanner.textContent = frame && frame.currentInCheck ? colorText(frame.currentPlayer) + "被将军" : "将军";
                    document.getElementById("replayMeta").textContent =
                      "先手：" + colorText(replayData.firstPlayer) + "，结果：" + replayResultText(replayData.status);
                    const board = document.getElementById("replayBoard");
                    board.innerHTML = "";
                    drawBoardBase(board, {yourColor: "red"});
                    const surface = board.querySelector(".xiangqi-surface");
                    for (const cell of boardCells) {
                      if (!cell.empty) surface.appendChild(replayPieceEl(cell));
                    }
                    renderReplayMessages();
                  }

                  function replayPieceEl(cell) {
                    const el = document.createElement("div");
                    el.className = "piece " + cell.color + (cell.visible ? "" : " hidden");
                    const text = document.createElement("span");
                    text.className = "piece-text";
                    text.textContent = cell.visible ? cell.piece : "暗";
                    el.appendChild(text);
                    placeAtReplay(el, cell.x, cell.y);
                    return el;
                  }

                  function placeAtReplay(el, file, rank) {
                    const sourceX = files.indexOf(file);
                    el.style.left = (OFFSET + sourceX * CELL) + "px";
                    el.style.top = (OFFSET + (9 - rank) * CELL) + "px";
                  }

                  function renderReplayMessages() {
                    const log = document.getElementById("replayLog");
                    log.innerHTML = "";
                    const initial = document.createElement("div");
                    initial.className = "log-line system-msg";
                    initial.textContent = "初始局面：暗子真身已用于复盘显示。";
                    log.appendChild(initial);
                    for (let i = 0; i < replayIndex; i++) {
                      const step = replayData.steps[i];
                      const line = document.createElement("div");
                      line.className = "log-line " + (step.color === "red" ? "red-msg" : "black-msg");
                      line.textContent = step.turn + ". " + colorText(step.color) + " " + step.source + "-" + step.destination
                        + (step.capture ? "，吃" + colorText(step.capturedColor) + pieceTypeText(step.capturedType)
                          + (step.capturedHidden ? "（暗子）" : "") : "");
                      log.appendChild(line);
                      if (step.message) {
                        const msg = document.createElement("div");
                        msg.className = "log-line system-msg";
                        msg.textContent = step.message;
                        log.appendChild(msg);
                      }
                    }
                    if (replayIndex >= replayData.steps.length && replayData.resultMessage) {
                      const result = document.createElement("div");
                      result.className = "log-line result-msg";
                      result.textContent = replayData.resultMessage;
                      log.appendChild(result);
                    }
                    log.scrollTop = log.scrollHeight;
                  }

                  function replayResultText(status) {
                    if (status === "RED_WIN") return "红方胜";
                    if (status === "BLACK_WIN") return "黑方胜";
                    if (status === "DRAW") return "和棋";
                    return status;
                  }

                  function pieceTypeText(type) {
                    const names = {king: "将帅", rook: "车", knight: "马", cannon: "炮", pawn: "兵卒", guard: "士", bishop: "象"};
                    return names[type] || "";
                  }

                  async function sendMove(sourceOverride, destinationOverride) {
                    if (sendingMove) return;
                    const source = sourceOverride || document.getElementById("source").value.trim();
                    const destination = destinationOverride || document.getElementById("destination").value.trim();
                    if (!source || !destination) return;
                    sendingMove = true;
                    const body = {
                      playerId,
                      messageType: "move",
                      fromX: source[0],
                      fromY: Number(source.slice(1)),
                      toX: destination[0],
                      toY: Number(destination.slice(1)),
                      isFlip: false,
                      mode: selectedMode,
                      roomId: selectedRoomId
                    };
                    try {
                      await fetch("/api/move", {method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify(body)});
                      selected = null;
                      document.getElementById("source").value = "";
                      document.getElementById("destination").value = "";
                      await refresh();
                    } finally {
                      sendingMove = false;
                    }
                  }

                  async function resign() {
                    await fetch("/api/resign", {method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify({playerId, mode: selectedMode, roomId: selectedRoomId})});
                    await refresh();
                  }

                  async function restartGame() {
                    if (lastState && !lastState.canRestart) return;
                    if (restartInFlight) return;
                    restartInFlight = true;
                    const button = document.getElementById("restartButton");
                    button.disabled = true;
                    try {
                      await fetch("/api/restart", {method: "POST", headers: {"Content-Type": "application/json"}, body: JSON.stringify({playerId, mode: selectedMode, roomId: selectedRoomId})});
                      selected = null;
                      document.getElementById("source").value = "";
                      document.getElementById("destination").value = "";
                      await refresh();
                    } finally {
                      restartInFlight = false;
                      button.disabled = lastState ? !lastState.canRestart : false;
                    }
                  }

                  function renderTimers() {
                    if (!lastState) return;
                    const localDelta = lastState.status === "PLAYING" ? Date.now() - lastStateLocalTime : 0;
                    const approxServerNow = lastState.serverTime + localDelta;
                    const turnRemaining = Math.max(0, lastState.turnLimitMillis - (approxServerNow - lastState.turnStartTime));
                    const gameElapsed = lastState.gameElapsedMillis + localDelta;
                    document.getElementById("turnTimer").textContent = formatDuration(turnRemaining);
                    document.getElementById("gameTimer").textContent = formatDuration(gameElapsed);
                    const box = document.getElementById("turnTimerBox");
                    box.classList.toggle("danger", turnRemaining <= 30000 && lastState.status === "PLAYING");
                    box.classList.toggle("warning", turnRemaining > 30000 && turnRemaining <= 60000 && lastState.status === "PLAYING");
                  }

                  function formatDuration(ms) {
                    const total = Math.max(0, Math.floor(ms / 1000));
                    const minutes = Math.floor(total / 60);
                    const seconds = total % 60;
                    return String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");
                  }

                  function colorText(color) {
                    if (color === "red") return "红方";
                    if (color === "black") return "黑方";
                    return "观战";
                  }

                  function seatText(occupied) {
                    return occupied ? "已有人" : "空位";
                  }

                  function roomStatusText(status) {
                    if (status === "PLAYING") return "对弈中";
                    if (status === "RED_WIN") return "红方胜";
                    if (status === "BLACK_WIN") return "黑方胜";
                    if (status === "DRAW") return "和棋";
                    return status;
                  }

                  function modeText(mode) {
                    if (mode === "ai") return "人机对战";
                    return "真人在线对战";
                  }

                  function aiSeatText(data) {
                    if (data.mode !== "ai" || !data.aiColor) return "";
                    return "，AI执" + colorText(data.aiColor);
                  }

                  if (selectedMode && selectedRoomId && playerId) {
                    showGameScreen();
                    refresh();
                  } else if (selectedMode === "online" || selectedMode === "ai") {
                    showRoomScreen();
                    loadRooms();
                  } else {
                    showModeScreen();
                  }
                  setInterval(refresh, 500);
                  setInterval(() => {
                    if (!document.getElementById("roomScreen").classList.contains("hidden-screen")) {
                      loadRooms();
                    }
                  }, 1500);
                  setInterval(renderTimers, 250);
                </script>
                </body>
                </html>
                """;
    }
}
