package edu.jieqi.ai;

import edu.jieqi.engine.GameStatus;
import edu.jieqi.model.Move;
import edu.jieqi.model.PlayerColor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExperienceMemory {
    private static final int WIN_REWARD = 100;
    private static final int DRAW_REWARD = 12;
    private static final int MAX_BONUS = 180;

    private final Path file;
    private final Map<String, Entry> entries = new HashMap<>();
    private FileTime lastLoadedTime;

    public ExperienceMemory(Path file) {
        this.file = file;
        load();
    }

    public synchronized int bonus(String positionKey, Move move) {
        Entry entry = entries.get(key(positionKey, move.notation()));
        if (entry == null || entry.visits == 0) {
            return 0;
        }
        int average = entry.totalScore / entry.visits;
        int confidence = Math.min(20, entry.visits);
        return clamp(average * confidence / 2, -MAX_BONUS, MAX_BONUS);
    }

    public synchronized void learn(List<Decision> decisions, int gameReward) {
        if (decisions.isEmpty()) {
            return;
        }
        reloadIfChanged();
        int index = 0;
        for (Decision decision : decisions) {
            int weight = 50 + (50 * index / Math.max(1, decisions.size()));
            int reward = gameReward * weight / 100;
            Entry entry = entries.computeIfAbsent(key(decision.positionKey(), decision.moveNotation()),
                    ignored -> new Entry());
            entry.visits++;
            entry.totalScore += reward;
            index++;
        }
        save();
    }

    public synchronized void mergeFrom(Path otherFile) {
        if (!Files.exists(otherFile)) {
            return;
        }
        reloadIfChanged();
        Map<String, Entry> otherEntries = readEntries(otherFile);
        for (Map.Entry<String, Entry> other : otherEntries.entrySet()) {
            Entry entry = entries.computeIfAbsent(other.getKey(), ignored -> new Entry());
            entry.visits += other.getValue().visits;
            entry.totalScore += other.getValue().totalScore;
        }
        save();
    }

    public synchronized void mergeFrom(ExperienceMemory otherMemory) {
        reloadIfChanged();
        Map<String, Entry> snapshot = otherMemory.snapshot();
        for (Map.Entry<String, Entry> other : snapshot.entrySet()) {
            Entry entry = entries.computeIfAbsent(other.getKey(), ignored -> new Entry());
            entry.visits += other.getValue().visits;
            entry.totalScore += other.getValue().totalScore;
        }
        save();
    }

    public synchronized void reloadIfChanged() {
        try {
            if (!Files.exists(file)) {
                return;
            }
            FileTime currentTime = Files.getLastModifiedTime(file);
            if (lastLoadedTime != null && currentTime.equals(lastLoadedTime)) {
                return;
            }
            entries.clear();
            load();
        } catch (IOException e) {
            System.out.println("刷新AI学习文件失败: " + e.getMessage());
        }
    }

    public synchronized int entryCount() {
        reloadIfChanged();
        return entries.size();
    }

    public static int rewardForBlackWin(boolean blackWin, boolean draw) {
        if (draw) {
            return DRAW_REWARD;
        }
        return blackWin ? WIN_REWARD : -WIN_REWARD;
    }

    public static int rewardFor(PlayerColor color, GameStatus status) {
        if (status == GameStatus.DRAW) {
            return DRAW_REWARD;
        }
        boolean win = (color == PlayerColor.RED && status == GameStatus.RED_WIN)
                || (color == PlayerColor.BLACK && status == GameStatus.BLACK_WIN);
        return win ? WIN_REWARD : -WIN_REWARD;
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        entries.putAll(readEntries(file));
        try {
            lastLoadedTime = Files.getLastModifiedTime(file);
        } catch (IOException ignored) {
        }
    }

    private Map<String, Entry> readEntries(Path sourceFile) {
        Map<String, Entry> loaded = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length != 4) {
                    continue;
                }
                Entry entry = new Entry();
                entry.visits = Integer.parseInt(parts[2]);
                entry.totalScore = Integer.parseInt(parts[3]);
                loaded.put(key(parts[0], parts[1]), entry);
            }
        } catch (Exception e) {
            System.out.println("读取AI学习文件失败: " + e.getMessage());
        }
        return loaded;
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
                for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
                    String[] parts = mapEntry.getKey().split("\t", 2);
                    Entry entry = mapEntry.getValue();
                    writer.write(parts[0]);
                    writer.write('\t');
                    writer.write(parts[1]);
                    writer.write('\t');
                    writer.write(Integer.toString(entry.visits));
                    writer.write('\t');
                    writer.write(Integer.toString(entry.totalScore));
                    writer.newLine();
                }
            }
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            lastLoadedTime = Files.getLastModifiedTime(file);
        } catch (IOException e) {
            System.out.println("保存AI学习文件失败: " + e.getMessage());
        }
    }

    private Map<String, Entry> snapshot() {
        Map<String, Entry> snapshot = new HashMap<>();
        for (Map.Entry<String, Entry> mapEntry : entries.entrySet()) {
            Entry copy = new Entry();
            copy.visits = mapEntry.getValue().visits;
            copy.totalScore = mapEntry.getValue().totalScore;
            snapshot.put(mapEntry.getKey(), copy);
        }
        return snapshot;
    }

    private String key(String positionKey, String moveNotation) {
        return positionKey + "\t" + moveNotation;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class Entry {
        private int visits;
        private int totalScore;
    }

    public record Decision(String positionKey, String moveNotation) {
    }
}
