package com.codesense.prototype.workshop;

import com.codesense.prototype.model.LabBrief;
import com.codesense.prototype.model.McqOption;
import com.codesense.prototype.model.Slot;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Socratic {

    public record TurnJudge(boolean correct, boolean skipAhead, int nextStage, String expected, String explanation, String nextQuestion) {
    }

    public record Help(int level, String hint, String question, String followUp, String expected) {
    }

    public record Ask(String kind, String question, List<McqOption> choices, String correct, String whyQuestion) {
        public boolean isMcq() {
            return "mcq".equals(kind) && choices != null && choices.size() >= 2;
        }
    }

    private static final Pattern MANUAL = Pattern.compile(
            "\\b(by hand|look through|go through|scan|keep|biggest|smallest|compare|count|multiply|pick)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern WHY_REASON = Pattern.compile(
            "\\b(bigger|larger|greater|smaller|less|worse|better|beat|keep|leave|replace|than|"
                    + "negative|zero|first|start|starts|safer|invent|vowel|letter|count|multiply|"
                    + "area|every|once)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HAS_NUMBER = Pattern.compile("-?\\d+");
    private static final Pattern PLAN = Pattern.compile(
            "\\b(input|set|for|loop|if|until|output|start|check|return|init)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACE = Pattern.compile(
            "-?\\d+|after|stored|starts|then|stays|becomes",
            Pattern.CASE_INSENSITIVE);

    private Socratic() {
    }

    public static LabBrief briefFromRules(String labQuestion) {
        String lab = labQuestion == null ? "" : labQuestion.toLowerCase(Locale.ROOT);
        LabBrief brief = new LabBrief();
        if (lab.contains("largest") || lab.contains("biggest") || lab.contains("max")) {
            brief.setInput("An array of integers, such as [3, 8, 2, 10, 5].");
            brief.setOutput("10");
            brief.setConstraintItems(List.of(
                    "The array contains at least one element.",
                    "Array elements can be positive, negative, or zero.",
                    "The array may contain duplicate elements."));
            brief.setSample("[3, 8, 2, 10, 5]");
            brief.setPassedTests(List.of(
                    "[3, 8, 2, 10, 5] → 10",
                    "[-5, -2, -9] → -2",
                    "[7] → 7",
                    "[4, 4, 4] → 4"));
            brief.finish();
            return brief;
        }
        if (lab.contains("smallest") || lab.contains("min")) {
            brief.setInput("An array of integers, such as [3, 8, 2, 10, 5].");
            brief.setOutput("2");
            brief.setConstraintItems(List.of(
                    "The array contains at least one element.",
                    "Array elements can be positive, negative, or zero.",
                    "The array may contain duplicate elements."));
            brief.setSample("[3, 8, 2, 10, 5]");
            brief.setPassedTests(List.of(
                    "[3, 8, 2, 10, 5] → 2",
                    "[-5, -2, -9] → -9",
                    "[7] → 7",
                    "[4, 4, 4] → 4"));
            brief.finish();
            return brief;
        }
        if (lab.contains("vowel")) {
            brief.setInput("A string of letters, such as hello.");
            brief.setOutput("2");
            brief.setConstraintItems(List.of(
                    "Count a, e, i, o, and u.",
                    "Uppercase and lowercase both count.",
                    "The string may be empty."));
            brief.setSample("hello");
            brief.setPassedTests(List.of("hello → 2", "sky → 0", "AEIOU → 5"));
            brief.finish();
            return brief;
        }
        if (lab.contains("square") || (lab.contains("area") && lab.contains("side"))) {
            brief.setInput("The side length of a square, such as 5.");
            brief.setOutput("25");
            brief.setConstraintItems(List.of(
                    "There is one side length, not a length and a width.",
                    "The side is a positive number."));
            brief.setSample("5");
            brief.setPassedTests(List.of("5 → 25", "3 → 9", "1 → 1"));
            brief.finish();
            return brief;
        }
        if (lab.contains("area") || lab.contains("rectangle")) {
            brief.setInput("The length and the width, such as 4 and 6.");
            brief.setOutput("24");
            brief.setConstraintItems(List.of(
                    "Length and width are positive numbers.",
                    "The result is length times width."));
            brief.setSample("4 and 6");
            brief.setPassedTests(List.of("4 and 6 → 24", "3 and 5 → 15", "2 and 2 → 4"));
            brief.finish();
            return brief;
        }
        brief.setInput("The values given in the question.");
        brief.setOutput("the result");
        brief.setConstraintItems(List.of("One procedure.", "One result."));
        brief.setSample("a small example from the question");
        brief.setPassedTests(List.of("A typical example → the asked result"));
        brief.finish();
        return brief;
    }

    public static String fallbackQuestion(int stage, LabBrief brief, String labQuestion) {
        String sample = cleanSample(brief != null ? brief.getSample() : null);
        LabKind kind = labKind(labQuestion);
        return switch (clampStage(stage)) {
            case 1 -> kind == LabKind.COUNT
                    ? "Suppose the input is " + sample + ". How would you count the vowels if you were doing it by hand?"
                    : "Suppose the input is " + sample + ". How would you identify the answer if you were solving it by hand?";
            case 2 -> switch (kind) {
                case ONCE -> "Do you need to look through leftover items, or can you get the answer with one calculation from the values you already have? Why?";
                case COUNT -> "Should you compare every letter with every other letter, or can you count by looking through the word just once? Why?";
                default -> "Should you compare every item with every other item, or can you find the answer by looking through just once? Why?";
            };
            case 3 -> kind == LabKind.COUNT
                    ? "Before you look at any letters, what should the count start at? Why start there?"
                    : "Before you start comparing, what value should you store first? Should it always be zero?";
            case 4 -> kind == LabKind.COUNT
                    ? "If this letter is a vowel, what happens to the count? What if it is not a vowel?"
                    : "If the current item is better than the value you stored, what should happen? What if it is not?";
            case 5 -> kind == LabKind.COUNT
                    ? "You have looked at some letters but not all. Are you finished counting, or do you still need to look at the rest?"
                    : "You have looked at some items but not all. Are you finished, or do you still need to look at the rest?";
            case 6 -> switch (kind) {
                case ONCE -> "If you were finding the area by hand, what two measurements would you be given first, and what would you write down at the end?";
                case COUNT -> "If you were counting vowels by hand, what would you be given first, and what would you write down at the end?";
                default -> "If you were doing this by hand, what would you be given first, and what would you write down at the end?";
            };
            default -> switch (kind) {
                case ONCE -> computeOnceTrace(brief);
                case COUNT -> countWalkQuestion(brief);
                default -> byHandHoldQuestion(brief);
            };
        };
    }

    public static List<String> narrowLadder(int stage, LabBrief brief) {
        return narrowLadder(stage, brief, null);
    }

    public static List<String> narrowLadder(int stage, LabBrief brief, String labQuestion) {
        String sample = cleanSample(brief != null ? brief.getSample() : null);
        LabKind kind = labQuestion != null && !labQuestion.isBlank()
                ? labKind(labQuestion)
                : (computeOnceBrief(brief) ? LabKind.ONCE : LabKind.BEST);
        if (kind == LabKind.COUNT || looksLikeCountBrief(brief)) {
            kind = LabKind.COUNT;
        }
        final LabKind k = kind;
        return switch (clampStage(stage)) {
            case 1 -> List.of(
                    k == LabKind.COUNT
                            ? "Look at " + sample + ". Which letters would you count, and why those?"
                            : "Look at " + sample + ". Which value would you pick, and why?",
                    k == LabKind.ONCE
                            ? "What two numbers do you already have, and what do you do with them?"
                            : k == LabKind.COUNT
                                    ? "If you only looked at the first letter, what would you miss?"
                                    : "If you could keep only one value while you look, which one would you keep?");
            case 2 -> List.of(
                    k == LabKind.ONCE
                            ? "Name the cheaper way: one calculation from the values you have, or a leftover list to scan."
                            : k == LabKind.COUNT
                                    ? "Name the cheaper way: check every pair of letters, or go through the word once."
                                    : "Name the cheaper way: check every pair, or go through the input once.",
                    k == LabKind.ONCE
                            ? "Do you need a loop, or is one multiply enough?"
                            : "Do you need a nested comparison, or is one pass enough?");
            case 3 -> k == LabKind.COUNT
                    ? List.of(
                            "What should the count be before you look at any letter? Why not start at 1?",
                            "Why does a count start at zero instead of at the first letter?")
                    : List.of(
                            "What should the stored value be before you look at the rest? Why not always zero?",
                            "Which item is a safe first stored value?");
            case 4 -> k == LabKind.COUNT
                    ? List.of(
                            "When this letter is a vowel, what do you change?",
                            "When this letter is not a vowel, what stays the same?")
                    : List.of(
                            "When this item beats the stored value, what do you change?",
                            "When this item does not beat the stored value, what stays the same?");
            case 5 -> List.of(
                    k == LabKind.COUNT
                            ? "After the first letter, what do you do with the leftover letters?"
                            : "After the first check, what do you do with the leftover items?",
                    "When are there no items left to check?");
            case 6 -> List.of(
                    k == LabKind.ONCE
                            ? "If you were finding the area by hand, what two measurements would you be given first, and what would you write down at the end?"
                            : k == LabKind.COUNT
                                    ? "If you were counting vowels by hand, what would you be given first, and what would you write down at the end?"
                                    : "If you were doing this by hand, what would you be given first, and what would you write down at the end?",
                    "Before you can even start, what has to be in front of you? After you finish, what do you show someone?");
            default -> k == LabKind.ONCE
                    ? List.of(
                            computeOnceTrace(brief),
                            "If the two measurements are " + sample + ", what number comes out?")
                    : k == LabKind.COUNT
                            ? List.of(
                                    countWalkQuestion(brief),
                                    "After the last letter of " + sample + ", what is the count?")
                            : List.of(
                                    "After the first three numbers, what number are you holding in your head?",
                                    "After the last number, what number are you holding?");
        };
    }

    static boolean looksLikeCountBrief(LabBrief brief) {
        if (brief == null) {
            return false;
        }
        String blob = (String.valueOf(brief.getInput()) + " " + String.valueOf(brief.getSample()) + " "
                + String.valueOf(brief.getOutput())).toLowerCase(Locale.ROOT);
        return blob.contains("vowel") || blob.contains("letter") || blob.contains("hello");
    }

    private static String cleanSample(String raw) {
        if (raw == null || raw.isBlank()) {
            return "a small example";
        }
        String sample = raw.trim();
        int arrow = sample.indexOf('→');
        if (arrow < 0) {
            arrow = sample.indexOf("->");
        }
        if (arrow > 0) {
            sample = sample.substring(0, arrow).trim();
        }
        return sample.isBlank() ? "a small example" : sample;
    }

    public static Slot slotForStage(int stage) {
        return switch (clampStage(stage)) {
            case 1 -> Slot.INPUT;
            case 2 -> Slot.STRATEGY;
            case 3, 4 -> Slot.MEMORY;
            case 5 -> Slot.STOP;
            default -> Slot.OUTPUT;
        };
    }

    public static int clampStage(int stage) {
        return Math.max(1, Math.min(7, stage));
    }

    public enum LabKind {
        BEST, COUNT, ONCE
    }

    public static boolean computeOnceLab(String labQuestion) {
        String lab = labQuestion == null ? "" : labQuestion.toLowerCase(Locale.ROOT);
        return lab.contains("area") || lab.contains("rectangle") || lab.contains("square")
                || lab.contains("multiply") || lab.contains("product");
    }

    public static LabKind labKind(String labQuestion) {
        if (computeOnceLab(labQuestion)) {
            return LabKind.ONCE;
        }
        String lab = labQuestion == null ? "" : labQuestion.toLowerCase(Locale.ROOT);
        if (lab.contains("vowel") || lab.contains("even")
                || (lab.contains("count") && !lab.contains("largest") && !lab.contains("biggest"))) {
            return LabKind.COUNT;
        }
        return LabKind.BEST;
    }

    static boolean computeOnceBrief(LabBrief brief) {
        if (brief == null) {
            return false;
        }
        String blob = (String.valueOf(brief.getInput()) + " " + String.valueOf(brief.getSample()) + " "
                + String.valueOf(brief.getOutput())).toLowerCase(Locale.ROOT);
        return blob.contains("length") || blob.contains("width") || blob.contains("area")
                || blob.contains("side") || blob.contains("multiply");
    }

    static String computeOnceTrace(LabBrief brief) {
        int[] pair = samplePair(brief);
        return "The two values are " + pair[0] + " and " + pair[1]
                + ". What number do you get when you multiply them?";
    }

    public static TurnJudge judgeFromRules(int stage, String answer, String labQuestion, LabBrief brief) {
        Slot slot = slotForStage(stage);
        Grader.Grade grade = Grader.fromRules(slot, answer, labQuestion);
        boolean correct = grade.correct();
        String text = answer == null ? "" : answer;
        if (stage == 1 && MANUAL.matcher(text).find()) {
            correct = true;
        }
        if (stage == 6 && PLAN.matcher(text).find()) {
            correct = true;
        }
        if (stage == 7 && TRACE.matcher(text).find()) {
            correct = true;
        }
        int next = correct ? nextStageAfter(stage, labQuestion) : stage;
        boolean skip = correct && next > stage + 1;
        String expected = grade.expected();
        String explanation = grade.explanation();
        int ask = next > 7 ? 7 : next;
        String question = fallbackQuestion(ask, brief, labQuestion);
        return new TurnJudge(correct, skip, next, expected, explanation, question);
    }

    /**
     * Next remaining understanding stage. Compute-once labs skip MEMORY/STOP (3-5)
     * but still visit name-the-parts (6) and trace (7) before Parsons (8).
     */
    public static int nextStageAfter(int current, String labQuestion) {
        int stage = clampStage(current);
        if (stage >= 7) {
            return 8;
        }
        int next = stage + 1;
        if (computeOnceLab(labQuestion) && next >= 3 && next <= 5) {
            return 6;
        }
        return next;
    }

    /**
     * Cap grader/LLM skipAhead so a thorough early answer cannot finish the interview.
     * The only allowed jump is compute-once MEMORY/STOP (already in {@link #nextStageAfter}).
     */
    public static int capAdvancement(int current, int proposed, boolean skipAhead, String labQuestion) {
        int required = nextStageAfter(current, labQuestion);
        if (proposed < 1) {
            return required;
        }
        if (proposed > 7 && current < 7) {
            return required;
        }
        if (proposed > required) {
            if (skipAhead && computeOnceLab(labQuestion) && current == 2) {
                return 6;
            }
            return required;
        }
        if (proposed < required) {
            return required;
        }
        return proposed;
    }

    public static int clampHelp(int level) {
        return Math.max(1, Math.min(4, level));
    }

    public static boolean preferMcq(int stage, int mcqAsked, int asksMade) {
        int s = clampStage(stage);
        if (asksMade <= 0 || s == 1) {
            return true;
        }
        if (s >= 6) {
            return false;
        }
        // Stage 1 always MCQ; 3 and 5 are decision checks. With a text why after
        // each MCQ that is about 30-40% multiple choice, the rest descriptive.
        if (s == 3 || s == 5) {
            return true;
        }
        return false;
    }

    public static Ask fallbackAsk(int stage, LabBrief brief, String labQuestion, boolean preferMcq) {
        if (preferMcq || clampStage(stage) == 1) {
            return fallbackMcq(stage, brief, labQuestion);
        }
        return new Ask("text", fallbackQuestion(stage, brief, labQuestion), List.of(), "", defaultWhy());
    }

    public static boolean mcqLooksRight(String answer, String key, List<McqOption> choices) {
        String a = answer == null ? "" : answer.trim();
        if (a.isBlank() || key == null || key.isBlank()) {
            return false;
        }
        String letter = key.trim().toUpperCase(Locale.ROOT);
        if (a.equalsIgnoreCase(letter)) {
            return true;
        }
        if (a.length() >= 2) {
            char next = a.charAt(1);
            if (a.substring(0, 1).equalsIgnoreCase(letter)
                    && (next == '.' || next == ')' || next == ':' || next == ' ')) {
                return true;
            }
        }
        McqOption match = findChoice(choices, letter);
        if (match == null) {
            return false;
        }
        String option = match.getText().toLowerCase(Locale.ROOT);
        String given = a.toLowerCase(Locale.ROOT);
        return given.contains(option) || option.contains(given) && given.length() > 12;
    }

    public static boolean whyLooksThin(String answer) {
        String a = answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
        if (a.isBlank() || a.length() < 8) {
            return true;
        }
        if (whyNamesLetter(a)
                || a.contains("is the correct answer")
                || a.contains("is the right answer")
                || a.equals("because it is correct")
                || a.contains("it makes sense")
                || a.contains("i guessed")) {
            return true;
        }
        if (WHY_REASON.matcher(a).find() || HAS_NUMBER.matcher(a).find() && a.contains("because")) {
            return false;
        }
        if (MANUAL.matcher(a).find()) {
            return false;
        }
        return a.split("\\s+").length < 8;
    }

    public static boolean whyNamesLetter(String answer) {
        String a = answer == null ? "" : answer.trim().toLowerCase(Locale.ROOT);
        return a.matches("because\\s+[a-d]\\b.*")
                || a.matches("[a-d]\\s+is\\s+(the )?(correct|right).*")
                || a.matches("^[a-d][.)\\s].*")
                || a.matches("^[a-d]$");
    }

    public static String defaultWhy() {
        return "Why did you choose that? Explain in your own words.";
    }

    public static boolean looksLikeMisconception(String answer) {
        String a = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        if (a.isBlank()) {
            return false;
        }
        boolean neighbour = a.contains("previous number")
                || a.contains("with the previous")
                || a.contains("previous value")
                || a.contains("the one before");
        boolean running = a.contains("so far")
                || a.contains("best")
                || a.contains("largest")
                || a.contains("smallest")
                || a.contains("remember");
        return neighbour && !running;
    }

    public static Ask misconceptionAsk(LabBrief brief) {
        return misconceptionAsk(brief, null);
    }

    public static Ask misconceptionAsk(LabBrief brief, String labQuestion) {
        String sample = cleanSample(brief != null ? brief.getSample() : null);
        LabKind kind = labKind(labQuestion);
        if (kind == LabKind.ONCE || (kind == LabKind.BEST && computeOnceBrief(brief))) {
            return fallbackAsk(2, brief, labQuestion == null ? "" : labQuestion, false);
        }
        if (kind == LabKind.COUNT || looksLikeCountBrief(brief)) {
            return new Ask("text",
                    "Suppose the word is " + sample
                            + ". If you only looked at the first letter, how would you know a later letter was a vowel?",
                    List.of(), "", defaultWhy());
        }
        return new Ask("text",
                "Interesting. Suppose the array is " + sample
                        + ". If you only compare each number with the previous number, how would you know a later value is larger than the largest you have seen so far?",
                List.of(), "", defaultWhy());
    }

    private static McqOption findChoice(List<McqOption> choices, String letter) {
        if (choices == null) {
            return null;
        }
        for (McqOption option : choices) {
            if (option != null && letter.equalsIgnoreCase(option.getLetter())) {
                return option;
            }
        }
        return null;
    }

    private static Ask fallbackMcq(int stage, LabBrief brief, String labQuestion) {
        String sample = cleanSample(brief != null ? brief.getSample() : null);
        LabKind kind = labKind(labQuestion);
        return switch (clampStage(stage)) {
            case 1 -> switch (kind) {
                case ONCE -> mcq("Suppose the input is " + sample + ". How would you start, if you were doing it by hand?",
                        "B",
                        choice("A", "Loop through a list of leftover values"),
                        choice("B", "Use the given measurements and calculate once"),
                        choice("C", "Keep a running maximum while scanning"),
                        choice("D", "Compare only the first value with the last value"),
                        "You chose one calculation. If the sides are 4 and 6, what do you do with those two numbers?");
                case COUNT -> mcq("Suppose the input is " + sample + ". How would you start counting, if you were doing it by hand?",
                        "B",
                        choice("A", "Only look at the first and last letters"),
                        choice("B", "Look at each letter and keep a running count"),
                        choice("C", "Multiply the letters together"),
                        choice("D", "Remember the largest letter"),
                        "You chose to look at each letter. Why must you check every letter, not just some of them?");
                default -> mcq("Suppose the input is " + sample + ". How would you start finding the answer if you were doing it by hand?",
                        "B",
                        choice("A", "Only compare the first value with the last value"),
                        choice("B", "Look at the values one at a time and remember the best so far"),
                        choice("C", "Add every value together"),
                        choice("D", "Pick a value from the middle and stop"),
                        "You chose to look one at a time. Why is looking through every value enough to find the largest, instead of only checking the first and last?");
            };
            case 2 -> switch (kind) {
                case ONCE -> mcq("Which approach would work here?",
                        "A",
                        choice("A", "Compute once from the values already given"),
                        choice("B", "Compare every pair of leftover items"),
                        choice("C", "Scan a list and keep a running maximum"),
                        choice("D", "Sort the values and pick an end"),
                        "You chose one calculation. Why is one multiply enough here, with no leftover list to scan?");
                case COUNT -> mcq("Which approach would work for counting?",
                        "B",
                        choice("A", "Compare every letter with every other letter"),
                        choice("B", "Look through the word once and add one when a letter is a vowel"),
                        choice("C", "Keep a running maximum of letters"),
                        choice("D", "Only check the first letter"),
                        "You chose one look through the word. Why is one pass enough to count the vowels?");
                default -> mcq("Which approach would work?",
                        "B",
                        choice("A", "Sort the values and pick the last one"),
                        choice("B", "Compare each item with a current best value"),
                        choice("C", "Add all the items together"),
                        choice("D", "Compare only the first and last items"),
                        "You chose one look through, keeping a remembered best. Why is that cheaper than checking every pair?");
            };
            case 3 -> kind == LabKind.COUNT
                    ? mcq("Before you look at any letters, what should the count start as?",
                            "B",
                            choice("A", "The first letter"),
                            choice("B", "Zero"),
                            choice("C", "How many letters there are"),
                            choice("D", "One"),
                            "You chose zero. Why is 0 the right start for a count, before any letter has been checked?")
                    : mcq("Before you start comparing, what should you store first?",
                            "A",
                            choice("A", "The first item from the input"),
                            choice("B", "Always zero"),
                            choice("C", "The last item"),
                            choice("D", "How many items there are"),
                            "You chose the first item. If the list starts with 3, why is 3 a safer start than 0?");
            case 4 -> kind == LabKind.COUNT
                    ? mcq("If this letter is a vowel, what should happen to the count?",
                            "C",
                            choice("A", "Stop looking"),
                            choice("B", "Reset the count to zero"),
                            choice("C", "Add one to the count"),
                            choice("D", "Replace the count with this letter"),
                            "You chose add one. If the letter is e, why does the count go up, and if the letter is h, why does it stay?")
                    : mcq("If the current item is better than the value you stored, what should happen?",
                            "C",
                            choice("A", "Stop looking"),
                            choice("B", "Throw the current item away"),
                            choice("C", "Replace the stored value with this item"),
                            choice("D", "Add this item to a running total"),
                            "You chose replace. You are holding 8 and the next item is 2. Do you replace 8? Say yes or no, then why.");
            default -> kind == LabKind.COUNT
                    ? mcq("When should you stop looking through the rest of the letters?",
                            "B",
                            choice("A", "After the first vowel"),
                            choice("B", "After the last letter"),
                            choice("C", "After two letters"),
                            choice("D", "You never stop"),
                            "You have looked at h and e but not yet l. Are you finished counting, or do you still need the leftover letters? Why?")
                    : mcq("When should you stop looking through the rest?",
                            "B",
                            choice("A", "After the first time you find a better item"),
                            choice("B", "After the last item"),
                            choice("C", "After two items"),
                            choice("D", "You never stop"),
                            "You have looked at 3 and 8 but not yet 2. Are you finished, or do you still need to look at 2? Why?");
        };
    }

    private static Ask mcq(String question, String correct, McqOption a, McqOption b, McqOption c, McqOption d,
            String why) {
        return new Ask("mcq", question, List.of(a, b, c, d), correct, why);
    }

    private static McqOption choice(String letter, String text) {
        return new McqOption(letter, text);
    }

    public static Help fallbackHelp(int stage, int level, LabBrief brief, String originalQuestion) {
        return fallbackHelp(stage, level, brief, originalQuestion, false, null);
    }

    public static Help fallbackHelp(
            int stage, int level, LabBrief brief, String originalQuestion, boolean stuckOnWhy) {
        return fallbackHelp(stage, level, brief, originalQuestion, stuckOnWhy, null);
    }

    public static Help fallbackHelp(
            int stage, int level, LabBrief brief, String originalQuestion, boolean stuckOnWhy, String labQuestion) {
        int[] pair = samplePair(brief);
        int first = pair[0];
        int second = pair[1];
        int bigger = Math.max(first, second);
        int smaller = Math.min(first, second);
        String original = originalQuestion == null || originalQuestion.isBlank()
                ? fallbackQuestion(stage, brief, labQuestion == null ? "" : labQuestion)
                : originalQuestion;
        int s = clampStage(stage);
        LabKind kind = labKind(labQuestion);
        if (kind == LabKind.BEST && looksLikeCountBrief(brief)) {
            kind = LabKind.COUNT;
        }
        boolean once = kind == LabKind.ONCE;
        if (once && (s == 6 || s == 7) && !stuckOnWhy) {
            return computeOnceHelp(s, level, original, first, second);
        }
        if (once) {
            return onceHelp(s, level, original, first, second, stuckOnWhy);
        }
        if (kind == LabKind.COUNT) {
            return countHelp(s, level, original, stuckOnWhy, brief);
        }
        if (stuckOnWhy) {
            return whyHelp(s, level, original, smaller, bigger);
        }
        return switch (clampHelp(level)) {
            case 1 -> new Help(1, stageOneHint(s, smaller, bigger), original, "", "");
            case 2 -> s == 2
                    ? new Help(2,
                            "You do not have to match every item with every other item. One walk through the list is enough if you keep a single remembered value as you go.",
                            original,
                            "Why is one look through enough?",
                            "once")
                    : s == 3
                    ? new Help(2,
                            "Zero is not always in the list. The first real item is a safe thing to remember because it is actually there.",
                            original,
                            "Why is the first item safer than always starting at zero?",
                            "first")
                    : s == 4
                    ? new Help(2,
                            "You only swap when the new value beats the one already in your head. Look at "
                                    + bigger + " then " + smaller
                                    + ". Does the second beat the first? What stays in your head?",
                            original,
                            "Why do you leave " + smaller + " instead of replacing " + bigger + "?",
                            "leave")
                    : s == 5
                    ? new Help(2,
                            leftoverCheckHint(first, second),
                            original,
                            "Why are you not finished after only some of the items?",
                            "rest")
                    : s == 6
                    ? new Help(2,
                            "If you started by writing the answer, would you even know which list you were working on? What has to be in front of you first?",
                            original,
                            "",
                            "result")
                    : s == 7
                    ? new Help(2,
                            "Go two values at a time. After the first, what number are you holding? After the next, did you keep it or replace it?",
                            original,
                            "",
                            "")
                    : new Help(2,
                            "Look at just two values from the sample. After "
                                    + smaller + ", you still have to look at " + bigger
                                    + ". Why isn't checking only the first and last values enough?",
                            original,
                            "Why look at every value?",
                            "every");
            case 3 -> s == 2
                    ? new Help(3,
                            "Checking every pair means looking at the same items over and over. One walk is enough if you keep one remembered value and update it as you go.",
                            original,
                            "Why is one look through enough?",
                            "once")
                    : s == 3
                    ? new Help(3,
                            "If the first reading is -2 and you had stored 0, -2 looks worse than 0 even though -2 is a real value in the list. Start from the first reading so you never invent a number.",
                            original,
                            "Why would starting at 0 hide a colder (or smaller) first value?",
                            "-2")
                    : s == 4
                    ? new Help(3,
                            "Think of a high-score board. First score 50. Next score 70: does 70 beat 50? Next score 60: does 60 beat what is on the board now? After those three, what is still on the board?",
                            original,
                            "Why not replace 70 with 60?",
                            "70")
                    : s == 5
                    ? new Help(3,
                            leftoverCheckHint(first, second)
                                    + " Would you stop after 8, or still look at the next one?",
                            original,
                            "Why look at 2 as well?",
                            "2")
                    : s == 6
                    ? new Help(3,
                            "Picture a friend asking you to find the largest. They have to give you the numbers first. You only tell them the answer after you finish looking.",
                            original,
                            "",
                            "result")
                    : s == 7
                    ? new Help(3,
                            "Go two values at a time, as questions. After the first, what is in your head? After the next, did it beat that? Stop there. You do not need the whole list yet.",
                            original,
                            "",
                            "")
                    : new Help(3,
                            "If you only checked the two ends of the list, a bigger value in the middle would never be seen. The sample has more than two numbers. How would you catch the middle ones by hand?",
                            original,
                            "Why look through every value?",
                            "middle");
            default -> s == 5
                    ? new Help(4,
                            leftoverCheckHint(first, second)
                                    + " Stopping after the first improvement, or never stopping, both skip leftover numbers.",
                            original,
                            "",
                            "last")
                    : s == 6
                    ? new Help(4,
                            "First someone gives you the list. Last you say the largest. Everything else sits in the middle. What is in front of you at the start, and what do you tell them at the end?",
                            original,
                            "",
                            "result")
                    : s == 7
                    ? new Help(4,
                            "You only get to remember one number at a time. For each new value, ask: is this better than the one I am already holding? After a few values, what is left in your head, and why that one rather than an earlier one?",
                            original,
                            "",
                            "")
                    : new Help(4,
                            "You only get to remember one number at a time. For each new value, ask: is this better than the one I am already holding? Do not skip values. After the last one, what is left, and why that one?",
                            original,
                            "",
                            "no");
        };
    }

    private static String stageOneHint(int stage, int smaller, int bigger) {
        return switch (clampStage(stage)) {
            case 2 -> "You do not need every pair. One walk through the list is enough if you keep one remembered value as you go.";
            case 3 -> "Zero might not be in the list. The first real item is a safe start because it actually arrived.";
            case 4 -> "You only swap when the new value beats the one already in your head. Look at "
                    + bigger + " then " + smaller + ". Does the second beat the first? What stays in your head?";
            case 5 -> leftoverCheckHint(smaller, bigger);
            case 6 -> "Someone just handed you a list of numbers. What do you need in front of you before you can even start looking? After you pick a largest, what would you tell them?";
            default -> "Picture solving this by hand. After you have seen "
                    + smaller + ", you still have to look at " + bigger
                    + ". Remember one number in your head so you can compare it with the next value.";
        };
    }

    private static Help whyHelp(int stage, int level, String original, int smaller, int bigger) {
        int s = clampStage(stage);
        if (s == 3) {
            return switch (clampHelp(level)) {
                case 1 -> new Help(1,
                        "Zero might not even be in the list. If the list starts with " + smaller
                                + ", which starting value actually arrived, and which one did you invent?",
                        original,
                        "",
                        String.valueOf(smaller));
                case 2 -> new Help(2,
                        "Zero is not always in the list. The first real item is a safe thing to remember because it is actually there.",
                        original,
                        "Why is the first item safer than always starting at zero?",
                        "first");
                case 3 -> new Help(3,
                        "If the first reading is -2 and you had stored 0, -2 looks worse than 0 even though -2 is a real value in the list. Start from the first reading so you never invent a number.",
                        original,
                        "Why would starting at 0 hide a colder (or smaller) first value?",
                        "-2");
                default -> new Help(4,
                        "Start from the first item so the remembered value is something that actually arrived. Do not invent 0.",
                        original,
                        "",
                        "first");
            };
        }
        if (s == 5) {
            return switch (clampHelp(level)) {
                case 1 -> new Help(1,
                        leftoverCheckHint(smaller, bigger),
                        original,
                        "",
                        "2");
                case 2 -> new Help(2,
                        leftoverCheckHint(smaller, bigger)
                                + " Would you stop after the first time you find a better one, or keep going?",
                        original,
                        "Why after the last item?",
                        "last");
                default -> new Help(3,
                        leftoverCheckHint(smaller, bigger)
                                + " If you stopped at 8, which leftover number would you never see?",
                        original,
                        "Why are you not finished after only some of the items?",
                        "rest");
            };
        }
        if (s == 4) {
            return switch (clampHelp(level)) {
                case 1 -> new Help(1,
                        "You only swap when the new value beats the one already in your head. Look at "
                                + bigger + " then " + smaller + ". Does the second beat the first? What stays in your head?",
                        original,
                        "",
                        "keep");
                case 2 -> new Help(2,
                        "This is only a keep-or-replace choice. Look at " + bigger + " then " + smaller
                                + ". Does the second beat the first?",
                        original,
                        "Why do you leave " + smaller + " instead of replacing " + bigger + "?",
                        "leave");
                default -> new Help(3,
                        "Think of a high-score board. First 50, then 70, then 60. After each score, ask: does this beat what is on the board? What is still there at the end?",
                        original,
                        "Why is 70 still the remembered score after 60?",
                        "70");
            };
        }
        return switch (clampHelp(level)) {
            case 1 -> new Help(1,
                    "Say it with just two numbers. After you see " + smaller + ", then " + bigger
                            + ", which one would still be in your head, and why that one rather than the other?",
                    original,
                    "",
                    String.valueOf(bigger));
            case 2 -> new Help(2,
                    "This is only a keep-or-replace choice. You are holding " + bigger
                            + " and the next number is " + smaller
                            + ". Does the second beat the first? What stays in your head?",
                    original,
                    "Why do you keep " + bigger + " instead of replacing it with " + smaller + "?",
                    "keep");
            case 3 -> new Help(3,
                    "Same idea as a high-score board. First 50, then 70, then 60. After each score, ask: does this beat what is on the board? What is still there at the end?",
                    original,
                    "Why is 70 still the remembered score after 60?",
                    "70");
            default -> new Help(4,
                    "You only get to remember one number at a time. For each new value, ask: is this better than the one I am already holding? After the last one, what is left, and why that one rather than adding them or checking every pair?",
                    original,
                    "",
                    "best");
        };
    }

    public static boolean helpLooksSafe(Help help, int level, String originalQuestion) {
        if (help == null || help.question() == null || help.question().isBlank()) {
            return false;
        }
        String hint = help.hint() == null ? "" : help.hint().trim();
        String question = help.question().trim();
        String blob = (hint + " " + question).toLowerCase(Locale.ROOT);
        if (blob.contains("the answer is") || blob.contains("initialize") || !Checker.questionIsSafe(question)) {
            return false;
        }
        if (!hint.isBlank() && (Checker.questionLeaksStep(hint) || Checker.hintDumpsAnswer(hint, originalQuestion)
                || Checker.hintDumpsAnswer(hint, question) || Checker.questionStatesTheMethod(hint)
                || Checker.hintMissesTheQuestion(hint, originalQuestion)
                || Checker.hintMissesTheQuestion(hint, question))) {
            return false;
        }
        if (hint.matches("-?\\d+") || thinSlogan(hint)) {
            return false;
        }
        if (Checker.sameQuestion(hint, originalQuestion) || Checker.sameQuestion(hint, question)
                || Checker.hintCopiesQuestion(hint, originalQuestion)
                || Checker.hintCopiesQuestion(hint, question)) {
            return false;
        }
        return true;
    }

    public static boolean helperLooksRight(String question, String answer, Help help) {
        String a = answer == null ? "" : answer.toLowerCase(Locale.ROOT);
        if (a.isBlank()) {
            return false;
        }
        if (help != null && help.expected() != null && !help.expected().isBlank()) {
            if (a.contains(help.expected().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        if (a.contains("because") && (a.contains("larger") || a.contains("bigger") || a.contains("greater")
                || a.contains("smaller") || a.contains("less"))) {
            return true;
        }
        if ((a.contains("no") || a.contains("keep") || a.contains("not"))
                && (question != null && question.toLowerCase(Locale.ROOT).contains("60"))) {
            return true;
        }
        if (a.contains("8") && question != null && question.contains("8")) {
            return true;
        }
        if (a.contains("70") && question != null && question.contains("70")) {
            return true;
        }
        if ((a.contains("negative") || a.contains("zero") || a.contains("first"))
                && question != null
                && (question.contains("0") || question.toLowerCase(Locale.ROOT).contains("zero")
                        || question.toLowerCase(Locale.ROOT).contains("first"))) {
            return true;
        }
        return false;
    }

    public static boolean thinSlogan(String hint) {
        if (hint == null) {
            return true;
        }
        String text = hint.trim().toLowerCase(Locale.ROOT);
        if (text.length() < 40) {
            return true;
        }
        return text.equals("let's make it smaller.")
                || text.equals("lets make it smaller.")
                || text.startsWith("let's make it smaller")
                || text.startsWith("lets make it smaller")
                || text.startsWith("try it as a concrete")
                || text.startsWith("try a concrete")
                || text.equals("a concrete example")
                || text.equals("a smaller question to get you started");
    }

    private static String leftoverCheckHint(int first, int second) {
        return "You have already looked at " + first + " and " + second
                + ". If there was one more number after that, would you skip checking whether it was larger, or would you look at it too?";
    }

    private static int[] samplePair(LabBrief brief) {
        String sample = cleanSample(brief != null ? brief.getSample() : null);
        java.util.regex.Matcher matcher = Pattern.compile("-?\\d+").matcher(sample);
        Integer first = null;
        Integer second = null;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group());
            if (first == null) {
                first = value;
            } else {
                second = value;
                break;
            }
        }
        if (first == null) {
            return new int[] {3, 8};
        }
        if (second == null || second.equals(first)) {
            return new int[] {first, first + 5};
        }
        return new int[] {first, second};
    }

    private static Help computeOnceHelp(int stage, int level, String original, int first, int second) {
        if (clampStage(stage) == 6) {
            return new Help(clampHelp(level),
                    "Someone just handed you " + first + " and " + second
                            + ". What two numbers do you need in front of you before you can even start? After you work out the area, what would you tell them?",
                    original,
                    "",
                    "area");
        }
        return new Help(clampHelp(level),
                "You already have both measurements (" + first + " and " + second
                        + "). What one calculation turns those two numbers into the amount of space inside? You are not scanning a leftover list.",
                original,
                "",
                String.valueOf(first * second));
    }

    private static Help onceHelp(int stage, int level, String original, int first, int second, boolean stuckOnWhy) {
        int s = clampStage(stage);
        int lv = clampHelp(level);
        if (s >= 6) {
            return computeOnceHelp(s, lv, original, first, second);
        }
        String hint = switch (lv) {
            case 1 -> "You already have " + first + " and " + second
                    + ". What one calculation turns those two measurements into the area?";
            case 2 -> "There is no leftover list. Length and width are both already given. Multiply them once.";
            case 3 -> "If the sides are " + first + " and " + second + ", write " + first + " × " + second
                    + " in your head. What number comes out? That is the area.";
            default -> "Area is length times width. You do not scan leftover items and you do not keep a running maximum.";
        };
        return new Help(lv, hint, original, stuckOnWhy ? "" : "What do you do with the two measurements?",
                String.valueOf(first * second));
    }

    private static Help countHelp(int stage, int level, String original, boolean stuckOnWhy, LabBrief brief) {
        int s = clampStage(stage);
        int lv = clampHelp(level);
        String word = countSampleWord(brief);
        return switch (s) {
            case 2 -> new Help(lv,
                    "You do not need to match every letter with every other letter. One look through "
                            + word + " is enough if you add one when the letter is a vowel.",
                    original, "Why is one look through enough?", "once");
            case 3 -> new Help(lv,
                    lv <= 2
                            ? "A count is how many you have found so far. Before you look at any letter of "
                                    + word + ", that number is 0. How many vowels have you found yet?"
                            : "Before any letter is checked, you have found none. That start is 0, not the first letter.",
                    original, "", "0");
            case 4 -> new Help(lv,
                    "Take one letter. If it is a, e, i, o, or u, the count goes up by one. If it is not, the count stays. For the e in "
                            + word + ", does the count change?",
                    original, "", "add");
            case 5 -> new Help(lv,
                    "You have already looked at some letters of " + word
                            + ". If there was one more letter, would you skip it, or would you still check whether it was a vowel?",
                    original, "", "rest");
            case 6 -> new Help(lv,
                    "Someone just handed you the word " + word
                            + ". What do you see first? After you finish counting, what would you tell them?",
                    original, "", "count");
            case 7 -> new Help(lv,
                    "Go letter by letter through " + word
                            + ". After each letter, what is the count? Do not skip letters.",
                    original, "", "2");
            default -> new Help(lv,
                    lv == 1
                            ? "Look at " + word + " one letter at a time. Start a count at 0. For each letter, ask: is this a vowel?"
                            : "h is not a vowel, so the count stays. e is a vowel, so the count goes up by one. Keep going through "
                                    + word + ".",
                    original, "", "count");
        };
    }

    static String countWalkQuestion(LabBrief brief) {
        String word = countSampleWord(brief);
        String letters = word.chars()
                .filter(Character::isLetter)
                .mapToObj(c -> String.valueOf((char) c))
                .reduce((a, b) -> a + ", then " + b)
                .orElse("h, then e, then l, then l, then o");
        return "The letters come in as " + letters + ". After each letter, what is the vowel count?";
    }

    static String countSampleWord(LabBrief brief) {
        String sample = cleanSample(brief != null ? brief.getSample() : null);
        String letters = sample.replaceAll("[^A-Za-z]", "");
        return letters.isBlank() ? "hello" : letters;
    }

    static String byHandHoldQuestion(LabBrief brief) {
        java.util.List<Integer> nums = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = Pattern.compile("-?\\d+")
                .matcher(cleanSample(brief != null ? brief.getSample() : null));
        while (matcher.find() && nums.size() < 5) {
            nums.add(Integer.parseInt(matcher.group()));
        }
        if (nums.size() >= 5) {
            return "The numbers come in as " + nums.get(0) + ", then " + nums.get(1) + ", then " + nums.get(2)
                    + ", then " + nums.get(3) + ", then " + nums.get(4) + ". After " + nums.get(0) + " then "
                    + nums.get(1) + " then " + nums.get(2) + ", what number are you holding? Then " + nums.get(3)
                    + "? Then " + nums.get(4) + "?";
        }
        if (nums.size() >= 3) {
            return "The numbers come in as " + nums.get(0) + ", then " + nums.get(1) + ", then " + nums.get(2)
                    + ". After " + nums.get(0) + " then " + nums.get(1) + ", what number are you holding? Then "
                    + nums.get(2) + "?";
        }
        return "Look at the numbers by hand, one at a time. After the first few, what number are you holding? What about after the last one?";
    }
}
