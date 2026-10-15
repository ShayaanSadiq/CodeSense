package com.codesense.prototype.workshop;

import com.codesense.prototype.model.LabBrief;
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
    private static final Pattern YES_NO_START = Pattern.compile(
            "^(do|does|did|is|are|was|were|can|could|should|would|will|have|has|had)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HOW_START = Pattern.compile("^how\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern IO_IN = Pattern.compile(
            "\\b(input|comes? in|given|receive|receives|data)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IO_OUT = Pattern.compile(
            "\\b(output|comes? out|print|show|result|produce|give back)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IO_OFF_TOPIC = Pattern.compile(
            "\\b(empty|only one|one element|compare|loop|walk|each item|algorithm|process|processes|find|finds|steps?|method|scan|go through)\\b",
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

    public static boolean questionStatesTheMethod(String question) {
        String q = question == null ? "" : question.toLowerCase();
        if (q.isBlank()) {
            return true;
        }
        if (q.contains("what would happen if you")
                || q.contains("what happens if you compared")
                || q.contains("what happens if you updated")
                || q.contains("what happens if you kept")
                || q.contains("what happens if you scanned")
                || q.contains("what would happen if we")) {
            return true;
        }
        if (q.contains("current maximum")
                || q.contains("current max")
                || q.contains("running max")
                || q.contains("running maximum")
                || q.contains("current largest")
                || q.contains("algorithm")
                || q.contains("trace the steps")
                || q.contains("trace your plan")
                || q.contains("trace the algorithm")
                || q.contains("maximum is updated")
                || q.contains("how the maximum")
                || q.contains("updated at each step")
                || q.contains("update the maximum")
                || q.contains("updated the maximum")) {
            return true;
        }
        boolean compare = q.contains("compar");
        boolean update = q.contains("update")
                || q.contains("replace")
                || q.contains("whenever a larger")
                || q.contains("whenever a bigger");
        return (compare && update && (q.contains("maximum") || q.contains("largest")))
                || q.contains("compared each number with the current")
                || q.contains("updated the maximum whenever")
                || q.contains("updated the max whenever");
    }

    public static boolean questionIsSafe(String question) {
        String q = question == null ? "" : question.trim();
        if (q.length() < 24 || q.equals("...") || q.equals("…")) {
            return false;
        }
        return !questionLeaksStep(question) && !questionStatesTheMethod(question);
    }

    public static boolean questionFitsLab(String question, String labQuestion, LabBrief brief) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String q = question.toLowerCase();
        Socratic.LabKind kind = Socratic.labKind(labQuestion);
        boolean listWalk = q.contains("what number are you holding")
                || q.contains("number are you holding")
                || q.contains("after 3 then 8")
                || (q.contains("then 8") && q.contains("then 2"))
                || q.contains("then 10") && q.contains("then 5")
                || (q.contains("keeping the old one") && q.contains("replacing"));
        boolean runningBest = q.contains("running maximum") || q.contains("remember the best")
                || q.contains("safer start than 0") || q.contains("largest so far");
        boolean vowelCount = q.contains("vowel") || q.contains("letter");
        boolean areaCalc = (q.contains("length") && q.contains("width"))
                || q.contains("multiply") && (q.contains("area") || q.contains("measurements"));
        if (kind == Socratic.LabKind.ONCE || Socratic.computeOnceBrief(brief)) {
            if (listWalk || runningBest || vowelCount) {
                return false;
            }
            return true;
        }
        if (kind == Socratic.LabKind.COUNT) {
            if (listWalk || q.contains("largest") || q.contains("biggest")
                    || q.contains("safer start than 0") || areaCalc) {
                return false;
            }
            return true;
        }
        if (vowelCount && !q.contains("largest") && !q.contains("number")) {
            return false;
        }
        if (areaCalc) {
            return false;
        }
        if (!listWalk) {
            return true;
        }
        String home = ((labQuestion == null ? "" : labQuestion) + " "
                + (brief == null ? "" : String.valueOf(brief.getSample()) + " " + String.valueOf(brief.getInput())))
                .toLowerCase();
        boolean thisLabIsTheListWalk = (home.contains("largest") || home.contains("smallest") || home.contains("list")
                || home.contains("[3"))
                && home.contains("3") && home.contains("8");
        return thisLabIsTheListWalk;
    }

    public static int stageOfQuestion(String question) {
        String intent = questionIntent(question);
        return switch (intent) {
            case "strategy" -> 2;
            case "start" -> 3;
            case "update", "keep" -> 4;
            case "stop" -> 5;
            default -> {
                String q = question == null ? "" : question.toLowerCase();
                if ((q.contains("name the") || q.contains("name four") || q.contains("name three"))
                        && (q.contains("part") || q.contains("calculation") || q.contains("result"))) {
                    yield 6;
                }
                if ((q.contains("given first") || q.contains("be given") || q.contains("comes first")
                        || q.contains("which part") || q.contains("in front of you")
                        || q.contains("write down at the end") || q.contains("show at the end"))
                        && (q.contains("last") || q.contains("end") || q.contains("tell them")
                                || q.contains("show someone"))) {
                    yield 6;
                }
                if (q.contains("holding") || q.contains("multiply them")
                        || q.contains("what is the area") || q.contains("what number do you get")
                        || (q.contains("after each letter") && q.contains("count"))) {
                    yield 7;
                }
                if (q.contains("by hand") || q.contains("manually") || q.contains("identify the answer")
                        || q.contains("start counting") || q.contains("start finding")) {
                    yield 1;
                }
                if ((q.contains("vowel") || q.contains("letter"))
                        && (q.contains("add") || q.contains("encounter") || q.contains("count change"))) {
                    yield 4;
                }
                yield 0;
            }
        };
    }

    public static boolean questionFitsStage(String question, int stage) {
        int got = stageOfQuestion(question);
        if (got == 0) {
            return true;
        }
        return got == Socratic.clampStage(stage);
    }

    public static boolean questionGoesBackward(String question, int stage, List<String> asked) {
        int got = stageOfQuestion(question);
        if (got > 0 && got < Socratic.clampStage(stage)) {
            return true;
        }
        return alreadyAsked(question, asked);
    }

    public static boolean hintCopiesQuestion(String hint, String question) {
        if (hint == null || hint.isBlank() || question == null || question.isBlank()) {
            return false;
        }
        if (sameQuestion(hint, question)) {
            return true;
        }
        String h = normalizeQuestion(hint);
        String q = normalizeQuestion(question);
        if (q.length() >= 24 && h.contains(q)) {
            return true;
        }
        java.util.Set<String> wq = contentWords(question);
        java.util.Set<String> wh = contentWords(hint);
        if (wq.size() < 6) {
            return false;
        }
        long shared = wq.stream().filter(wh::contains).count();
        return shared >= 0.85 * wq.size();
    }

    public static boolean hintLeaksEarlierAsk(String hint, String current, List<String> asked) {
        if (hint == null || hint.isBlank() || asked == null || asked.isEmpty()) {
            return false;
        }
        for (String previous : asked) {
            if (previous == null || previous.isBlank() || sameQuestion(previous, current)) {
                continue;
            }
            if (hintCopiesQuestion(hint, previous) && !hintCopiesQuestion(hint, current)) {
                return true;
            }
            String snippet = normalizeQuestion(previous);
            if (snippet.length() >= 32 && normalizeQuestion(hint).contains(snippet)) {
                return true;
            }
        }
        return false;
    }

    public static boolean briefFitsLab(LabBrief brief, String labQuestion) {
        if (brief == null) {
            return false;
        }
        if (!Socratic.computeOnceLab(labQuestion) && !Socratic.computeOnceBrief(brief)) {
            return true;
        }
        String sample = brief.getSample() == null ? "" : brief.getSample();
        if (sample.contains("[") && sample.contains("3") && sample.contains("8") && sample.contains("2")) {
            return false;
        }
        return numberTokens(sample).size() >= 1
                && !sample.toLowerCase().contains("small example");
    }

    public static boolean hintDumpsAnswer(String hint, String question) {
        if (hint == null || hint.isBlank()) {
            return false;
        }
        String h = hint.toLowerCase();
        if (h.contains("the answer is")
                || h.contains("the correct answer")
                || h.contains("algorithm")
                || h.contains("update the maximum")
                || h.contains("updated the maximum")
                || h.contains("the maximum to")
                || h.contains("assign") && h.contains("maximum")
                || h.contains("trace the")
                || h.contains("you should be holding")
                || h.contains("you are holding the largest")
                || h.contains("you are holding the biggest")
                || h.contains("holding is the largest")
                || h.contains("holding is the biggest")
                || h.contains("kept the biggest")
                || h.contains("kept the largest")
                || h.contains("because you compared")
                || h.contains("so you would keep")
                || h.contains("so you keep ")
                || h.contains("so you leave ")
                || h.contains("so the remembered number stays")
                || h.contains("the number you keep is the bigger")) {
            return true;
        }
        String q = question == null ? "" : question.toLowerCase();
        boolean asksWhatTheyHold = q.contains("holding")
                || q.contains("what number are you")
                || q.contains("final number");
        if (asksWhatTheyHold && (h.contains("not the first")
                || h.contains("largest number you've seen")
                || h.contains("largest you have seen")
                || h.contains("biggest you have seen")
                || (h.contains("holding") && (h.contains("largest") || h.contains("biggest"))))) {
            return true;
        }
        int seeBeats = countPhrase(h, "when you see") + countPhrase(h, "after you see");
        if (seeBeats >= 3) {
            return true;
        }
        java.util.Set<String> questionNumbers = numberTokens(question);
        java.util.Set<String> hintNumbers = numberTokens(hint);
        if (questionNumbers.size() >= 3) {
            long overlap = hintNumbers.stream().filter(questionNumbers::contains).count();
            boolean walks = (h.contains("keep") || h.contains("update"))
                    && (h.contains("then") || h.contains("finally"));
            if (overlap >= 3 && walks) {
                return true;
            }
        }
        return false;
    }

    public static boolean hintMissesTheQuestion(String hint, String question) {
        String needed = questionIntent(question);
        if (!needed.isEmpty()) {
            String got = questionIntent(hint);
            if (!got.isEmpty() && !needed.equals(got)) {
                return true;
            }
            if (!hintCoversIntent(hint, needed)) {
                return true;
            }
        }
        return hintUsesWrongPicture(hint, needed);
    }

    static boolean hintCoversIntent(String hint, String intent) {
        String h = hint == null ? "" : hint.toLowerCase();
        boolean peopleLine = isPeopleLinePicture(h);
        return switch (intent) {
            case "start" -> {
                boolean namesZero = h.contains("zero") || h.contains("invent")
                        || Pattern.compile("\\b0\\b").matcher(h).find();
                yield namesZero && !peopleLine;
            }
            case "stop" -> h.contains("last") || h.contains("left") || h.contains("finished")
                    || h.contains("rest") || h.contains("later") || h.contains("leftover")
                    || h.contains("one more") || h.contains("skip checking") || h.contains("look at it too");
            case "strategy" -> h.contains("pair") || h.contains("once") || h.contains("one walk")
                    || h.contains("one look");
            case "update" -> h.contains("beat") || h.contains("replace") || h.contains("swap")
                    || h.contains("leave") || h.contains("score");
            case "keep" -> (h.contains("bigger") || h.contains("smaller") || h.contains("beat")
                    || h.contains("score") || h.contains("remember") || h.contains("keep"))
                    && !peopleLine;
            case "order" -> (h.contains("hand") || h.contains("given") || h.contains("in front")
                    || h.contains("write") || h.contains("tell them") || h.contains("show"))
                    && (h.contains("first") || h.contains("before") || h.contains("end")
                            || h.contains("last") || h.contains("start"))
                    && !h.contains("still have to look") && !h.contains("first and last values");
            default -> true;
        };
    }

    static boolean hintUsesWrongPicture(String hint, String intent) {
        String family = pictureFamily(hint);
        if (family.isEmpty()) {
            return false;
        }
        return !pictureAllowed(intent, family);
    }

    static String pictureFamily(String hint) {
        String h = hint == null ? "" : hint.toLowerCase();
        if (isPeopleLinePicture(h)) {
            return "people-line";
        }
        if (h.contains("high-score") || h.contains("high score") || h.contains("scoreboard")
                || (h.contains("board") && (h.contains("score") || h.contains("beat")))) {
            return "high-score";
        }
        if (h.contains("thermometer") || h.contains("temperature") || h.contains("colder")
                || h.contains("first reading")) {
            return "thermometer";
        }
        if (h.contains("race") || h.contains("lap") || h.contains("runner")) {
            return "race";
        }
        if (h.contains("tile")) {
            return "tiles";
        }
        return "";
    }

    static boolean pictureAllowed(String intent, String family) {
        return switch (intent == null ? "" : intent) {
            case "start" -> "thermometer".equals(family);
            case "stop" -> "race".equals(family);
            case "strategy" -> "people-line".equals(family);
            case "update", "keep" -> "high-score".equals(family);
            default -> !"people-line".equals(family);
        };
    }

    private static boolean isPeopleLinePicture(String h) {
        return h.contains("tallest") || h.contains("shortest") || h.contains("person")
                || h.contains("people in a line") || h.contains("in a line")
                || (h.contains("assume they") && h.contains("tall"));
    }

    public static String namedNumbers(String text) {
        return String.join(", ", numberTokens(text));
    }

    public static String questionIntent(String text) {
        String q = text == null ? "" : text.toLowerCase();
        if ((q.contains("given first") || q.contains("be given") || q.contains("comes first")
                || q.contains("which part") || q.contains("write down at the end")
                || q.contains("in front of you"))
                && (q.contains("last") || q.contains("end") || q.contains("tell them")
                        || q.contains("show someone") || q.contains("comes last"))) {
            return "order";
        }
        if (q.contains("stop") || q.contains("finished") || q.contains("last item")
                || q.contains("the rest") || q.contains("never stop") || q.contains("leftover")
                || q.contains("not yet") && q.contains("look")) {
            return "stop";
        }
        if (q.contains("zero") || q.contains("store first") || q.contains("first item")
                || q.contains("safer start") || q.contains("starting at 0") || q.contains("start as")) {
            return "start";
        }
        if (q.contains("every pair") || q.contains("one look") || q.contains("scan just once")
                || q.contains("looking through just once")) {
            return "strategy";
        }
        if (q.contains("replace") || q.contains("leave it") || q.contains("leave the")
                || q.contains("do you keep")) {
            return "update";
        }
        if (q.contains("keep track") || q.contains("which one would you keep")
                || q.contains("which one would you remember") || q.contains("makes a number the largest")
                || q.contains("what should you remember")) {
            return "keep";
        }
        return "";
    }

    private static int countPhrase(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (from < haystack.length()) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                break;
            }
            count++;
            from = at + needle.length();
        }
        return count;
    }

    private static java.util.Set<String> numberTokens(String text) {
        java.util.Set<String> numbers = new java.util.LinkedHashSet<>();
        if (text == null) {
            return numbers;
        }
        java.util.regex.Matcher matcher = Pattern.compile("-?\\d+").matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers;
    }

    public static boolean sameQuestion(String left, String right) {
        return normalizeQuestion(left).equals(normalizeQuestion(right)) && !normalizeQuestion(left).isEmpty();
    }

    public static boolean similarQuestion(String left, String right) {
        if (sameQuestion(left, right)) {
            return true;
        }
        int s1 = stageOfQuestion(left);
        int s2 = stageOfQuestion(right);
        java.util.Set<String> n1 = numberTokens(left);
        java.util.Set<String> n2 = numberTokens(right);
        long overlap = n1.stream().filter(n2::contains).count();
        if (overlap >= 2 && keepIntent(left) && keepIntent(right)) {
            return true;
        }
        java.util.Set<String> w1 = contentWords(left);
        java.util.Set<String> w2 = contentWords(right);
        if (w1.size() < 4 || w2.size() < 4) {
            return s1 > 0 && s1 == s2 && !questionIntent(left).isEmpty()
                    && questionIntent(left).equals(questionIntent(right));
        }
        long shared = w1.stream().filter(w2::contains).count();
        if (s1 > 0 && s1 == s2 && shared >= 4) {
            return true;
        }
        return shared >= 5 && shared >= 0.5 * Math.max(w1.size(), w2.size());
    }

    public static boolean alreadyAsked(String candidate, List<String> asked) {
        if (candidate == null || candidate.isBlank()) {
            return true;
        }
        if (asked == null) {
            return false;
        }
        return asked.stream().anyMatch(previous -> similarQuestion(candidate, previous));
    }

    private static boolean keepIntent(String question) {
        String q = question == null ? "" : question.toLowerCase();
        return q.contains("remember") || q.contains("keep") || q.contains("hold")
                || q.contains("in mind") || q.contains("bigger") || q.contains("larger");
    }

    private static java.util.Set<String> contentWords(String question) {
        java.util.Set<String> words = new java.util.LinkedHashSet<>();
        for (String word : normalizeQuestion(question).split(" ")) {
            if (word.length() > 2 && !STOP.contains(word)) {
                words.add(word);
            }
        }
        return words;
    }

    private static final java.util.Set<String> STOP = java.util.Set.of(
            "the", "and", "you", "would", "then", "that", "this", "what", "which",
            "one", "are", "for", "with", "from", "after", "see", "how", "why",
            "did", "does", "your", "own", "words", "chose", "choice");

    public static boolean isYesNoQuestion(String question) {
        String q = question == null ? "" : question.trim();
        return !q.isEmpty() && YES_NO_START.matcher(q).find();
    }

    public static boolean isHowQuestion(String question) {
        String q = question == null ? "" : question.trim();
        return !q.isEmpty() && HOW_START.matcher(q).find();
    }

    public static boolean staysOnSlot(Slot slot, String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty() || questionLeaksStep(q) || isYesNoQuestion(q) || isHowQuestion(q)) {
            return false;
        }
        if (slot == Slot.INPUT) {
            if (IO_OFF_TOPIC.matcher(q).find() || IO_OUT.matcher(q).find()) {
                return false;
            }
            return IO_IN.matcher(q).find();
        }
        if (slot == Slot.OUTPUT) {
            if (IO_OFF_TOPIC.matcher(q).find()) {
                return false;
            }
            return IO_OUT.matcher(q).find();
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
        if (a.isEmpty()) {
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
