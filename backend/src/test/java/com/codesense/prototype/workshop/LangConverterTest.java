package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class LangConverterTest {

    @Test
    void javaLargestKeepsTheSameAnswer() {
        var python = PythonConverter.from(
                "Find the largest number in a list.",
                List.of(
                        new PseudoLine("a", "INPUT a list of numbers", 0),
                        new PseudoLine("b", "SET max to the first number", 0),
                        new PseudoLine("c", "FOR each number in the list", 0),
                        new PseudoLine("d", "IF this number is greater than max, SET max to this number", 1),
                        new PseudoLine("e", "UNTIL there are no more items", 0),
                        new PseudoLine("f", "OUTPUT the largest number", 0)),
                "3, 8, 2");
        var java = LangConverter.localize(python, "java");
        assertEquals("java", LangConverter.normalize("Java"));
        assertTrue(java.source().contains("public class Lab"));
        assertTrue(java.source().contains("System.out.println"));
        assertLoopComments(java.source());
        assertEquals("8", java.expectedOutput());
        var result = CodeRunner.run("java", java.source(), "");
        if ("java".equals(result.tool()) || "javac".equals(result.tool())) {
            assertEquals(0, result.exitCode(), result.stderr());
            assertTrue(result.stdout().trim().contains("8"));
        }
    }

    @Test
    void cSquareKeepsTheSameAnswer() {
        var python = PythonConverter.from(
                "Compute the area of a square given side.",
                List.of(
                        new PseudoLine("in", "INPUT the side", 0),
                        new PseudoLine("once", "COMPUTE the result in one step", 0),
                        new PseudoLine("out", "OUTPUT the area", 0)),
                "5");
        var c = LangConverter.localize(python, "c");
        assertTrue(c.source().contains("#include <stdio.h>"));
        assertTrue(c.source().contains("int side = 5"));
        assertCalcComments(c.source());
        assertEquals("25", c.expectedOutput());
        var result = CodeRunner.run("c", c.source(), "");
        assertEquals("jscpp", result.tool());
    }

    @Test
    void unknownLanguageStaysPython() {
        assertEquals("python", LangConverter.normalize("ruby"));
        assertEquals("Python", LangConverter.displayName("python"));
        assertEquals("Java", LangConverter.displayName("java"));
        assertEquals("C", LangConverter.displayName("c"));
    }

    @Test
    void javaAndCTemplatesUsePythonStyleBlockComments() {
        var largest = PythonConverter.from(
                "Find the largest number in a list.",
                List.of(
                        new PseudoLine("a", "INPUT a list of numbers", 0),
                        new PseudoLine("b", "SET max to the first number", 0),
                        new PseudoLine("c", "FOR each number in the list", 0),
                        new PseudoLine("d", "IF this number is greater than max, SET max to this number", 1),
                        new PseudoLine("e", "UNTIL there are no more items", 0),
                        new PseudoLine("f", "OUTPUT the largest number", 0)),
                "3, 8, 2");
        var smallest = PythonConverter.from(
                "Find the smallest number in a list.",
                List.of(
                        new PseudoLine("a", "INPUT a list of numbers", 0),
                        new PseudoLine("b", "SET min to the first number", 0),
                        new PseudoLine("c", "FOR each number in the list", 0),
                        new PseudoLine("d", "IF this number is smaller than min, SET min to this number", 1),
                        new PseudoLine("e", "UNTIL there are no more items", 0),
                        new PseudoLine("f", "OUTPUT the smallest number", 0)),
                "3, 8, 2");
        var vowels = PythonConverter.from(
                "Count the vowels in a string.",
                List.of(
                        new PseudoLine("in", "INPUT a string", 0),
                        new PseudoLine("set", "SET count to the first character", 0),
                        new PseudoLine("for", "FOR each character in the list", 0),
                        new PseudoLine("iff", "IF the letter is a vowel", 1),
                        new PseudoLine("out", "OUTPUT the number of vowels", 0)),
                "hello");
        var countEven = PythonConverter.from(
                "Count how many even numbers are in an array.",
                List.of(
                        new PseudoLine("in", "INPUT a list of numbers", 0),
                        new PseudoLine("set", "SET count to 0", 0),
                        new PseudoLine("for", "FOR each number in the list", 0),
                        new PseudoLine("iff", "IF this number is even", 1),
                        new PseudoLine("out", "OUTPUT the count", 0)),
                "1, 2, 3, 4");
        var square = PythonConverter.from(
                "Compute the area of a square given side.",
                List.of(
                        new PseudoLine("in", "INPUT the side", 0),
                        new PseudoLine("once", "COMPUTE the result in one step", 0),
                        new PseudoLine("out", "OUTPUT the area", 0)),
                "5");
        var rectangle = PythonConverter.from(
                "Compute the area of a rectangle given length and width.",
                List.of(
                        new PseudoLine("in", "INPUT the length and the width", 0),
                        new PseudoLine("once", "COMPUTE the result in one step", 0),
                        new PseudoLine("out", "OUTPUT the area", 0)),
                "5 and 7");
        var product = PythonConverter.from(
                "Multiply two integers and print the product.",
                List.of(
                        new PseudoLine("in", "INPUT two numbers", 0),
                        new PseudoLine("once", "COMPUTE the result in one step", 0),
                        new PseudoLine("out", "OUTPUT the product", 0)),
                "4 and 6");

        assertLoopComments(LangConverter.localize(largest, "java").source());
        assertLoopComments(LangConverter.localize(largest, "c").source());
        assertLoopComments(LangConverter.localize(smallest, "java").source());
        assertLoopComments(LangConverter.localize(smallest, "c").source());
        assertLoopComments(LangConverter.localize(vowels, "java").source());
        assertLoopComments(LangConverter.localize(vowels, "c").source());
        assertLoopComments(LangConverter.localize(countEven, "java").source());
        assertLoopComments(LangConverter.localize(countEven, "c").source());
        assertCalcComments(LangConverter.localize(square, "java").source());
        assertCalcComments(LangConverter.localize(square, "c").source());
        assertCalcComments(LangConverter.localize(rectangle, "java").source());
        assertCalcComments(LangConverter.localize(rectangle, "c").source());
        assertCalcComments(LangConverter.localize(product, "java").source());
        assertCalcComments(LangConverter.localize(product, "c").source());

        String cVowels = LangConverter.localize(vowels, "c").source();
        assertTrue(!cVowels.contains("string.h"));
        assertTrue(!cVowels.contains("strchr"));
        assertTrue(!cVowels.contains("# What you are given"));
        assertTrue(cVowels.contains("// Yes or no"));
        assertTrue(LangConverter.localize(largest, "java").source().contains("            // Yes or no"));
    }

    private static void assertLoopComments(String source) {
        assertTrue(source.contains("// What you are given"));
        assertTrue(source.contains("// Remember"));
        assertTrue(source.contains("// Look at each"));
        assertTrue(source.contains("// Yes or no"));
        assertTrue(source.contains("// Show the answer"));
        assertTrue(!source.contains("# What you are given"));
        assertTrue(!source.contains("# Remember"));
        assertTrue(!source.contains("# Look at each"));
        assertTrue(!source.contains("# Yes or no"));
        assertTrue(!source.contains("# Show the answer"));
    }

    private static void assertCalcComments(String source) {
        assertTrue(source.contains("// What you are given"));
        assertTrue(source.contains("// One calculation"));
        assertTrue(source.contains("// Show the answer"));
        assertTrue(!source.contains("# What you are given"));
        assertTrue(!source.contains("# One calculation"));
        assertTrue(!source.contains("# Show the answer"));
    }
}
