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
        assertFalse(Checker.questionLeaksStep("Which do you do: look at every item, or stop when you find one?"));
    }

    @Test
    void rejectsYesNoQuestions() {
        assertTrue(Checker.isYesNoQuestion("Do they check each number in the list one by one to find the biggest one?"));
        assertTrue(Checker.isYesNoQuestion("Is this a walk through everything?"));
        assertFalse(Checker.isYesNoQuestion("Which approach do you use: look at every item, or compute once?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.STRATEGY,
                "Do they check each number in the list one by one to find the biggest one?"));
    }

    @Test
    void inputQuestionMustStayOnInputOnly() {
        assertTrue(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT,
                "Name what data the program is given."));
        assertTrue(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT, "What data does the program receive?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT, "What comes in, and what must come out?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.INPUT,
                "How does the program process the input list to find and output the maximum value?"));
        assertTrue(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.OUTPUT, "What one result should it give back?"));
        assertFalse(Checker.staysOnSlot(
                com.codesense.prototype.model.Slot.OUTPUT,
                "What should the function return if the list has one element?"));
        assertTrue(Checker.alreadyAsked("What data comes in?", List.of("What data comes in?")));
        assertFalse(Checker.alreadyAsked("What data comes in?", List.of("Name the value given to the program.")));
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
