package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import com.codesense.prototype.model.WalkLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class PythonConverterTest {

    @Test
    void largestPrintsEightForSampleList() {
        var program = PythonConverter.from(
                "Find the largest number in a list.",
                List.of(
                        new PseudoLine("a", "INPUT a list of numbers", 0),
                        new PseudoLine("b", "SET max to the first number", 0),
                        new PseudoLine("c", "FOR each number in the list", 0),
                        new PseudoLine("d", "IF this number is greater than max, SET max to this number", 1),
                        new PseudoLine("e", "UNTIL there are no more items", 0),
                        new PseudoLine("f", "OUTPUT the largest number", 0)));
        assertTrue(program.source().contains("numbers = [3, 8, 2]"));
        assertTrue(program.source().contains("print(max_value)"));
        assertEquals("8", program.expectedOutput());
        assertTrue(program.steps().get(0).getPrompt().toLowerCase().contains("numbers"));
        assertEquals("8", program.steps().get(program.steps().size() - 1).getExpected());
    }

    @Test
    void areaUsesCustomLengthAndWidth() {
        var program = PythonConverter.from(
                "Compute the area of a rectangle given length and width.",
                List.of(
                        new PseudoLine("in", "INPUT the length and the width", 0),
                        new PseudoLine("once", "COMPUTE the result in one step", 0),
                        new PseudoLine("out", "OUTPUT the area", 0)),
                "5 and 7");
        assertTrue(program.source().contains("length = 5"));
        assertTrue(program.source().contains("width = 7"));
        assertEquals("35", program.expectedOutput());
        assertEquals(3, program.steps().size());
    }

    @Test
    void squareUsesOneSideNotLengthAndWidth() {
        var program = PythonConverter.from(
                "Compute the area of a square given side.",
                List.of(
                        new PseudoLine("in", "INPUT the side", 0),
                        new PseudoLine("once", "COMPUTE the result in one step", 0),
                        new PseudoLine("out", "OUTPUT the area", 0)),
                "5");
        assertTrue(program.source().contains("side = 5"));
        assertTrue(program.source().contains("area = side * side"));
        assertTrue(!program.source().contains("length"));
        assertTrue(!program.source().contains("width"));
        assertEquals("25", program.expectedOutput());
        assertEquals("5", program.sampleInput());
        assertTrue(program.steps().get(0).getPrompt().toLowerCase().contains("side"));
    }

    @Test
    void vowelsCountsHello() {
        var program = PythonConverter.from(
                "Count the vowels in a string.",
                List.of(
                        new PseudoLine("in", "INPUT a string", 0),
                        new PseudoLine("set", "SET count to the first character", 0),
                        new PseudoLine("for", "FOR each character in the list", 0),
                        new PseudoLine("iff", "IF the letter is a vowel", 1),
                        new PseudoLine("out", "OUTPUT the number of vowels", 0)),
                "hello");
        assertEquals("2", program.expectedOutput());
        assertTrue(program.source().contains("text = \"hello\""));
    }

    @Test
    void acceptsEverydayWordingForANumber() {
        assertTrue(PythonConverter.answersMatch("max is 8", "8"));
        assertTrue(PythonConverter.answersMatch("3, 8, 2", "3, 8, 2"));
        assertFalse(PythonConverter.answersMatch("the sum", "8"));
    }

    @Test
    void fallbackWalkSkipsHashCommentsAndBlankLines() {
        var program = PythonConverter.from(
                "Find the largest number in a list.",
                List.of(
                        new PseudoLine("a", "INPUT a list of numbers", 0),
                        new PseudoLine("b", "SET max to the first number", 0),
                        new PseudoLine("c", "FOR each number in the list", 0),
                        new PseudoLine("d", "IF this number is greater than max, SET max to this number", 1),
                        new PseudoLine("e", "UNTIL there are no more items", 0),
                        new PseudoLine("f", "OUTPUT the largest number", 0)));
        var walk = PythonConverter.fallbackWalk(program);
        assertFalse(walk.isEmpty());
        for (var step : walk) {
            String code = step.getCode().trim();
            assertFalse(code.isEmpty());
            assertFalse(code.startsWith("#"));
        }
        assertTrue(walk.stream().anyMatch(step -> step.getCode().contains("numbers =")));
        assertTrue(walk.stream().anyMatch(step -> step.getCode().contains("print(")));
    }

    @Test
    void fallbackWalkKeepsIncludeAndSkipsSlashComments() {
        var program = new PythonConverter.Program(
                """
                #include <stdio.h>
                int main(void) {
                    // remember the first value
                    int side = 5;
                    /* one calculation */
                    int area = side * side;
                    printf("%d\\n", area);
                    return 0;
                }
                """,
                "5",
                "25",
                List.of());
        var walk = PythonConverter.fallbackWalk(program);
        assertTrue(walk.stream().anyMatch(step -> step.getCode().startsWith("#include")));
        assertTrue(walk.stream().anyMatch(step -> step.getCode().contains("int side")));
        assertTrue(walk.stream().noneMatch(step -> step.getCode().contains("remember")));
        assertTrue(walk.stream().noneMatch(step -> step.getCode().contains("one calculation")));
        assertTrue(PythonConverter.isCommentOrBlank("# What you are given"));
        assertTrue(PythonConverter.isCommentOrBlank("// remember"));
        assertTrue(PythonConverter.isCommentOrBlank("/* note */"));
        assertTrue(PythonConverter.isCommentOrBlank("   "));
        assertFalse(PythonConverter.isCommentOrBlank("#include <stdio.h>"));
        assertFalse(PythonConverter.isCommentOrBlank("import math"));
        assertFalse(PythonConverter.isCommentOrBlank("print(area)"));
    }

    @Test
    void javaAndCWalksSkipSlashCommentsButKeepInclude() {
        var python = PythonConverter.from(
                "Find the largest number in a list.",
                List.of(
                        new PseudoLine("a", "INPUT a list of numbers", 0),
                        new PseudoLine("b", "SET max to the first number", 0),
                        new PseudoLine("c", "FOR each number in the list", 0),
                        new PseudoLine("d", "IF this number is greater than max, SET max to this number", 1),
                        new PseudoLine("e", "UNTIL there are no more items", 0),
                        new PseudoLine("f", "OUTPUT the largest number", 0)));
        var javaWalk = PythonConverter.fallbackWalk(LangConverter.localize(python, "java"));
        var cWalk = PythonConverter.fallbackWalk(LangConverter.localize(python, "c"));
        assertFalse(javaWalk.isEmpty());
        assertFalse(cWalk.isEmpty());
        for (var step : javaWalk) {
            assertFalse(step.getCode().trim().startsWith("//"));
            assertFalse(step.getCode().trim().startsWith("/*"));
        }
        for (var step : cWalk) {
            assertFalse(step.getCode().trim().startsWith("//"));
        }
        assertTrue(cWalk.stream().anyMatch(step -> step.getCode().contains("#include")));
        assertTrue(javaWalk.stream().anyMatch(step -> step.getCode().contains("int[] numbers")));
        assertTrue(javaWalk.stream().anyMatch(step -> step.getCode().contains("System.out.println")));
    }

    @Test
    void keepRunnableDropsCommentWalkLines() {
        var kept = PythonConverter.keepRunnable(List.of(
                new WalkLine("# Remember", "A note", "Does not run.", "Skip."),
                new WalkLine("max_value = numbers[0]", "Store the first", "Starts from the given list.", "Later checks need it."),
                new WalkLine("// wrapper", "A note", "Does not run.", "Skip."),
                new WalkLine("/* note */", "A note", "Does not run.", "Skip.")));
        assertEquals(1, kept.size());
        assertEquals("max_value = numbers[0]", kept.get(0).getCode());
    }

    @Test
    void fallbackWalkSkipsJavaCommentsAndKeepsStatements() {
        var program = new PythonConverter.Program(
                """
                public class Lab {
                    public static void main(String[] args) {
                        // What you are given
                        int side = 5;
                        /* One calculation */
                        int area = side * side;
                        // Show the answer
                        System.out.println(area);
                    }
                }
                """,
                "5",
                "25",
                List.of());
        var walk = PythonConverter.fallbackWalk(program);
        assertTrue(walk.stream().anyMatch(step -> step.getCode().contains("int side")));
        assertTrue(walk.stream().anyMatch(step -> step.getCode().contains("System.out.println")));
        assertTrue(walk.stream().anyMatch(step -> step.getCode().contains("public class")));
        assertTrue(walk.stream().noneMatch(step -> step.getCode().trim().startsWith("//")));
        assertTrue(walk.stream().noneMatch(step -> step.getCode().trim().startsWith("/*")));
    }
}
