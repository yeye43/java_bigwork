package edu.jieqi.network;

import edu.jieqi.engine.Game;
import edu.jieqi.message.BoardMessage;
import edu.jieqi.message.ErrorMessage;
import edu.jieqi.message.GameStartMessage;
import edu.jieqi.message.JsonCodec;
import edu.jieqi.message.MoveMessage;
import edu.jieqi.message.MoveResultMessage;
import edu.jieqi.model.Move;
import edu.jieqi.model.MoveResult;
import edu.jieqi.model.Piece;
import edu.jieqi.model.PlayerColor;
import edu.jieqi.record.GameRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JieqiServer {
    private static final int DEFAULT_PORT = 8887;

    private final JsonCodec codec = new JsonCodec();
    private final Game game = new Game();
    private final GameRecord gameRecord;
    private ClientConnection redClient;
    private ClientConnection blackClient;

    public JieqiServer() throws IOException {
        this.gameRecord = new GameRecord(Path.of("records"));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                gameRecord.close();
            } catch (IOException ignored) {
            }
        }));
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new JieqiServer().start(port);
    }

    public void start(int port) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("揭棋服务器启动，端口 " + port);
            System.out.println("棋谱记录文件: " + gameRecord.file().toAbsolutePath());
            printConnectionHints(port);
            redClient = accept(serverSocket, PlayerColor.RED, "red-player");
            blackClient = accept(serverSocket, PlayerColor.BLACK, "black-player");
            sendGameStart();
            broadcastBoard();
            new Thread(redClient, "red-client").start();
            new Thread(blackClient, "black-client").start();
            System.out.println("双方已连接，对局开始。");
            waitForever();
        }
    }

    private ClientConnection accept(ServerSocket serverSocket, PlayerColor color, String playerId) throws IOException {
        System.out.println("等待" + color.displayName() + "方客户端连接...");
        Socket socket = serverSocket.accept();
        ClientConnection client = new ClientConnection(socket, color, playerId);
        client.send(codec.toJson(new ErrorMessage(0, "你是" + color.displayName() + "方。")));
        System.out.println(color.displayName() + "方已连接: " + socket.getRemoteSocketAddress());
        return client;
    }

    private void sendGameStart() {
        redClient.send(codec.toJson(new GameStartMessage("red-player", "black-player", "red", true)));
        blackClient.send(codec.toJson(new GameStartMessage("red-player", "black-player", "black", false)));
    }

    private void printConnectionHints(int port) {
        List<String> addresses = localIpv4Addresses();
        if (addresses.isEmpty()) {
            System.out.println("未找到局域网 IPv4 地址。客户端仍可尝试连接 127.0.0.1。");
            return;
        }
        System.out.println("局域网客户端可使用以下命令连接：");
        for (String address : addresses) {
            System.out.println("  powershell -ExecutionPolicy Bypass -File .\\client.ps1 " + address + " " + port);
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

    private synchronized void handleMessage(ClientConnection client, String json) {
        try {
            if (json.contains("\"messageType\":\"resign\"")) {
                MoveResult result = game.resign(client.color);
                broadcast(codec.toJson(new ErrorMessage(0, result.message())));
                return;
            }
            MoveMessage moveMessage = codec.parseMoveMessage(json);
            if (client.color != game.currentPlayer()) {
                client.send(codec.toJson(new ErrorMessage(2002, "未轮到本方走子。")));
                return;
            }
            Move move = moveMessage.toMove(game.turnStartTime());
            Piece capturedBeforeMove = game.board().get(move.destination());
            boolean capturedHiddenOpponentPiece = capturedBeforeMove != null
                    && capturedBeforeMove.color() != client.color
                    && !capturedBeforeMove.visible();
            MoveResult result = game.play(move);
            MoveResultMessage response = MoveResultMessage.fromResult(result, moveMessage);
            if (result.success()) {
                gameRecord.recordMove(client.color, move, result);
                sendMoveResultWithVisibility(client, response, moveMessage, capturedHiddenOpponentPiece);
                broadcastBoard();
            } else {
                client.send(codec.toJson(response));
            }
        } catch (Exception e) {
            client.send(codec.toJson(new ErrorMessage(4001, "JSON 格式错误或消息无法处理: " + e.getMessage())));
        }
    }

    private void broadcastBoard() {
        broadcast(codec.toJson(new BoardMessage(game.board().render())));
    }

    private void broadcast(String json) {
        redClient.send(json);
        blackClient.send(json);
    }

    private void sendMoveResultWithVisibility(
            ClientConnection actor,
            MoveResultMessage response,
            MoveMessage moveMessage,
            boolean capturedHiddenOpponentPiece) {
        ClientConnection opponent = actor.color == PlayerColor.RED ? blackClient : redClient;
        actor.send(codec.toJson(response));
        if (capturedHiddenOpponentPiece) {
            String hiddenMessage = actor.color.displayName() + "从 "
                    + moveMessage.fromX() + moveMessage.fromY()
                    + " 走到 " + moveMessage.toX() + moveMessage.toY()
                    + "，吃掉一个暗子。";
            opponent.send(codec.toJson(new MoveResultMessage(
                    response.success(),
                    response.valid(),
                    hiddenMessage,
                    moveMessage)));
        } else {
            opponent.send(codec.toJson(response));
        }
    }

    private void waitForever() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class ClientConnection implements Runnable {
        private final Socket socket;
        private final PlayerColor color;
        private final String playerId;
        private final BufferedReader in;
        private final PrintWriter out;

        private ClientConnection(Socket socket, PlayerColor color, String playerId) throws IOException {
            this.socket = socket;
            this.color = color;
            this.playerId = playerId;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleMessage(this, line);
                }
            } catch (IOException e) {
                broadcast(codec.toJson(new ErrorMessage(3002, playerId + " 断开连接。")));
            }
        }

        private void send(String json) {
            out.println(json);
        }
    }
}
