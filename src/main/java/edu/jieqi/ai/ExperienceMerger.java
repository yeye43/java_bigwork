package edu.jieqi.ai;

import java.nio.file.Path;

public class ExperienceMerger {
    public static void main(String[] args) {
        Path target = args.length > 0 ? Path.of(args[0]) : Path.of("records", "ai-learning.tsv");
        Path source = args.length > 1 ? Path.of(args[1]) : Path.of("records", "ai-training.tsv");
        ExperienceMemory memory = new ExperienceMemory(target);
        memory.mergeFrom(source);
        System.out.println("merged " + source + " -> " + target);
    }
}
