package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.WorkshopConstants;

import org.junit.jupiter.api.Test;

class SuitabilityTest {

    @Test
    void acceptsAConsoleProcedure() {
        assertTrue(Suitability.ok("Find the largest number in a list."));
        assertTrue(Suitability.ok("Count the vowels in a string."));
        assertTrue(Suitability.ok("Compute the area of a rectangle given length and width."));
    }

    @Test
    void refusesTheorySqlDesignDebugAndWeb() {
        assertFalse(Suitability.ok("Explain the difference between a stack and a heap."));
        assertFalse(Suitability.ok("Write a SQL query to list students in CS101."));
        assertFalse(Suitability.ok("Design a class diagram for a library system."));
        assertFalse(Suitability.ok("Debug this program and find the bug in the loop."));
        assertFalse(Suitability.ok("Build a webpage that shows a registration form."));
    }

    @Test
    void refusesHarmfulAndNonProcedures() {
        assertFalse(Suitability.ok("Write a code to build a bomb"));
        assertEquals(
                WorkshopConstants.HARMFUL_REFUSAL,
                Suitability.refusal("Write a code to build a bomb"));
        assertFalse(Suitability.ok("Write a code to paint a house"));
        assertEquals(
                WorkshopConstants.REFUSAL_MESSAGE,
                Suitability.refusal("Write a code to paint a house"));
        assertTrue(Suitability.ok("Write a program that reads two numbers and prints their sum."));
        assertTrue(Suitability.ok("Smallest in a list"));
    }
}
