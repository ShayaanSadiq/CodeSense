package com.codesense.prototype.workshop;

import com.codesense.prototype.model.PseudoLine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class Parsons {

    private Parsons() {
    }

    public static void applyIndent(List<PseudoLine> lines) {
        if (lines == null) {
            return;
        }
        int indent = 0;
        for (PseudoLine line : lines) {
            String first = firstWord(line.getText());
            if ("UNTIL".equals(first) || "RETURN".equals(first) || "ELSE".equals(first) || "OUTPUT".equals(first)) {
                indent = 0;
            }
            line.setIndent(Math.max(0, indent));
            if ("FOR".equals(first) || "WHILE".equals(first)) {
                indent++;
            } else if ("IF".equals(first) && !combinedIf(line.getText())) {
                indent++;
            }
        }
    }

    public static List<PseudoLine> shuffle(List<PseudoLine> solution) {
        List<PseudoLine> puzzle = copy(solution, 0);
        if (puzzle.size() < 2) {
            return puzzle;
        }
        Random random = new Random();
        for (int i = puzzle.size() - 1; i > 0; i--) {
            Collections.swap(puzzle, i, random.nextInt(i + 1));
        }
        if (sameOrder(puzzle, solution)) {
            Collections.swap(puzzle, 0, 1);
        }
        return puzzle;
    }

    public static List<PseudoLine> canonicalOrder(List<PseudoLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        List<PseudoLine> ordered = new ArrayList<>(lines);
        ordered.sort(Comparator.comparingInt(line -> rank(verb(line.getText()))));
        return ordered;
    }

    public static List<PseudoLine> withCanonicalIndent(List<PseudoLine> lines) {
        List<PseudoLine> ordered = new ArrayList<>();
        for (PseudoLine line : canonicalOrder(lines)) {
            ordered.add(new PseudoLine(line.getId(), line.getText(), 0));
        }
        applyIndent(ordered);
        return ordered;
    }

    public static boolean orderMatches(List<PseudoLine> solution, List<PseudoLine> attempt) {
        return sameOrder(solution, attempt);
    }

    public static boolean matches(List<PseudoLine> solution, List<PseudoLine> attempt) {
        if (solution == null || attempt == null || solution.size() != attempt.size()) {
            return false;
        }
        for (int i = 0; i < solution.size(); i++) {
            PseudoLine expected = solution.get(i);
            PseudoLine got = attempt.get(i);
            if (!Objects.equals(expected.getId(), got.getId()) || expected.getIndent() != got.getIndent()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPermutation(List<PseudoLine> solution, List<PseudoLine> attempt) {
        if (solution == null || attempt == null || solution.size() != attempt.size()) {
            return false;
        }
        List<String> expected = solution.stream().map(PseudoLine::getId).sorted().toList();
        List<String> got = attempt.stream().map(PseudoLine::getId).sorted().toList();
        return expected.equals(got);
    }

    private static List<PseudoLine> copy(List<PseudoLine> lines, Integer indent) {
        List<PseudoLine> copy = new ArrayList<>();
        if (lines == null) {
            return copy;
        }
        for (PseudoLine line : lines) {
            copy.add(new PseudoLine(line.getId(), line.getText(), indent != null ? indent : line.getIndent()));
        }
        return copy;
    }

    private static boolean sameOrder(List<PseudoLine> left, List<PseudoLine> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (!Objects.equals(left.get(i).getId(), right.get(i).getId())) {
                return false;
            }
        }
        return true;
    }

    private static int rank(String verb) {
        return switch (verb) {
            case "INPUT" -> 0;
            case "SET" -> 1;
            case "FOR", "WHILE" -> 2;
            case "SEARCH", "SPLIT", "COMPUTE" -> 3;
            case "IF" -> 4;
            case "UNTIL" -> 5;
            case "RETURN" -> 6;
            case "OUTPUT" -> 7;
            default -> 3;
        };
    }

    private static String verb(String text) {
        String first = firstWord(text);
        if ("SET".equals(first) && ReferencePlan.isLoopUpdate(text)) {
            return "IF";
        }
        return first;
    }

    private static String firstWord(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().split("\\s+")[0].toUpperCase();
    }

    private static boolean combinedIf(String text) {
        return text != null && text.trim().matches("(?i)IF\\b.*,\\s*SET\\b.*");
    }
}
