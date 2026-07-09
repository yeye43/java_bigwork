package edu.jieqi.network;

import edu.jieqi.message.JsonCodec;
import edu.jieqi.message.MoveMessage;
import edu.jieqi.model.Move;
import edu.jieqi.model.Position;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class JieqiClient {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8887;

    private final JsonCodec codec = new JsonCodec();

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
        new JieqiClient().start(host, port);
    }

    public void start(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             Scanner scanner = new Scanner(System.in)) {

            Thread reader = new Thread(() -> readServerMessages(in), "server-reader");
            reader.setDaemon(true);
            reader.start();

            printHelp();
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                if ("quit".equalsIgnoreCase(line)) {
                    return;
                }
                if ("help".equalsIgnoreCase(line)) {
                    printHelp();
                    continue;
                }
                String json = toJsonCommand(line);
                if (json == null) {
                    System.out.println("未知命令。输入 help 查看帮助。");
                    continue;
                }
                out.println(json);
            }
        }
    }

    private void readServerMessages(BufferedReader in) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println();
                printServerMessage(line);
                System.out.print("> ");
            }
        } catch (IOException e) {
            System.out.println("服务器连接已断开。");
        }
    }

    private void printServerMessage(String json) {
        try {
            String messageType = codec.readStringField(json, "messageType");
            if ("board".equals(messageType)) {
                System.out.println(codec.readStringField(json, "board"));
                return;
            }
            if ("error".equals(messageType)) {
                System.out.println("[提示] " + codec.readStringField(json, "message"));
                return;
            }
            if ("moveResult".equals(messageType)) {
                System.out.println("[走子结果] " + codec.readStringField(json, "message"));
                return;
            }
            System.out.println("[server] " + json);
        } catch (Exception e) {
            System.out.println("[server] " + json);
        }
    }

    private String toJsonCommand(String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0].toLowerCase();
        try {
            if ("flip".equals(command)) {
                System.out.println("新规则不允许原地翻子，暗子只能在移动或吃子后翻开。");
                return null;
            }
            if ("move".equals(command) && parts.length == 3) {
                Move move = Move.move(Position.parse(parts[1]), Position.parse(parts[2]), 0);
                return codec.toJson(MoveMessage.fromMove(move));
            }
            if ("resign".equals(command) && parts.length == 1) {
                return "{\"messageType\":\"resign\"}";
            }
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private void printHelp() {
        System.out.println("""
                客户端命令:
                  move b2 b9   移动或吃子
                  resign       认输
                  help         显示帮助
                  quit         退出客户端
                """);
    }
}
