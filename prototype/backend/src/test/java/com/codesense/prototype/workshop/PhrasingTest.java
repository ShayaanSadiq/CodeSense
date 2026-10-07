package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.Slot;
import java.util.List;
import org.junit.jupiter.api.Test;

class PhrasingTest {

    @Test
    void turnsInputAndOutputAnswersIntoSeparateLines() {
        assertEquals(
                List.of("INPUT a list"),
                Phrasing.phraseFromRules(
                        Slot.INPUT,
                        "A list comes in.",
                        "Find the largest number in a list.",
                        List.of()));
        assertEquals(
                List.of("OUTPUT the biggest number"),
                Phrasing.phraseFromRules(
                        Slot.OUTPUT,
                        "The biggest number comes out.",
                        "Find the largest number in a list.",
                        List.of()));
    }

    @Test
    void keepsMaxOnFollowUp() {
        assertEquals(
                List.of("SET max to the first number"),
                Phrasing.phraseFromRules(
                        Slot.MEMORY,
                        "It starts as the first number.",
                        "Find the largest number in a list.",
                        List.of("SET max to the value so far")));
    }

    @Test
    void phrasesScanAndFormula() {
        assertEquals(
                List.of("FOR each number in the input"),
                Phrasing.phraseFromRules(Slot.STRATEGY, "Look at every number.", "Find the largest number in a list.", List.of()));
        assertEquals(
                List.of("COMPUTE the result in one step"),
                Phrasing.phraseFromRules(
                        Slot.STRATEGY,
                        "Compute once. Just multiply length and width.",
                        "Compute the area of a rectangle.",
                        List.of()));
    }

    @Test
    void dropsNonPseudocodeLines() {
        assertEquals(
                List.of("INPUT a list", "RETURN max"),
                Phrasing.sanitizeLines(List.of("INPUT a list", "then you sort it", "RETURN max")));
    }

    @Test
    void dropsBareAndMashedLines() {
        assertEquals(
                List.of("INPUT a list"),
                Phrasing.sanitizeLines(List.of(
                        "INPUT a list",
                        "RETURN",
                        "UNTIL",
                        "IF list has more THEN FOR each item COMPUTE compare",
                        "SET the value so far to the value so far")));
    }

    @Test
    void rewritesRememberedNameToMax() {
        assertEquals(
                List.of(
                        "SET max to the first number",
                        "IF this number is larger than max, SET max to this number"),
                Phrasing.useConcreteNames(
                        List.of(
                                "SET the remembered name to the first number",
                                "IF this number is larger than the remembered name, SET the remembered name to this number"),
                        "Find the largest number in a list."));
        assertTrue(Phrasing.hasVagueNames(List.of("SET the remembered name to the first number")));
        assertFalse(Phrasing.usablePlan(List.of(
                "INPUT a list",
                "OUTPUT the largest number",
                "SET the remembered name to the first number")));
    }

    @Test
    void namesMinFromLabQuestion() {
        assertEquals(
                List.of("SET min to the first number"),
                Phrasing.phraseFromRules(
                        Slot.MEMORY,
                        "It starts as the first number.",
                        "Smallest in a list",
                        List.of()));
    }
}
