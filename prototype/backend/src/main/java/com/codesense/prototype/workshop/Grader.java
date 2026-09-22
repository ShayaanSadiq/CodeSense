package com.codesense.prototype.workshop;

import com.codesense.prototype.model.Slot;
import java.util.regex.Pattern;

public final class Grader {

    public record Grade(boolean correct, String expected, String explanation) {
        public Grade withFallback(Grade fallback) {
            String expectedText = blank(expected) && fallback != null ? fallback.expected() : expected;
            String why = blank(explanation) && fallback != null ? fallback.explanation() : explanation;
            return new Grade(correct, expectedText, why);
        }
    }

    private Grader() {
    }

    public static Grade fromRules(Slot slot, String answer, String labQuestion) {
        String a = answer == null ? "" : answer;
        String lab = labQuestion == null ? "" : labQuestion;
        if (slot == Slot.IO) {
            return gradeIo(a, lab);
        }
        if (slot == Slot.STRATEGY) {
            return gradeStrategy(a, lab);
        }
        if (slot == Slot.MEMORY) {
            return gradeMemory(a, lab);
        }
        return gradeStop(a, lab);
    }

    private static Grade gradeIo(String answer, String lab) {
        if (isLargest(lab)) {
            return new Grade(
                    has(answer, "\\b(list|array)\\b") && has(answer, "\\b(largest|biggest|maximum|max)\\b"),
                    "A list of numbers comes in. The largest number comes out.",
                    "INPUT is the list the program is given. OUTPUT is the one value it must produce: the largest number in that list.");
        }
        if (isSmallest(lab)) {
            return new Grade(
                    has(answer, "\\b(list|array)\\b") && has(answer, "\\b(smallest|minimum|min)\\b"),
                    "A list of numbers comes in. The smallest number comes out.",
                    "INPUT is the list the program is given. OUTPUT is the one value it must produce: the smallest number in that list.");
        }
        if (isVowels(lab)) {
            return new Grade(
                    has(answer, "\\b(string|word|text|sentence)\\b") && has(answer, "vowel"),
                    "A string comes in. The number of vowels comes out.",
                    "INPUT is the text to inspect. OUTPUT is how many vowels that text contains.");
        }
        if (isArea(lab)) {
            return new Grade(
                    (has(answer, "\\blength\\b") || has(answer, "\\bwidth\\b") || has(answer, "\\brectangle\\b"))
                            && has(answer, "\\barea\\b"),
                    "The length and the width come in. The area comes out.",
                    "INPUT is the two measurements of the rectangle. OUTPUT is the area, the one result of multiplying them.");
        }
        return new Grade(
                has(answer, "\\b(comes in|given|input)\\b") && has(answer, "\\b(comes out|output|print|result)\\b"),
                "The given values come in. The result comes out.",
                "INPUT names the data the program receives. OUTPUT names the one result it should produce.");
    }

    private static Grade gradeStrategy(String answer, String lab) {
        if (isArea(lab)) {
            return new Grade(
                    has(answer, "\\b(comput|multiply|once|formula|product)\\b"),
                    "Compute the area in one step by multiplying length and width.",
                    "Nothing repeats: two numbers go in and one multiplication produces the area, so there is no loop.");
        }
        return new Grade(
                has(answer, "\\b(every|each|all|walk|scan|go through|look at)\\b"),
                "Look at every item in the input.",
                "The result depends on every value, so the procedure walks the whole input rather than stopping at the first item or computing once.");
    }

    private static Grade gradeMemory(String answer, String lab) {
        if (isLargest(lab)) {
            return new Grade(
                    has(answer, "\\b(max|biggest|largest|greatest|so far|first)\\b"),
                    "Remember the largest so far. Start it as the first number, then replace it when a bigger number appears.",
                    "You need one remembered value to compare against. Starting from the first number means every later number has something to beat.");
        }
        if (isSmallest(lab)) {
            return new Grade(
                    has(answer, "\\b(min|smallest|so far|first)\\b"),
                    "Remember the smallest so far. Start it as the first number, then replace it when a smaller number appears.",
                    "You need one remembered value to compare against. Starting from the first number means every later number has something to beat.");
        }
        if (isVowels(lab)) {
            return new Grade(
                    has(answer, "\\b(count|zero|0|so far)\\b"),
                    "Remember a count of vowels. Start it at 0, then add one when the current character is a vowel.",
                    "A count is the running total you keep while you walk the string.");
        }
        return new Grade(
                has(answer, "\\b(remember|keep|so far|start|first|zero)\\b"),
                "Remember the value you need between steps, and say where it starts.",
                "Memory is the running value the procedure updates as it walks the input.");
    }

    private static Grade gradeStop(String answer, String lab) {
        String result = isLargest(lab) ? "the largest number"
                : isSmallest(lab) ? "the smallest number"
                : isVowels(lab) ? "the vowel count"
                : "the result";
        return new Grade(
                has(answer, "\\b(end|last|no more|finish|done|return|give back)\\b"),
                "When there are no more items, give back " + result + ".",
                "The walk is finished after the last item, and that is when the remembered result is returned.");
    }

    private static boolean isLargest(String lab) {
        return has(lab, "\\b(largest|biggest|maximum|max)\\b");
    }

    private static boolean isSmallest(String lab) {
        return has(lab, "\\b(smallest|minimum|min)\\b");
    }

    private static boolean isVowels(String lab) {
        return has(lab, "vowel");
    }

    private static boolean isArea(String lab) {
        return has(lab, "\\barea\\b") || has(lab, "\\brectangle\\b");
    }

    private static boolean has(String text, String regex) {
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(text == null ? "" : text).find();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
