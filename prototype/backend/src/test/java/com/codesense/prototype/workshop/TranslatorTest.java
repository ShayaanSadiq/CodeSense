package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TranslatorTest {

    @Test
    void assemblesLargestWithInputFirstAndOutputLast() {
        List<String> lines = Translator.assemble(
                "Find the largest number in a list.",
                List.of(
                        new Translator.Mark("list", "INPUT"),
                        new Translator.Mark("each", "FOR"),
                        new Translator.Mark("largest", "OUTPUT")));
        assertEquals("INPUT a list of numbers", lines.get(0));
        assertEquals("OUTPUT the largest number", lines.get(lines.size() - 1));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("FOR ")));
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("SET max")));
    }

    @Test
    void assemblesAreaWithoutALoop() {
        List<String> lines = Translator.assemble(
                "Compute the area of a rectangle given length and width.",
                List.of(
                        new Translator.Mark("length", "INPUT"),
                        new Translator.Mark("once", "COMPUTE"),
                        new Translator.Mark("area", "OUTPUT")));
        assertEquals("INPUT the length and the width", lines.get(0));
        assertEquals("OUTPUT the area", lines.get(lines.size() - 1));
        assertTrue(lines.contains("COMPUTE the result in one step"));
        assertTrue(lines.stream().noneMatch(line -> line.startsWith("FOR ")));
    }
}
