package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
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
        assertTrue(lines.stream().noneMatch(line -> line.startsWith("RETURN ")));
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

    @Test
    void glossaryUsesEverydayPhrasesNotOnlyDatatypes() {
        assertTrue(Translator.glossary().stream().anyMatch(entry -> entry.word().contains(" ")));
        assertTrue(Translator.glossary().stream().noneMatch(entry -> entry.word().equals("list")));
        assertTrue(Translator.glossary().stream().anyMatch(entry -> "INPUT".equals(entry.construct())));
        assertTrue(Translator.glossary().stream().anyMatch(entry -> "FOR".equals(entry.construct())));
        assertTrue(Translator.glossary().stream().noneMatch(entry -> "RETURN".equals(entry.construct())));
        assertEquals(1, Translator.glossary().stream().filter(entry -> "OUTPUT".equals(entry.construct())).count());
    }

    @Test
    void rephrasesLargestWithDifferentWordsThanCards() {
        String lab = "Find the largest number in a list.";
        String voiced = Translator.rephrase(lab);
        assertTrue(!Translator.copiesCardPhrase(voiced));
        assertTrue(!voiced.toLowerCase().contains("you are given"));
        assertTrue(!voiced.toLowerCase().contains("look at each"));
        assertTrue(!voiced.toLowerCase().contains("show the answer"));
        assertTrue(!voiced.toLowerCase().contains("largest number in a list"));
        var lines = Translator.ruleLines(lab);
        assertEquals(6, lines.size());
        assertEquals("INPUT", lines.get(0).card());
        assertEquals("OUTPUT", lines.get(lines.size() - 1).card());
        assertTrue(Translator.sanitizeVoiceLines(
                        lab,
                        Translator.neededConstructs(lab),
                        List.of(new Translator.VoiceLine("Look at each number in the list.", "FOR")))
                .stream()
                .noneMatch(line -> line.text().toLowerCase().contains("look at each")));
    }

    @Test
    void whyExplainsInputFirstAndOutputLast() {
        assertTrue(Translator.why("INPUT").contains("always first"));
        assertTrue(Translator.why("OUTPUT").contains("always last"));
        assertTrue(Translator.why("IF").toLowerCase().contains("under"));
    }

    @Test
    void largestNeedsQuestionCardsAndRejectsCompute() {
        String lab = "Find the largest number in a list.";
        assertTrue(Translator.neededConstructs(lab).contains("INPUT"));
        assertTrue(Translator.neededConstructs(lab).contains("OUTPUT"));
        assertTrue(!Translator.neededConstructs(lab).contains("COMPUTE"));
        assertTrue(Translator.unusedReason("COMPUTE", lab).contains("would not use"));
        assertTrue(Translator.unusedReason("COMPUTE", lab).contains("largest"));
        assertTrue(Translator.unusedReason("FOR", "Multiply two integers and print the product.")
                .contains("product"));
        var unused = Translator.unusedReasons(lab, Translator.neededConstructs(lab), Map.of());
        assertTrue(unused.containsKey("COMPUTE"));
        assertTrue(unused.get("COMPUTE").contains("largest"));
        assertTrue(!unused.containsKey("FOR"));
        assertTrue(Translator.markProblem(lab, List.of(new Translator.Mark("what you are given", "INPUT")))
                .contains("Match every sentence"));
        assertNull(Translator.markProblem(
                lab,
                Translator.neededConstructs(lab).stream()
                        .map(verb -> new Translator.Mark(verb.toLowerCase(), verb))
                        .toList()));
    }
}
