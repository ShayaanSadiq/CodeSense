package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.Slot;
import org.junit.jupiter.api.Test;

class GraderTest {

    @Test
    void acceptsACorrectInputAnswer() {
        assertTrue(Grader.fromRules(
                        Slot.INPUT,
                        "A list comes in.",
                        "Find the largest number in a list.")
                .correct());
        assertTrue(Grader.fromRules(
                        Slot.OUTPUT,
                        "The biggest number comes out.",
                        "Find the largest number in a list.")
                .correct());
    }

    @Test
    void rejectsAWrongInputOrOutputAnswer() {
        assertFalse(Grader.fromRules(Slot.INPUT, "asdfasdf", "Smallest in a list").correct());
        assertFalse(Grader.fromRules(Slot.OUTPUT, "the sum of the list", "Find the largest number in a list.")
                .correct());
    }

    @Test
    void acceptsEverydayStrategyWording() {
        assertTrue(Grader.fromRules(
                        Slot.STRATEGY,
                        "yes they check every number in the list to find the biggest one",
                        "Find the largest number in a list.")
                .correct());
    }
}
