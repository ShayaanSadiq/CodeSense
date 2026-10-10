package com.codesense.prototype.workshop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class Translator {

    public record Mark(String word, String construct) {
    }

    public record GlossaryEntry(String word, String construct, String hint) {
    }

    private static final List<GlossaryEntry> GLOSSARY = List.of(
            new GlossaryEntry("list", "INPUT", "Incoming collection"),
            new GlossaryEntry("array", "INPUT", "Incoming collection"),
            new GlossaryEntry("string", "INPUT", "Incoming text"),
            new GlossaryEntry("length", "INPUT", "A measurement that comes in"),
            new GlossaryEntry("width", "INPUT", "A measurement that comes in"),
            new GlossaryEntry("each", "FOR", "Walk every item"),
            new GlossaryEntry("every", "FOR", "Walk every item"),
            new GlossaryEntry("once", "COMPUTE", "No loop"),
            new GlossaryEntry("multiply", "COMPUTE", "One calculation"),
            new GlossaryEntry("remember", "SET", "A value kept between steps"),
            new GlossaryEntry("largest", "OUTPUT", "The result that comes out"),
            new GlossaryEntry("smallest", "OUTPUT", "The result that comes out"),
            new GlossaryEntry("area", "OUTPUT", "The result that comes out"),
            new GlossaryEntry("vowels", "OUTPUT", "The result that comes out"),
            new GlossaryEntry("if", "IF", "A compare-and-update"),
            new GlossaryEntry("until", "UNTIL", "When the walk ends"),
            new GlossaryEntry("return", "RETURN", "Give the remembered value back"));

    private Translator() {
    }

    public static List<GlossaryEntry> glossary() {
        return GLOSSARY;
    }

    public static List<String> assemble(String labQuestion, List<Mark> marks) {
        Map<String, String> chosen = new LinkedHashMap<>();
        if (marks != null) {
            for (Mark mark : marks) {
                if (mark == null || mark.construct() == null || mark.construct().isBlank()) {
                    continue;
                }
                String verb = mark.construct().trim().toUpperCase(Locale.ROOT);
                if (!chosen.containsKey(verb)) {
                    chosen.put(verb, mark.word() == null ? "" : mark.word());
                }
            }
        }
        String lab = labQuestion == null ? "" : labQuestion;
        List<String> lines = new ArrayList<>();
        lines.add("INPUT " + inputNoun(lab, chosen.get("INPUT")));
        if (chosen.containsKey("SET") || needsMemory(lab)) {
            lines.add("SET " + remembered(lab) + " to the first " + itemNoun(lab));
        }
        if (chosen.containsKey("COMPUTE") || isOnce(lab)) {
            lines.add("COMPUTE the result in one step");
        } else {
            lines.add("FOR each " + itemNoun(lab) + " in the list");
        }
        if (chosen.containsKey("IF") || needsMemory(lab)) {
            lines.add(compareLine(lab));
        }
        if (chosen.containsKey("UNTIL") || needsMemory(lab)) {
            lines.add("UNTIL there are no more items");
        }
        if (chosen.containsKey("RETURN") || needsMemory(lab)) {
            lines.add("RETURN " + remembered(lab));
        }
        lines.add("OUTPUT " + outputNoun(lab, chosen.get("OUTPUT")));
        List<String> cleaned = Phrasing.sanitizeLines(lines);
        cleaned.sort(Comparator.comparingInt(line -> rank(line.split("\\s+")[0])));
        return cleaned;
    }

    private static boolean needsMemory(String lab) {
        return find(lab, "\\b(largest|smallest|vowel|count|sum)\\b") && !isOnce(lab);
    }

    private static boolean isOnce(String lab) {
        return find(lab, "\\b(area|rectangle|multiply|once)\\b");
    }

    private static String inputNoun(String lab, String marked) {
        if (marked != null && !marked.isBlank() && !"list".equalsIgnoreCase(marked)) {
            if (find(marked, "string|text|word")) {
                return "a string";
            }
            if (find(marked, "length|width")) {
                return "the length and the width";
            }
        }
        if (find(lab, "\\b(list|array)\\b")) {
            return "a list of numbers";
        }
        if (find(lab, "\\b(string|word|text)\\b")) {
            return "a string";
        }
        if (find(lab, "\\brectangle\\b") || (find(lab, "\\blength\\b") && find(lab, "\\bwidth\\b"))) {
            return "the length and the width";
        }
        return "the given value";
    }

    private static String outputNoun(String lab, String marked) {
        if (find(lab + " " + (marked == null ? "" : marked), "\\b(largest|biggest|max)\\b")) {
            return "the largest number";
        }
        if (find(lab + " " + (marked == null ? "" : marked), "\\b(smallest|min)\\b")) {
            return "the smallest number";
        }
        if (find(lab + " " + (marked == null ? "" : marked), "vowel")) {
            return "the number of vowels";
        }
        if (find(lab + " " + (marked == null ? "" : marked), "\\barea\\b")) {
            return "the area";
        }
        return "the result";
    }

    private static String remembered(String lab) {
        if (find(lab, "\\b(largest|biggest|max)\\b")) {
            return "max";
        }
        if (find(lab, "\\b(smallest|min)\\b")) {
            return "min";
        }
        if (find(lab, "vowel|count")) {
            return "count";
        }
        return "the value so far";
    }

    private static String itemNoun(String lab) {
        if (find(lab, "\\b(number|list|array)\\b")) {
            return "number";
        }
        if (find(lab, "vowel|string|character")) {
            return "character";
        }
        return "item";
    }

    private static String compareLine(String lab) {
        String name = remembered(lab);
        String noun = itemNoun(lab);
        if (find(lab, "\\b(largest|biggest|max)\\b")) {
            return "IF this " + noun + " is greater than " + name + ", SET " + name + " to this " + noun;
        }
        if (find(lab, "\\b(smallest|min)\\b")) {
            return "IF this " + noun + " is smaller than " + name + ", SET " + name + " to this " + noun;
        }
        if (find(lab, "vowel")) {
            return "IF this " + noun + " is a vowel, SET " + name + " to " + name + " plus 1";
        }
        return "IF this " + noun + " should update " + name + ", SET " + name + " to this " + noun;
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

    private static boolean find(String text, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text).find();
    }
}
