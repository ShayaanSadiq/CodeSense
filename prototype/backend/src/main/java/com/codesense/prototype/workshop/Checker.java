package com.codesense.prototype.workshop;

import com.codesense.prototype.model.PseudoLine;
import com.codesense.prototype.model.Slot;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class Checker {

    private static final Pattern IMPERATIVE =
            Pattern.compile("\\b(SET|FOR|WHILE|IF|ELSE|ELIF|RETURN|INPUT|OUTPUT|THEN)\\b");
    private static final Pattern NAMED_ALGORITHM = Pattern.compile(
            "\\b(bubble sort|merge sort|quick\\s*sort|insertion sort|selection sort|binary search|linear search|two[- ]pointers?|kadane|dijkstra|dfs|bfs|dynamic programming)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ASSIGNMENT = Pattern.compile("\\b[A-Za-z_]\\w*\\s*=\\s*[^=]\\w*");
    private static final Pattern FOR_EACH = Pattern.compile("\\bfor\\s+each\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DONT_KNOW = Pattern.compile(
            "^(i\\s+don'?t\\s+know|dont know|idk|no idea|not sure|help me|hint)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NAMES_VALUE = Pattern.compile(
            "\\b(max|min|count|sum|total|so far|remember|keep|track|biggest|largest|smallest)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SAYS_HOW = Pattern.compile(
            "\\b(first|start|starts|initial|zero|0|greater|less|update|change|if|when)\\b",
            Pattern.CASE_INSENSITIVE);

    private Checker() {
    }

    public static boolean questionLeaksStep(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty() || q.contains("```")) {
            return true;
        }
        return NAMED_ALGORITHM.matcher(q).find()
                || FOR_EACH.matcher(q).find()
                || ASSIGNMENT.matcher(q).find()
                || IMPERATIVE.matcher(q).find();
    }

    public static boolean sameQuestion(String left, String right) {
        return normalizeQuestion(left).equals(normalizeQuestion(right)) && !normalizeQuestion(left).isEmpty();
    }

    public static boolean alreadyAsked(String candidate, List<String> asked) {
        if (candidate == null || candidate.isBlank()) {
            return true;
        }
        if (asked == null) {
            return false;
        }
        return asked.stream().anyMatch(previous -> sameQuestion(candidate, previous));
    }

    public static boolean staysOnSlot(Slot slot, String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty() || questionLeaksStep(q)) {
            return false;
        }
        if (slot == Slot.IO) {
            if (Pattern.compile("\\b(empty|only one|one element|compare|loop|walk|each item|algorithm)\\b",
                    Pattern.CASE_INSENSITIVE).matcher(q).find()) {
                return false;
            }
            return Pattern.compile("\\b(input|output|comes in|comes out|given|print|receive|show|value|result)\\b",
                    Pattern.CASE_INSENSITIVE).matcher(q).find();
        }
        return true;
    }

    public static String normalizeQuestion(String question) {
        if (question == null) {
            return "";
        }
        return question.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
    }

    public static boolean isDontKnow(String answer) {
        String a = answer == null ? "" : answer.trim().toLowerCase();
        if (a.isEmpty() || a.length() < 4) {
            return true;
        }
        return DONT_KNOW.matcher(a).find();
    }

    public static boolean memoryAnswerIsThin(String answer) {
        String a = answer == null ? "" : answer.toLowerCase();
        return NAMES_VALUE.matcher(a).find() && !SAYS_HOW.matcher(a).find();
    }

    public static boolean detectRepetition(String answer, List<PseudoLine> lines) {
        String blob = (answer == null ? "" : answer) + " " + lines.stream()
                .map(PseudoLine::getText)
                .collect(Collectors.joining(" "));
        if (Pattern.compile("\\bCOMPUTE\\b").matcher(blob).find()
                && Pattern.compile("\\bonce\\b", Pattern.CASE_INSENSITIVE).matcher(blob).find()) {
            return false;
        }
        if (Pattern.compile("\\b(just calculat|formula|no loop|no repeat|one step|directly)\\b", Pattern.CASE_INSENSITIVE)
                .matcher(blob)
                .find()) {
            return false;
        }
        return true;
    }
}
