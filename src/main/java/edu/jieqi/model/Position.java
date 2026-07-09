package edu.jieqi.model;

import java.util.Objects;

public final class Position {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 10;

    private final int x;
    private final int y;

    public Position(int x, int y) {
        if (!isInside(x, y)) {
            throw new IllegalArgumentException("坐标越界: " + x + "," + y);
        }
        this.x = x;
        this.y = y;
    }

    public static boolean isInside(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public static Position parse(String text) {
        if (text == null || text.length() < 2 || text.length() > 3) {
            throw new IllegalArgumentException("坐标格式应为 a0 到 i9");
        }
        int x = Character.toLowerCase(text.charAt(0)) - 'a';
        int y;
        try {
            y = Integer.parseInt(text.substring(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("坐标格式应为 a0 到 i9");
        }
        return new Position(x, y);
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public String notation() {
        return String.valueOf((char) ('a' + x)) + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position position)) {
            return false;
        }
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return notation();
    }
}
