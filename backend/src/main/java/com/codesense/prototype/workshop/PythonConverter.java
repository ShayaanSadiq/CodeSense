package com.codesense.prototype.workshop;

import com.codesense.prototype.model.PseudoLine;
import com.codesense.prototype.model.PythonStep;
import com.codesense.prototype.model.WalkLine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PythonConverter {

    public record Program(String source, String sampleInput, String expectedOutput, List<PythonStep> steps) {
    }

    private enum Kind {
        LARGEST,
        SMALLEST,
        VOWELS,
        AREA,
        SQUARE,
        PRODUCT,
        COUNT,
        GENERIC
    }

    private PythonConverter() {
    }

    public static Program from(String labQuestion, List<PseudoLine> lines) {
        return from(labQuestion, lines, null);
    }

    public static Program from(String labQuestion, List<PseudoLine> lines, String rawInput) {
        Kind kind = kind(labQuestion, lines);
        List<PseudoLine> plan = lines == null ? List.of() : lines;
        return switch (kind) {
            case LARGEST -> largest(plan, parseNumbers(rawInput, List.of(3, 8, 2)));
            case SMALLEST -> smallest(plan, parseNumbers(rawInput, List.of(3, 8, 2)));
            case VOWELS -> vowels(plan, parseText(rawInput, "hello"));
            case SQUARE -> square(plan, parseSide(rawInput, 5));
            case AREA, PRODUCT -> product(plan, parsePair(rawInput, 4, 6), kind == Kind.AREA);
            case COUNT -> count(plan, parseNumbers(rawInput, List.of(1, 2, 3, 4)));
            case GENERIC -> generic(plan, parseNumbers(rawInput, List.of(3, 8, 2)));
        };
    }

    private static final Pattern PREPROCESSOR = Pattern.compile(
            "(?i)^#\\s*(include|define|undef|ifdef|ifndef|if|elif|else|endif|pragma|error|warning)\\b");

    public static List<WalkLine> fallbackWalk(Program program) {
        List<WalkLine> steps = new ArrayList<>();
        if (program == null || program.source() == null) {
            return steps;
        }
        boolean inBlock = false;
        for (String raw : program.source().split("\\R", -1)) {
            Scan scan = scanLine(raw.trim(), inBlock);
            inBlock = scan.inBlock();
            if (!scan.runnable()) {
                continue;
            }
            steps.add(explainLine(raw.trim(), program));
        }
        return steps;
    }

    public static List<WalkLine> keepRunnable(List<WalkLine> walk) {
        if (walk == null || walk.isEmpty()) {
            return List.of();
        }
        List<WalkLine> kept = new ArrayList<>();
        for (WalkLine step : walk) {
            if (step == null) {
                continue;
            }
            String code = step.getCode();
            if (code != null && !code.isBlank() && isCommentOrBlank(code)) {
                continue;
            }
            kept.add(step);
        }
        return kept;
    }

    public static boolean isCommentOrBlank(String line) {
        return !scanLine(line == null ? "" : line.trim(), false).runnable();
    }

    private record Scan(boolean runnable, boolean inBlock) {
    }

    private static Scan scanLine(String trimmed, boolean inBlock) {
        if (trimmed.isEmpty()) {
            return new Scan(false, inBlock);
        }
        if (inBlock) {
            return new Scan(false, !trimmed.contains("*/"));
        }
        if (trimmed.startsWith("//") || trimmed.startsWith("*/")) {
            return new Scan(false, false);
        }
        if (trimmed.startsWith("/*")) {
            return new Scan(false, !trimmed.contains("*/"));
        }
        if (trimmed.startsWith("#") && !PREPROCESSOR.matcher(trimmed).find()) {
            return new Scan(false, false);
        }
        return new Scan(true, false);
    }

    private static WalkLine explainLine(String line, Program program) {
        String sample = program.sampleInput();
        String result = program.expectedOutput();
        if (line.startsWith("#include")
                || line.startsWith("public class")
                || line.startsWith("public static void main")
                || line.startsWith("int main")
                || line.equals("{")
                || line.equals("}")
                || line.equals("return 0;")) {
            return new WalkLine(line, "The program wrapper",
                    "This line starts or ends the program. It does not compute the answer.",
                    "The language needs this shell so the steps inside can run.");
        }
        if (line.startsWith("for ") || line.startsWith("for (")) {
            return new WalkLine(line, "Look at each item",
                    "This loop visits every value in the collection, one at a time.",
                    "Without it the script would only see the first value and miss the rest.");
        }
        if (line.startsWith("if ") || line.startsWith("if (")) {
            return new WalkLine(line, "A yes-or-no check",
                    "This line asks whether the current value should change what is stored.",
                    "The update below only happens when this check is true.");
        }
        if (line.startsWith("print(") || line.contains("System.out.println") || line.startsWith("printf(")) {
            return new WalkLine(line, "Show the answer",
                    "This prints the one result. With these values that result is " + result + ".",
                    "Without this line the script would finish and never show the answer.");
        }
        if (line.contains("*")) {
            return new WalkLine(line, "One calculation",
                    "This multiplies the given values in a single step.",
                    "The printed answer comes from this line.");
        }
        if (line.contains("[0]") || line.endsWith("= 0")) {
            return new WalkLine(line, "Remember a starting value",
                    "This stores a first working value before later lines look at more items.",
                    "Later checks need something to compare against. Here the given values start as "
                            + sample + ".");
        }
        if (line.contains("=") && !line.contains("==")) {
            if (line.contains("number") && !line.contains("[")) {
                return new WalkLine(line, "Keep the better value",
                        "When the check above is true, the stored value becomes this item.",
                        "This is how the remembered answer improves as the loop continues.");
            }
            if (line.contains("count + 1") || line.contains("count=count+1")) {
                return new WalkLine(line, "Add one to the count",
                        "The current item matched, so the count goes up by one.",
                        "This is the only line that changes the running total.");
            }
            return new WalkLine(line, "Store what you are given",
                    "This line keeps the starting data. Here that is " + sample + ".",
                    "Every later line reads this value. Without it the script has nothing to work on.");
        }
        return new WalkLine(line, "This line of the script",
                "It is part of turning the plan into a program for these values: " + sample + ".",
                "The script needs each line in this order to reach " + result + ".");
    }

    public static boolean answersMatch(String answer, String expected) {
        if (expected == null) {
            return false;
        }
        String got = normalize(answer);
        String want = normalize(expected);
        if (got.isEmpty()) {
            return false;
        }
        if (got.equals(want) || want.contains(got) || got.contains(want)) {
            return true;
        }
        return numbersOf(got).equals(numbersOf(want)) && !numbersOf(want).isEmpty();
    }

    private static Kind kind(String lab, List<PseudoLine> lines) {
        String text = (lab == null ? "" : lab) + " " + texts(lines);
        if (find(text, "\\bsquare\\b") || (find(text, "\\barea\\b") && find(text, "\\bside\\b") && !find(text, "\\b(rectangle|length|width)\\b"))) {
            return Kind.SQUARE;
        }
        if (find(text, "\\b(area|rectangle|length|width)\\b")) {
            return Kind.AREA;
        }
        if (find(text, "\\b(multiply|product|times)\\b")) {
            return Kind.PRODUCT;
        }
        if (find(text, "vowel")) {
            return Kind.VOWELS;
        }
        if (find(text, "\\b(smallest|min)\\b")) {
            return Kind.SMALLEST;
        }
        if (find(text, "\\b(largest|biggest|max)\\b")) {
            return Kind.LARGEST;
        }
        if (find(text, "\\bcount\\b") || find(text, "\\beven\\b")) {
            return Kind.COUNT;
        }
        if (hasVerb(lines, "COMPUTE")) {
            return Kind.PRODUCT;
        }
        return Kind.GENERIC;
    }

    private static Program largest(List<PseudoLine> plan, List<Integer> numbers) {
        int max = numbers.get(0);
        String source = """
                # What you are given
                numbers = %s

                # Remember
                max_value = numbers[0]

                # Look at each
                for number in numbers:
                    # Yes or no
                    if number > max_value:
                        max_value = number

                # Show the answer
                print(max_value)
                """.formatted(numbers);
        List<PythonStep> steps = new ArrayList<>();
        steps.add(step(plan, "INPUT", "What numbers does the program start with?", join(numbers),
                "It starts with " + join(numbers) + "."));
        steps.add(step(plan, "SET", "What is max after the first number is remembered?", String.valueOf(max),
                "max starts as " + max + "."));
        for (int i = 1; i < numbers.size(); i++) {
            int next = numbers.get(i);
            if (next > max) {
                max = next;
            }
            steps.add(step(plan, "IF", "The next number is " + next + ". What is max after this check?",
                    String.valueOf(max), "After " + next + ", max is " + max + "."));
        }
        steps.add(step(plan, "OUTPUT", "What number does it print?", String.valueOf(max),
                "It prints " + max + "."));
        return new Program(source, join(numbers), String.valueOf(max), steps);
    }

    private static Program smallest(List<PseudoLine> plan, List<Integer> numbers) {
        int min = numbers.get(0);
        String source = """
                # What you are given
                numbers = %s

                # Remember
                min_value = numbers[0]

                # Look at each
                for number in numbers:
                    # Yes or no
                    if number < min_value:
                        min_value = number

                # Show the answer
                print(min_value)
                """.formatted(numbers);
        List<PythonStep> steps = new ArrayList<>();
        steps.add(step(plan, "INPUT", "What numbers does the program start with?", join(numbers),
                "It starts with " + join(numbers) + "."));
        steps.add(step(plan, "SET", "What is min after the first number is remembered?", String.valueOf(min),
                "min starts as " + min + "."));
        for (int i = 1; i < numbers.size(); i++) {
            int next = numbers.get(i);
            if (next < min) {
                min = next;
            }
            steps.add(step(plan, "IF", "The next number is " + next + ". What is min after this check?",
                    String.valueOf(min), "After " + next + ", min is " + min + "."));
        }
        steps.add(step(plan, "OUTPUT", "What number does it print?", String.valueOf(min),
                "It prints " + min + "."));
        return new Program(source, join(numbers), String.valueOf(min), steps);
    }

    private static Program vowels(List<PseudoLine> plan, String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if ("aeiouAEIOU".indexOf(text.charAt(i)) >= 0) {
                count++;
            }
        }
        String source = """
                # What you are given
                text = %s

                # Remember
                count = 0

                # Look at each
                for letter in text:
                    # Yes or no
                    if letter in "aeiouAEIOU":
                        count = count + 1

                # Show the answer
                print(count)
                """.formatted(pythonString(text));
        List<PythonStep> steps = new ArrayList<>();
        steps.add(step(plan, "INPUT", "What text does the program start with?", text,
                "It starts with “" + text + "”."));
        steps.add(step(plan, "SET", "What is count before any letter is checked?", "0",
                "count starts at 0."));
        int running = 0;
        for (int i = 0; i < text.length(); i++) {
            char letter = text.charAt(i);
            if ("aeiouAEIOU".indexOf(letter) >= 0) {
                running++;
            }
            steps.add(step(plan, "IF", "The next letter is “" + letter + "”. What is count after this check?",
                    String.valueOf(running), "After “" + letter + "”, count is " + running + "."));
        }
        steps.add(step(plan, "OUTPUT", "What number does it print?", String.valueOf(count),
                "It prints " + count + "."));
        return new Program(source, text, String.valueOf(count), steps);
    }

    private static Program square(List<PseudoLine> plan, int side) {
        int result = side * side;
        String source = """
                # What you are given
                side = %d

                # One calculation
                area = side * side

                # Show the answer
                print(area)
                """.formatted(side);
        List<PythonStep> steps = new ArrayList<>();
        steps.add(step(plan, "INPUT", "What side length does it start with?", String.valueOf(side),
                "It starts with side " + side + "."));
        steps.add(step(plan, "COMPUTE", "What is " + side + " times " + side + "?",
                String.valueOf(result), side + " × " + side + " is " + result + "."));
        steps.add(step(plan, "OUTPUT", "What number does it print?", String.valueOf(result),
                "It prints " + result + "."));
        return new Program(source, String.valueOf(side), String.valueOf(result), steps);
    }

    private static Program product(List<PseudoLine> plan, int[] pair, boolean area) {
        int result = pair[0] * pair[1];
        String left = area ? "length" : "first";
        String right = area ? "width" : "second";
        String name = area ? "area" : "product";
        String source = """
                # What you are given
                %s = %d
                %s = %d

                # One calculation
                %s = %s * %s

                # Show the answer
                print(%s)
                """.formatted(left, pair[0], right, pair[1], name, left, right, name);
        List<PythonStep> steps = new ArrayList<>();
        steps.add(step(plan, "INPUT",
                area ? "What length and width does it start with?" : "What two numbers does it start with?",
                pair[0] + " and " + pair[1],
                "It starts with " + pair[0] + " and " + pair[1] + "."));
        steps.add(step(plan, "COMPUTE", "What is " + pair[0] + " times " + pair[1] + "?",
                String.valueOf(result), pair[0] + " × " + pair[1] + " is " + result + "."));
        steps.add(step(plan, "OUTPUT", "What number does it print?", String.valueOf(result),
                "It prints " + result + "."));
        return new Program(source, pair[0] + " and " + pair[1], String.valueOf(result), steps);
    }

    private static Program count(List<PseudoLine> plan, List<Integer> numbers) {
        int count = 0;
        for (int number : numbers) {
            if (number % 2 == 0) {
                count++;
            }
        }
        String source = """
                # What you are given
                numbers = %s

                # Remember
                count = 0

                # Look at each
                for number in numbers:
                    # Yes or no
                    if number %% 2 == 0:
                        count = count + 1

                # Show the answer
                print(count)
                """.formatted(numbers);
        List<PythonStep> steps = new ArrayList<>();
        steps.add(step(plan, "INPUT", "What numbers does the program start with?", join(numbers),
                "It starts with " + join(numbers) + "."));
        steps.add(step(plan, "SET", "What is count before any number is checked?", "0",
                "count starts at 0."));
        int running = 0;
        for (int number : numbers) {
            if (number % 2 == 0) {
                running++;
            }
            steps.add(step(plan, "IF", "The next number is " + number + ". What is count after this check?",
                    String.valueOf(running), "After " + number + ", count is " + running + "."));
        }
        steps.add(step(plan, "OUTPUT", "What number does it print?", String.valueOf(count),
                "It prints " + count + "."));
        return new Program(source, join(numbers), String.valueOf(count), steps);
    }

    private static Program generic(List<PseudoLine> plan, List<Integer> numbers) {
        if (hasVerb(plan, "COMPUTE")) {
            return product(plan, new int[] { numbers.get(0), numbers.size() > 1 ? numbers.get(1) : 6 }, false);
        }
        return largest(plan, numbers);
    }

    private static PythonStep step(List<PseudoLine> plan, String verb, String prompt, String expected, String reveal) {
        String lineId = "";
        for (PseudoLine line : plan) {
            if (line != null && line.getText() != null && line.getText().toUpperCase(Locale.ROOT).startsWith(verb)) {
                lineId = line.getId();
                break;
            }
        }
        return new PythonStep("P-" + verb + "-" + prompt.hashCode(), lineId, prompt, expected, reveal);
    }

    private static List<Integer> parseNumbers(String raw, List<Integer> fallback) {
        List<Integer> found = numbersOf(raw);
        return found.isEmpty() ? fallback : found;
    }

    private static int parseSide(String raw, int fallback) {
        List<Integer> found = numbersOf(raw);
        return found.isEmpty() ? fallback : found.get(0);
    }

    private static int[] parsePair(String raw, int left, int right) {
        List<Integer> found = numbersOf(raw);
        if (found.size() >= 2) {
            return new int[] { found.get(0), found.get(1) };
        }
        return new int[] { left, right };
    }

    private static String parseText(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String cleaned = raw.trim().replaceAll("^['\"]|['\"]$", "");
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static List<Integer> numbersOf(String raw) {
        List<Integer> numbers = new ArrayList<>();
        if (raw == null) {
            return numbers;
        }
        Matcher matcher = Pattern.compile("-?\\d+").matcher(raw);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }

    private static String join(List<Integer> numbers) {
        List<String> parts = new ArrayList<>();
        for (Integer number : numbers) {
            parts.add(String.valueOf(number));
        }
        return String.join(", ", parts);
    }

    private static String pythonString(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String texts(List<PseudoLine> lines) {
        if (lines == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (PseudoLine line : lines) {
            if (line != null && line.getText() != null) {
                builder.append(' ').append(line.getText());
            }
        }
        return builder.toString();
    }

    private static boolean hasVerb(List<PseudoLine> lines, String verb) {
        if (lines == null) {
            return false;
        }
        for (PseudoLine line : lines) {
            if (line != null && line.getText() != null
                    && line.getText().toUpperCase(Locale.ROOT).startsWith(verb)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static boolean find(String text, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text).find();
    }
}
