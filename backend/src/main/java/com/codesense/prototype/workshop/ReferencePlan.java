package com.codesense.prototype.workshop;

import com.codesense.prototype.model.PseudoLine;
import com.codesense.prototype.model.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hidden gold-standard plan, built when the lab is entered. The student never
 * sees this list. Correct Socratic answers unlock slices of it into the visible
 * draft, in canonical order.
 */
public final class ReferencePlan {

    private ReferencePlan() {
    }

    public static List<PseudoLine> build(String labQuestion) {
        String lab = labQuestion == null ? "" : labQuestion.toLowerCase(Locale.ROOT);
        List<PseudoLine> lines = new ArrayList<>();
        if (Socratic.computeOnceLab(labQuestion)) {
            boolean square = lab.contains("square") || (lab.contains("area") && lab.contains("side"));
            if (square) {
                add(lines, "INPUT the side", 0);
                add(lines, "COMPUTE the result in one step", 0);
                add(lines, "OUTPUT the area", 0);
            } else {
                add(lines, "INPUT the length and the width", 0);
                add(lines, "COMPUTE the result in one step", 0);
                add(lines, "OUTPUT the area", 0);
            }
            return lines;
        }
        if (lab.contains("vowel")) {
            add(lines, "INPUT a string", 0);
            add(lines, "SET count to 0", 0);
            add(lines, "FOR each letter in the text", 0);
            add(lines, "IF this letter is a vowel", 1);
            add(lines, "SET count to count + 1", 2);
            add(lines, "UNTIL there are no more letters", 0);
            add(lines, "OUTPUT the number of vowels", 0);
            return lines;
        }
        if (lab.contains("even") || (lab.contains("count") && !lab.contains("vowel"))) {
            add(lines, "INPUT a list of numbers", 0);
            add(lines, "SET count to 0", 0);
            add(lines, "FOR each number in the list", 0);
            add(lines, "IF this number is even", 1);
            add(lines, "SET count to count + 1", 2);
            add(lines, "UNTIL there are no more items", 0);
            add(lines, "OUTPUT the count", 0);
            return lines;
        }
        boolean smallest = lab.contains("smallest") || lab.contains("min");
        String name = smallest ? "min" : "max";
        String cmp = smallest ? "smaller than" : "greater than";
        String out = smallest ? "the smallest number" : "the largest number";
        add(lines, "INPUT a list of numbers", 0);
        add(lines, "SET " + name + " to the first number", 0);
        add(lines, "FOR each number in the list", 0);
        add(lines, "IF this number is " + cmp + " " + name, 1);
        add(lines, "SET " + name + " to this number", 2);
        add(lines, "UNTIL there are no more items", 0);
        add(lines, "OUTPUT " + out, 0);
        return lines;
    }

    public static void reveal(Session session, int completedStage) {
        List<PseudoLine> reference = session.getReferenceLines();
        if (reference == null || reference.isEmpty()) {
            return;
        }
        List<PseudoLine> draft = new ArrayList<>();
        for (PseudoLine line : reference) {
            if (stageOf(line.getText()) <= completedStage) {
                draft.add(copy(line));
            }
        }
        session.setConfirmedLines(draft);
    }

    public static List<PseudoLine> finished(Session session) {
        List<PseudoLine> reference = session.getReferenceLines();
        if (reference == null || reference.isEmpty()) {
            return Parsons.withCanonicalIndent(session.getConfirmedLines());
        }
        List<PseudoLine> copy = new ArrayList<>();
        for (PseudoLine line : reference) {
            copy.add(copy(line));
        }
        return copy;
    }

    static int stageOf(String text) {
        String first = firstWord(text);
        if ("INPUT".equals(first)) {
            return 1;
        }
        if ("FOR".equals(first) || "WHILE".equals(first) || "COMPUTE".equals(first)
                || "SEARCH".equals(first) || "SPLIT".equals(first)) {
            return 2;
        }
        if (isLoopUpdate(text) || "IF".equals(first)) {
            return 4;
        }
        if ("SET".equals(first)) {
            return 3;
        }
        if ("UNTIL".equals(first)) {
            return 5;
        }
        return 6;
    }

    static boolean isLoopUpdate(String text) {
        return text != null && text.trim().matches("(?i)SET\\s+\\S+\\s+to\\s+(this\\b|count \\+ 1).*");
    }

    private static void add(List<PseudoLine> lines, String text, int indent) {
        lines.add(new PseudoLine("R" + (lines.size() + 1), text, indent));
    }

    private static PseudoLine copy(PseudoLine line) {
        return new PseudoLine(line.getId(), line.getText(), line.getIndent());
    }

    private static String firstWord(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
    }
}
