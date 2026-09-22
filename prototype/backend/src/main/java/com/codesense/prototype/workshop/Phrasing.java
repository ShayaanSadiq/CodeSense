package com.codesense.prototype.workshop;

import com.codesense.prototype.model.Slot;
import com.codesense.prototype.model.WorkshopConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Phrasing {

    private Phrasing() {
    }

    public static List<String> phraseFromRules(Slot slot, String answer, String labQuestion, List<String> priorLines) {
        String a = answer == null ? "" : answer.trim();
        String lab = labQuestion == null ? "" : labQuestion;
        List<String> prior = priorLines == null ? List.of() : priorLines;
        if (slot == Slot.IO) {
            return List.of("INPUT " + ioInput(a, lab), "OUTPUT " + ioOutput(a, lab));
        }
        if (slot == Slot.STRATEGY) {
            if (find(a, "\\b(comput(e|es|ed)? once|just calculat|formula|no loop|no repeat|one step|directly)\\b")) {
                return List.of("COMPUTE the result in one step");
            }
            if (find(a, "\\b(split|divid|half|smaller parts|recursive|conquer)\\b")) {
                return List.of("SPLIT the problem into smaller parts");
            }
            if (find(a, "\\b(search|find one|until i find|look for one|stop when i find)\\b")) {
                return List.of("SEARCH until the needed item is found");
            }
            return List.of("FOR each " + itemNoun(a, lab) + " in the input");
        }
        if (slot == Slot.MEMORY) {
            String name = rememberedName(a, lab, prior);
            List<String> lines = new ArrayList<>();
            if (find(a, "\\bfirst\\b")) {
                lines.add("SET " + name + " to the first " + itemNoun(a, lab));
            } else if (find(a, "\\b(zero|0|starts at)\\b")) {
                lines.add("SET " + name + " to 0");
            } else {
                lines.add("SET " + name + " to the value so far");
            }
            if (find(a, "\\b(greater|larger|bigger|update|change)\\b")) {
                String noun = itemNoun(a, lab);
                lines.add("IF this " + noun + " is greater than " + name + ", SET " + name + " to this " + noun);
            }
            return lines;
        }
        List<String> lines = new ArrayList<>();
        if (find(a, "\\b(end|no more|last|finish|over|done|list ends|string ends)\\b")) {
            lines.add("UNTIL there are no more items");
        }
        String ret = firstMatch(List.of(a), "(?:give back|return|print)\\s+(.+?)(?:\\.|$)");
        if (ret == null && find(a, "\\bmax\\b")) {
            ret = "max";
        } else if (ret == null && find(a, "\\bcount\\b")) {
            ret = "count";
        } else if (ret == null) {
            ret = "the result";
        }
        lines.add("RETURN " + ret);
        return lines;
    }

    public static List<String> sanitizeLines(List<String> texts) {
        List<String> out = new ArrayList<>();
        if (texts == null) {
            return out;
        }
        for (String raw : texts) {
            if (raw == null) {
                continue;
            }
            String text = raw.replaceAll("\\s+", " ").trim();
            if (text.isEmpty()) {
                continue;
            }
            String verb = text.split("\\s+")[0].toUpperCase();
            if (!WorkshopConstants.ALLOWED_VERBS.contains(verb)) {
                continue;
            }
            String rest = text.substring(verb.length()).trim();
            if (rest.isEmpty()) {
                continue;
            }
            String cleaned = verb + " " + rest;
            if (extraVerb(cleaned) || tautology(cleaned)) {
                continue;
            }
            out.add(cleaned);
        }
        return out;
    }

    public static boolean usableForSlot(Slot slot, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        if (slot == Slot.IO) {
            return hasVerb(lines, "INPUT") && hasVerb(lines, "OUTPUT");
        }
        return true;
    }

    public static boolean usablePlan(List<String> lines) {
        if (lines == null || lines.size() < 3) {
            return false;
        }
        return hasVerb(lines, "INPUT") && hasVerb(lines, "OUTPUT");
    }

    private static boolean hasVerb(List<String> lines, String verb) {
        String prefix = verb + " ";
        return lines.stream().anyMatch(line -> line.startsWith(prefix));
    }

    private static boolean extraVerb(String line) {
        String[] words = line.split("\\s+");
        for (int i = 1; i < words.length; i++) {
            String word = words[i].toUpperCase();
            if ("SET".equals(word) || "THEN".equals(word)) {
                continue;
            }
            if (WorkshopConstants.ALLOWED_VERBS.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tautology(String line) {
        Matcher matcher = Pattern.compile("^SET\\s+(.+?)\\s+to\\s+\\1$", Pattern.CASE_INSENSITIVE).matcher(line);
        return matcher.matches();
    }

    private static String titleNoun(String value) {
        String cleaned = value.replaceAll("\\s+", " ").trim().replaceAll("[.,;:]+$", "");
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        return Character.toLowerCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private static List<String> sentences(String text) {
        List<String> parts = new ArrayList<>();
        for (String part : text.split("[.!?]+")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts;
    }

    private static String matchGroup(String text, String regex) {
        Matcher matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text);
        if (!matcher.find()) {
            return null;
        }
        for (int i = 1; i <= matcher.groupCount(); i++) {
            if (matcher.group(i) != null && !matcher.group(i).isBlank()) {
                return titleNoun(matcher.group(i));
            }
        }
        return null;
    }

    private static String firstMatch(List<String> texts, String regex) {
        for (String text : texts) {
            String found = matchGroup(text, regex);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean find(String text, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text).find();
    }

    private static String ioInput(String answer, String lab) {
        List<String> parts = sentences(answer);
        String fromAnswer = firstMatch(parts, "(?:comes in|is given|are given|takes|receives|input is)\\s+(.+)");
        if (fromAnswer == null) {
            fromAnswer = firstMatch(parts, "(.+?)\\s+comes in");
        }
        if (fromAnswer != null) {
            return fromAnswer;
        }
        String blob = (answer + " " + lab).toLowerCase();
        if (find(blob, "\\b(list|array)\\b")) {
            return "a list of numbers";
        }
        if (find(blob, "\\b(string|word|sentence|text)\\b")) {
            return "a string";
        }
        if (find(blob, "\\brectangle\\b") || (find(blob, "\\blength\\b") && find(blob, "\\bwidth\\b"))) {
            return "the length and the width";
        }
        if (find(blob, "\\btwo numbers\\b")) {
            return "two numbers";
        }
        if (find(blob, "\\bnumber\\b")) {
            return "a number";
        }
        return "the given value";
    }

    private static String ioOutput(String answer, String lab) {
        List<String> parts = sentences(answer);
        String fromAnswer = firstMatch(parts, "(?:comes out|must come out|should (?:print|return|give)|output is|result is)\\s+(.+)");
        if (fromAnswer == null) {
            fromAnswer = firstMatch(parts, "(.+?)\\s+comes out");
        }
        if (fromAnswer != null) {
            return fromAnswer;
        }
        String blob = (answer + " " + lab).toLowerCase();
        if (find(blob, "\\b(largest|biggest|maximum|max)\\b")) {
            return "the largest number";
        }
        if (find(blob, "\\b(smallest|minimum|min)\\b")) {
            return "the smallest number";
        }
        if (find(blob, "vowel")) {
            return "the number of vowels";
        }
        if (find(blob, "\\barea\\b")) {
            return "the area";
        }
        if (find(blob, "\\bcount\\b")) {
            return "the count";
        }
        if (find(blob, "\\bsum\\b")) {
            return "the sum";
        }
        return "the result";
    }

    private static String itemNoun(String answer, String lab) {
        String blob = (answer + " " + lab).toLowerCase();
        if (find(blob, "\\b(number|integer)\\b")) {
            return "number";
        }
        if (find(blob, "\\b(character|letter|vowel)\\b")) {
            return "character";
        }
        if (find(blob, "\\b(word)\\b")) {
            return "word";
        }
        return "item";
    }

    private static String rememberedName(String answer, String lab, List<String> priorLines) {
        String a = (answer + " " + lab).toLowerCase();
        if (find(a, "\\b(count)\\b")) {
            return "count";
        }
        if (find(a, "\\b(sum|total)\\b")) {
            return "sum";
        }
        if (find(a, "\\b(min|smallest)\\b")) {
            return "min";
        }
        if (find(a, "\\b(max|biggest|largest|greatest)\\b")) {
            return "max";
        }
        String named = matchGroup(answer, "remember\\s+(?:the\\s+)?(.+)");
        if (named != null) {
            return named;
        }
        for (int i = priorLines.size() - 1; i >= 0; i--) {
            Matcher set = Pattern.compile("^SET\\s+(\\S+)", Pattern.CASE_INSENSITIVE).matcher(priorLines.get(i));
            if (set.find() && !"the".equals(set.group(1))) {
                return set.group(1);
            }
        }
        return "the value so far";
    }
}
