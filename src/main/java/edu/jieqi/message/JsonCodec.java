package edu.jieqi.message;

import java.util.LinkedHashMap;
import java.util.Map;

public class JsonCodec {
    public String toJson(MoveMessage message) {
        return "{"
                + pair("messageType", message.messageType()) + ","
                + pair("fromX", message.fromX()) + ","
                + pair("fromY", message.fromY()) + ","
                + pair("toX", message.toX()) + ","
                + pair("toY", message.toY()) + ","
                + pair("isFlip", message.flip())
                + "}";
    }

    public String toJson(MoveResultMessage message) {
        String json = "{"
                + pair("messageType", message.messageType()) + ","
                + pair("success", message.success()) + ","
                + pair("valid", message.valid()) + ","
                + pair("message", message.message()) + ","
                + "\"move\":" + toJson(message.move())
                + (message.flipResult() == null ? "" : "," + pair("flipResult", pieceName(message.flipResult())))
                + "}";
        return json;
    }

    public String toJson(GameStartMessage message) {
        return "{"
                + pair("messageType", message.messageType()) + ","
                + pair("redPlayerId", message.redPlayerId()) + ","
                + pair("blackPlayerId", message.blackPlayerId()) + ","
                + pair("yourColor", message.yourColor()) + ","
                + pair("firstHand", message.firstHand())
                + "}";
    }

    public String toJson(ErrorMessage message) {
        return "{"
                + pair("messageType", message.messageType()) + ","
                + pair("code", message.code()) + ","
                + pair("message", message.message())
                + "}";
    }

    public String toJson(BoardMessage message) {
        return "{"
                + pair("messageType", message.messageType()) + ","
                + pair("board", message.board())
                + "}";
    }

    public MoveMessage parseMoveMessage(String json) {
        Map<String, String> fields = parseFlatJson(json);
        String messageType = fields.get("messageType");
        if (!"move".equals(messageType)) {
            throw new IllegalArgumentException("不是 move 消息: " + messageType);
        }
        return new MoveMessage(
                required(fields, "fromX"),
                parseInt(fields, "fromY"),
                required(fields, "toX"),
                parseInt(fields, "toY"),
                parseBoolean(fields, "isFlip"));
    }

    public String readStringField(String json, String key) {
        return required(parseFlatJson(json), key);
    }

    private Map<String, String> parseFlatJson(String json) {
        String text = json.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new IllegalArgumentException("JSON 必须用 {} 包裹");
        }
        text = text.substring(1, text.length() - 1).trim();
        Map<String, String> fields = new LinkedHashMap<>();
        int index = 0;
        while (index < text.length()) {
            index = skipWhitespaceAndComma(text, index);
            if (index >= text.length()) {
                break;
            }
            ParsedString key = readString(text, index);
            index = skipWhitespace(text, key.nextIndex());
            if (index >= text.length() || text.charAt(index) != ':') {
                throw new IllegalArgumentException("JSON 字段缺少冒号");
            }
            index = skipWhitespace(text, index + 1);
            ParsedValue value = readValue(text, index);
            fields.put(key.value(), value.value());
            index = value.nextIndex();
        }
        return fields;
    }

    private ParsedString readString(String text, int start) {
        if (start >= text.length() || text.charAt(start) != '"') {
            throw new IllegalArgumentException("JSON 字符串必须以双引号开头");
        }
        StringBuilder sb = new StringBuilder();
        int index = start + 1;
        while (index < text.length()) {
            char ch = text.charAt(index);
            if (ch == '\\') {
                if (index + 1 >= text.length()) {
                    throw new IllegalArgumentException("JSON 转义不完整");
                }
                char escaped = text.charAt(index + 1);
                sb.append(switch (escaped) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case '\\' -> '\\';
                    case '"' -> '"';
                    default -> escaped;
                });
                index += 2;
            } else if (ch == '"') {
                return new ParsedString(sb.toString(), index + 1);
            } else {
                sb.append(ch);
                index++;
            }
        }
        throw new IllegalArgumentException("JSON 字符串未闭合");
    }

    private ParsedValue readValue(String text, int start) {
        if (start < text.length() && text.charAt(start) == '"') {
            ParsedString value = readString(text, start);
            return new ParsedValue(value.value(), value.nextIndex());
        }
        int index = start;
        while (index < text.length() && text.charAt(index) != ',') {
            index++;
        }
        return new ParsedValue(text.substring(start, index).trim(), index);
    }

    private int skipWhitespaceAndComma(String text, int index) {
        while (index < text.length() && (Character.isWhitespace(text.charAt(index)) || text.charAt(index) == ',')) {
            index++;
        }
        return index;
    }

    private int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private String required(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null) {
            throw new IllegalArgumentException("缺少字段: " + key);
        }
        return value;
    }

    private int parseInt(Map<String, String> fields, String key) {
        return Integer.parseInt(required(fields, key));
    }

    private boolean parseBoolean(Map<String, String> fields, String key) {
        return Boolean.parseBoolean(required(fields, key));
    }

    private String pair(String key, String value) {
        return "\"" + escape(key) + "\":\"" + escape(value) + "\"";
    }

    private String pair(String key, int value) {
        return "\"" + escape(key) + "\":" + value;
    }

    private String pair(String key, boolean value) {
        return "\"" + escape(key) + "\":" + value;
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String pieceName(edu.jieqi.model.PieceType pieceType) {
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

    private record ParsedString(String value, int nextIndex) {
    }

    private record ParsedValue(String value, int nextIndex) {
    }
}
