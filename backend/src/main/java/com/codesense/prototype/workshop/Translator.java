package com.codesense.prototype.workshop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class Translator {

    public record Mark(String word, String construct) {
    }

    public record GlossaryEntry(String word, String construct, String hint, List<String> aliases) {
        public GlossaryEntry(String word, String construct, String hint) {
            this(word, construct, hint, List.of());
        }
    }

    public record Role(String construct, String title, String why) {
    }

    public record VoiceLine(String text, String card) {
    }

    public record Voice(String text, List<String> needed, Map<String, String> unused, List<VoiceLine> lines) {
        public Voice(String text, List<String> needed) {
            this(text, needed, Map.of(), List.of());
        }

        public Voice(String text, List<String> needed, Map<String, String> unused) {
            this(text, needed, unused, List.of());
        }
    }

    public record CardJudge(boolean correct, String explanation) {
    }

    private static final List<GlossaryEntry> GLOSSARY = List.of(
            new GlossaryEntry(
                    "what you are given",
                    "INPUT",
                    "The numbers or text you start with.",
                    List.of(
                            "you are given some numbers",
                            "you are given some text",
                            "you are given two measurements",
                            "you are given something")),
            new GlossaryEntry(
                    "remember the first one",
                    "SET",
                    "Save one number now so you can compare later ones to it.",
                    List.of("remember the first number", "remember a count of zero")),
            new GlossaryEntry(
                    "look at each one",
                    "FOR",
                    "Go through them one by one.",
                    List.of("look at each number", "look at each letter")),
            new GlossaryEntry(
                    "do it in one step",
                    "COMPUTE",
                    "You already have everything. Just calculate once.",
                    List.of("do it in one step")),
            new GlossaryEntry(
                    "only if this one is bigger",
                    "IF",
                    "Change your answer only when a new number is bigger.",
                    List.of(
                            "keep it only if this one is bigger",
                            "keep it only if this one is smaller",
                            "add one only if the letter is a vowel")),
            new GlossaryEntry(
                    "stop at the last one",
                    "UNTIL",
                    "You are done when there are no more numbers.",
                    List.of("stop at the last number", "stop at the last letter")),
            new GlossaryEntry(
                    "show the answer",
                    "OUTPUT",
                    "Last step. Give back the result you kept.",
                    List.of("show the answer", "use the number you remembered")));

    private static final List<String> CARD_ECHO = List.of(
            "you are given",
            "what you are given",
            "remember the first",
            "remember a count",
            "look at each",
            "do it in one step",
            "keep it only if",
            "only if this one",
            "stop at the last",
            "show the answer");

    private static final List<Role> ROLES = List.of(
            new Role(
                    "INPUT",
                    "Start with what you are given",
                    "You cannot do any work until you know the numbers or text. That is why this is always first."),
            new Role(
                    "SET",
                    "Remember the first one",
                    "Save the first number now. Later numbers will be compared to it, so this comes right after what you are given."),
            new Role(
                    "FOR",
                    "Look at each one",
                    "Now go through every number, one by one. This comes after you know the list and have remembered the first number."),
            new Role(
                    "COMPUTE",
                    "Do it in one step",
                    "You do not need to go through a list. Just calculate once from the values you already have."),
            new Role(
                    "IF",
                    "Check each number",
                    "This check happens for every number, not after you finish. Put it under the step that looks at each one."),
            new Role(
                    "UNTIL",
                    "Stop at the last one",
                    "You stop when there are no more numbers. This comes after the check."),
            new Role(
                    "OUTPUT",
                    "Show the answer",
                    "Last step. Give back the result you kept. The person only sees it after all the work. This is always last."));

    private Translator() {
    }

    public static List<GlossaryEntry> glossary() {
        return GLOSSARY;
    }

    public static List<Role> roles() {
        return ROLES;
    }

    public static String why(String construct) {
        String verb = construct == null ? "" : construct.trim().toUpperCase(Locale.ROOT);
        if ("RETURN".equals(verb)) {
            verb = "OUTPUT";
        }
        for (Role role : ROLES) {
            if (role.construct().equals(verb)) {
                return role.why();
            }
        }
        return "Put this step where that work actually happens.";
    }

    public static String rephrase(String labQuestion) {
        return joinVoice(ruleLines(labQuestion));
    }

    public static Voice ruleVoice(String labQuestion) {
        List<VoiceLine> lines = ruleLines(labQuestion);
        List<String> needed = sanitizeNeeded(lines.stream().map(VoiceLine::card).toList());
        List<VoiceLine> cleaned = sanitizeVoiceLines(labQuestion, needed, lines);
        return new Voice(
                joinVoice(cleaned),
                needed,
                unusedReasons(labQuestion, needed, Map.of()),
                cleaned);
    }

    public static List<VoiceLine> ruleLines(String labQuestion) {
        String lab = labQuestion == null ? "" : labQuestion;
        List<VoiceLine> lines = new ArrayList<>();
        if (find(lab, "\\b(area|rectangle)\\b")) {
            lines.add(new VoiceLine("Length and width are already in your hands.", "INPUT"));
            lines.add(new VoiceLine("Work out the area from those two measurements.", "COMPUTE"));
            lines.add(new VoiceLine("Tell the person the area you got.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "vowel")) {
            lines.add(new VoiceLine("A word sits at the start.", "INPUT"));
            lines.add(new VoiceLine("Start a tally at zero.", "SET"));
            lines.add(new VoiceLine("Visit every letter in the word.", "FOR"));
            lines.add(new VoiceLine("Bump the tally when the letter is a vowel.", "IF"));
            lines.add(new VoiceLine("Finish after the final letter.", "UNTIL"));
            lines.add(new VoiceLine("Give back how many vowels you found.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "\\b(smallest|min)\\b")) {
            lines.add(new VoiceLine("A list of numbers is waiting at the start.", "INPUT"));
            lines.add(new VoiceLine("Hold onto the opening value so later ones can be compared.", "SET"));
            lines.add(new VoiceLine("Go through the list one value at a time.", "FOR"));
            lines.add(new VoiceLine("Replace what you kept when a new value is smaller.", "IF"));
            lines.add(new VoiceLine("Finish after the final value.", "UNTIL"));
            lines.add(new VoiceLine("Give back the result you kept.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "\\b(largest|biggest|max)\\b")) {
            lines.add(new VoiceLine("A list of numbers is waiting at the start.", "INPUT"));
            lines.add(new VoiceLine("Hold onto the opening value so later ones can be compared.", "SET"));
            lines.add(new VoiceLine("Go through the list one value at a time.", "FOR"));
            lines.add(new VoiceLine("Replace what you kept when a new value is greater.", "IF"));
            lines.add(new VoiceLine("Finish after the final value.", "UNTIL"));
            lines.add(new VoiceLine("Give back the result you kept.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "\\b(even|odd)\\b")) {
            boolean odd = find(lab, "\\bodd\\b");
            lines.add(new VoiceLine("A list of numbers is waiting at the start.", "INPUT"));
            lines.add(new VoiceLine("Start a tally at zero.", "SET"));
            lines.add(new VoiceLine("Visit every number in the list.", "FOR"));
            lines.add(new VoiceLine(
                    odd
                            ? "Bump the tally when the number is odd."
                            : "Bump the tally when the number is even.",
                    "IF"));
            lines.add(new VoiceLine("Finish after the final number.", "UNTIL"));
            lines.add(new VoiceLine("Give back how many you found.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "\\b(count|sum)\\b")) {
            lines.add(new VoiceLine("A list of numbers is waiting at the start.", "INPUT"));
            lines.add(new VoiceLine("Start a tally at zero.", "SET"));
            lines.add(new VoiceLine("Visit every number in the list.", "FOR"));
            lines.add(new VoiceLine("Finish after the final number.", "UNTIL"));
            lines.add(new VoiceLine("Give back the total you kept.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "\\b(multiply|product|times)\\b")) {
            lines.add(new VoiceLine("Two numbers are already in your hands.", "INPUT"));
            lines.add(new VoiceLine("Work out the product from those two values.", "COMPUTE"));
            lines.add(new VoiceLine("Tell the person the product you got.", "OUTPUT"));
            return lines;
        }
        if (find(lab, "\\b(list|array|each|every|numbers|letters)\\b")) {
            lines.add(new VoiceLine("Some items are waiting at the start.", "INPUT"));
            lines.add(new VoiceLine("Hold a starting value before you move on.", "SET"));
            lines.add(new VoiceLine("Go through the items one by one.", "FOR"));
            lines.add(new VoiceLine("Finish after the final item.", "UNTIL"));
            lines.add(new VoiceLine("Give back the result you kept.", "OUTPUT"));
            return lines;
        }
        lines.add(new VoiceLine("Something is waiting at the start.", "INPUT"));
        lines.add(new VoiceLine("Give back the result.", "OUTPUT"));
        return lines;
    }

    public static String joinVoice(List<VoiceLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (VoiceLine line : lines) {
            if (line == null || line.text() == null || line.text().isBlank()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(endSentence(line.text().trim()));
        }
        return out.toString();
    }

    public static List<VoiceLine> sanitizeVoiceLines(
            String labQuestion, List<String> needed, List<VoiceLine> hinted) {
        Map<String, String> byCard = new LinkedHashMap<>();
        if (hinted != null) {
            for (VoiceLine line : hinted) {
                if (line == null || line.card() == null || line.text() == null) {
                    continue;
                }
                String verb = line.card().trim().toUpperCase(Locale.ROOT);
                if ("RETURN".equals(verb)) {
                    verb = "OUTPUT";
                }
                if (needed == null || !needed.contains(verb)) {
                    continue;
                }
                String text = endSentence(line.text().trim());
                if (text.length() < 8 || copiesCardPhrase(text)) {
                    continue;
                }
                byCard.putIfAbsent(verb, text);
            }
        }
        Map<String, String> rules = new LinkedHashMap<>();
        for (VoiceLine line : ruleLines(labQuestion)) {
            rules.put(line.card(), endSentence(line.text().trim()));
        }
        List<VoiceLine> out = new ArrayList<>();
        if (needed != null) {
            for (String verb : needed) {
                String text = byCard.get(verb);
                if (text == null || text.isBlank()) {
                    text = rules.get(verb);
                }
                if (text != null && !text.isBlank()) {
                    out.add(new VoiceLine(text, verb));
                }
            }
        }
        return out;
    }

    public static boolean copiesCardPhrase(String sentence) {
        String lower = sentence == null ? "" : sentence.toLowerCase(Locale.ROOT);
        for (String echo : CARD_ECHO) {
            if (lower.contains(echo)) {
                return true;
            }
        }
        for (GlossaryEntry entry : GLOSSARY) {
            if (containsPhrase(lower, entry.word())) {
                return true;
            }
            for (String alias : entry.aliases()) {
                if (containsPhrase(lower, alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<String> sanitizeNeeded(List<String> raw) {
        Set<String> allowed = Set.of("INPUT", "SET", "FOR", "COMPUTE", "IF", "UNTIL", "OUTPUT");
        Set<String> needed = new LinkedHashSet<>();
        if (raw != null) {
            for (String item : raw) {
                if (item == null) {
                    continue;
                }
                String verb = item.trim().toUpperCase(Locale.ROOT);
                if ("RETURN".equals(verb)) {
                    verb = "OUTPUT";
                }
                if (allowed.contains(verb)) {
                    needed.add(verb);
                }
            }
        }
        needed.add("INPUT");
        needed.add("OUTPUT");
        if (needed.contains("FOR") && needed.contains("COMPUTE")) {
            needed.remove("COMPUTE");
        }
        return List.copyOf(needed);
    }

    public static List<String> neededConstructs(String labQuestion) {
        return sanitizeNeeded(ruleLines(labQuestion).stream().map(VoiceLine::card).toList());
    }

    public static Map<String, String> unusedReasons(
            String labQuestion, List<String> needed, Map<String, String> hinted) {
        Set<String> need = new LinkedHashSet<>();
        if (needed != null) {
            need.addAll(needed);
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (GlossaryEntry entry : GLOSSARY) {
            String verb = entry.construct();
            if (need.contains(verb)) {
                continue;
            }
            String why = hinted != null ? hinted.get(verb) : null;
            if (why == null || why.isBlank()) {
                why = unusedReason(verb, labQuestion);
            }
            if (why != null && !why.isBlank()) {
                out.put(verb, why.trim());
            }
        }
        return out;
    }

    public static String unusedReason(String construct, String labQuestion) {
        String verb = construct == null ? "" : construct.trim().toUpperCase(Locale.ROOT);
        String lab = shortLab(labQuestion);
        if ("RETURN".equals(verb)) {
            return "You do not need a separate return. Show the answer is already the last step.";
        }
        if (neededConstructs(labQuestion).contains(verb)) {
            return null;
        }
        boolean scans = scansItems(labQuestion);
        if ("COMPUTE".equals(verb)) {
            return scans
                    ? "You would not use this card for “" + lab + "”. You look at each item, so you do not work it out in one step."
                    : "You would not use this card for “" + lab + "”.";
        }
        if ("SET".equals(verb) || "FOR".equals(verb) || "IF".equals(verb) || "UNTIL".equals(verb)) {
            return scans
                    ? "You would not use this card for “" + lab + "”. This step is not part of this plan."
                    : "You would not use this card for “" + lab + "”. There is no list to look through, so you just calculate once.";
        }
        return "This card does not match “" + lab + "”, so you do not need it for this plan.";
    }

    public static boolean scansItems(String labQuestion) {
        return find(labQuestion, "\\b(list|array|each|every|numbers|letters|vowel|even|odd|count|sum|largest|smallest|biggest)\\b")
                && !isOnce(labQuestion);
    }

    public static String shortLab(String labQuestion) {
        String lab = labQuestion == null ? "" : labQuestion.trim();
        if (lab.isEmpty()) {
            return "this question";
        }
        return lab.length() > 90 ? lab.substring(0, 87) + "..." : lab;
    }

    public static String markProblem(String labQuestion, List<Mark> marks) {
        Set<String> needed = new LinkedHashSet<>(neededConstructs(labQuestion));
        Set<String> got = new LinkedHashSet<>();
        if (marks != null) {
            for (Mark mark : marks) {
                if (mark == null || mark.construct() == null || mark.construct().isBlank()) {
                    continue;
                }
                String verb = mark.construct().trim().toUpperCase(Locale.ROOT);
                if ("RETURN".equals(verb)) {
                    verb = "OUTPUT";
                }
                if (!needed.contains(verb)) {
                    return unusedReason(verb, labQuestion);
                }
                got.add(verb);
            }
        }
        if (!got.containsAll(needed)) {
            return "Match every sentence in the question to a card before you build the path.";
        }
        return null;
    }

    public static List<String> assemble(String labQuestion, List<Mark> marks) {
        Map<String, String> chosen = new LinkedHashMap<>();
        if (marks != null) {
            for (Mark mark : marks) {
                if (mark == null || mark.construct() == null || mark.construct().isBlank()) {
                    continue;
                }
                String verb = mark.construct().trim().toUpperCase(Locale.ROOT);
                if ("RETURN".equals(verb)) {
                    verb = "OUTPUT";
                }
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
        if (find(lab, "\\bsquare\\b") || (find(lab, "\\barea\\b") && find(lab, "\\bside\\b"))) {
            return "the side";
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

    private static String endSentence(String text) {
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
            return text;
        }
        return text + ".";
    }

    private static boolean matchesQuestion(String voiced, GlossaryEntry entry) {
        if (containsPhrase(voiced, entry.word())) {
            return true;
        }
        for (String alias : entry.aliases()) {
            if (containsPhrase(voiced, alias)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPhrase(String voiced, String phrase) {
        return phrase != null && !phrase.isBlank() && voiced.contains(phrase.toLowerCase(Locale.ROOT));
    }

    private static boolean find(String text, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text).find();
    }
}
