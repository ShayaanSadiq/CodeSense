package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class CheckerTest {

    @Test
    void rejectsAQuestionThatAlreadyContainsTheStep() {
        assertTrue(Checker.questionLeaksStep("Should you SET max to the first number?"));
        assertTrue(Checker.questionLeaksStep("Do you FOR each number in the list?"));
        assertTrue(Checker.questionLeaksStep("Is this a job for binary search?"));
        assertTrue(Checker.questionLeaksStep("Do you write max = arr[0]?"));
    }

    @Test
    void allowsADecisionQuestion() {
        assertFalse(Checker.questionLeaksStep("What do you remember between steps?"));
        assertFalse(Checker.questionLeaksStep("Do you look at every item, or stop when you find one?"));
    }

    @Test
    void ioQuestionMustStayOnInputAndOutput() {
        assertTrue(Checker.staysOnSlot(com.codesense.prototype.model.Slot.IO, "What value is given to the program?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.IO,
                "What should the function return if the list has one element?"));
        assertTrue(Checker.alreadyAsked("What comes in, and what must come out?", List.of("What comes in, and what must come out?")));
        assertFalse(Checker.alreadyAsked("What data does the program receive?", List.of("What comes in, and what must come out?")));
    }

    @Test
    void treatsEmptyAndIdkAsDontKnow() {
        assertTrue(Checker.isDontKnow(""));
        assertTrue(Checker.isDontKnow("idk"));
        assertTrue(Checker.isDontKnow("I don't know"));
        assertFalse(Checker.isDontKnow("A list comes in and the largest comes out."));
    }

    @Test
    void flagsAThinMemoryAnswer() {
        assertTrue(Checker.memoryAnswerIsThin("the biggest so far"));
        assertFalse(Checker.memoryAnswerIsThin("the first number"));
    }

    @Test
    void detectRepetition() {
        assertFalse(Checker.detectRepetition("just compute once",
                List.of(new PseudoLine("1", "COMPUTE the result in one step", 0))));
        assertTrue(Checker.detectRepetition("look at every number",
                List.of(new PseudoLine("1", "FOR each number in the input", 0))));
    }
}
