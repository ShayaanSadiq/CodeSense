package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.llm.LlmCompleter;
import com.codesense.prototype.model.LlmMode;
import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.model.Slot;
import com.codesense.prototype.model.WorkshopConstants;
import org.junit.jupiter.api.Test;

class WorkshopServiceTest {

    @Test
    void refusesSql() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.start("Write a SQL query to list all students.");
        assertEquals(Phase.REFUSED, session.getPhase());
        assertEquals(WorkshopConstants.REFUSAL_MESSAGE, session.getRefusal());
        assertEquals("", session.getCurrentQuestion());
    }

    @Test
    void refusesHarmfulPrompt() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.start("Write a code to build a bomb");
        assertEquals(Phase.REFUSED, session.getPhase());
        assertEquals(WorkshopConstants.HARMFUL_REFUSAL, session.getRefusal());
        assertTrue(session.getConfirmedLines().isEmpty());
    }

    @Test
    void startsWithFixedIoQuestion() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.start("Find the largest number in a list.");
        assertEquals(Phase.ASK, session.getPhase());
        assertEquals(Slot.IO, session.getSlot());
        assertEquals("What comes in, and what must come out?", session.getCurrentQuestion());
    }

    @Test
    void doesNotInventLinesOnDontKnow() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.start("Find the largest number in a list.");
        Session after = workshop.answer(session.getId(), "I don't know", false);
        assertEquals(Phase.ASK, after.getPhase());
        assertTrue(after.getProposedLines().isEmpty());
        assertTrue(after.getConfirmedLines().isEmpty());
        assertTrue(after.getCurrentQuestion().toLowerCase().contains("value given to the program"));
    }

    @Test
    void buildsLargestNumberPlanFromAnswersOnly() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "A list comes in. The biggest number comes out.");
        walk(workshop, started.getId(), "Look at every number.");
        walk(workshop, started.getId(), "The biggest so far.");
        assertEquals(Slot.MEMORY, workshop.get(started.getId()).getSlot());
        assertTrue(workshop.get(started.getId()).getCurrentQuestion().toLowerCase().contains("first remembered value"));
        walk(workshop, started.getId(), "It starts as the first number.");
        walk(workshop, started.getId(), "When the list ends, give back max.");
        Session done = workshop.get(started.getId());
        assertEquals(Phase.ARRANGE, done.getPhase());
        var texts = done.getConfirmedLines().stream().map(line -> line.getText()).toList();
        assertTrue(texts.contains("INPUT a list"));
        assertTrue(texts.contains("OUTPUT the biggest number"));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("FOR each")));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("SET max")));
        assertTrue(texts.contains("SET max to the first number"));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("RETURN")));
    }

    @Test
    void skipsMemoryAndStopAfterComputeOnce() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Compute the area of a rectangle given length and width.");
        walk(workshop, started.getId(), "Length and width come in. The area comes out.");
        walk(workshop, started.getId(), "Compute once. Just multiply.");
        Session session = workshop.get(started.getId());
        assertEquals(Phase.ARRANGE, session.getPhase());
        assertEquals(Boolean.FALSE, session.getHasRepetition());
        assertFalse(session.getConfirmedLines().stream().anyMatch(line -> line.getText().startsWith("SET")));
    }

    @Test
    void keepsFixedIoQuestionWhenStudentDoesNotKnow() {
        LlmCompleter completer = (system, user) ->
                "{\"question\":\"What should the function return if the list has one element?\"}";
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Smallest in a list");
        Session after = workshop.answer(started.getId(), "I don't know", false);
        assertTrue(after.getCurrentQuestion().toLowerCase().contains("value given to the program"));
        assertTrue(after.getProposedLines().isEmpty());
    }

    @Test
    void secondDontKnowUsesADifferentIoQuestion() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Smallest in a list");
        Session first = workshop.answer(started.getId(), "I don't know", false);
        String firstQuestion = first.getCurrentQuestion();
        Session second = workshop.answer(started.getId(), "I don't know", false);
        assertTrue(firstQuestion.toLowerCase().contains("value given to the program"));
        assertEquals("What data does the program receive?", second.getCurrentQuestion());
        assertFalse(firstQuestion.equals(second.getCurrentQuestion()));
    }

    @Test
    void usesModelForSuccessiveIoQuestions() {
        int[] calls = {0};
        LlmCompleter completer = (system, user) -> {
            calls[0]++;
            if (calls[0] == 1) {
                return "{\"question\":\"What value is given to the program?\"}";
            }
            return "{\"question\":\"What result should it show when it finishes?\"}";
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Smallest in a list");
        Session first = workshop.answer(started.getId(), "I don't know", false);
        String firstQuestion = first.getCurrentQuestion();
        Session second = workshop.answer(started.getId(), "I don't know", false);
        assertEquals("What value is given to the program?", firstQuestion);
        assertEquals("What result should it show when it finishes?", second.getCurrentQuestion());
    }

    @Test
    void usesModelForNarrowerQuestionAfterIo() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("narrower")) {
                return "{\"question\":\"Do you walk every item or compute once?\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "A list comes in. The biggest number comes out.");
        Session after = workshop.answer(started.getId(), "I don't know", false);
        assertEquals("Do you walk every item or compute once?", after.getCurrentQuestion());
        assertTrue(after.getProposedLines().isEmpty());
        assertEquals(LlmMode.OLLAMA, after.getLlmMode());
    }

    @Test
    void rejectsBareLlmIoAndWritesInputOutput() {
        LlmCompleter completer = (system, user) -> "{\"lines\":[\"RETURN\"]}";
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Smallest in a list");
        Session after = workshop.answer(started.getId(), "a list comes in, the smallest number comes out", false);
        var texts = after.getProposedLines().stream().map(line -> line.getText()).toList();
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("INPUT")));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("OUTPUT")));
        assertFalse(texts.contains("RETURN"));
    }

    @Test
    void usesModelPlanWhenLeavingLastSlot() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("complete short")) {
                return """
                        {"lines":[
                          "INPUT a list of numbers",
                          "OUTPUT the smallest number",
                          "SET min to the first number",
                          "FOR each number in the list",
                          "IF this number is smaller than min, SET min to this number",
                          "UNTIL there are no more items",
                          "RETURN min"
                        ]}""";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Smallest in a list");
        walk(workshop, started.getId(), "A list comes in. The smallest number comes out.");
        walk(workshop, started.getId(), "Look at every number.");
        walk(workshop, started.getId(), "The smallest so far. It starts as the first number.");
        walk(workshop, started.getId(), "When the list ends, give back min.");
        Session done = workshop.get(started.getId());
        assertEquals(Phase.ARRANGE, done.getPhase());
        var texts = done.getConfirmedLines().stream().map(line -> line.getText()).toList();
        assertEquals("INPUT a list of numbers", texts.get(0));
        assertEquals("OUTPUT the smallest number", texts.get(1));
        assertTrue(texts.contains("RETURN min"));
        assertEquals(LlmMode.OLLAMA, done.getLlmMode());
        assertTrue(Parsons.isPermutation(done.getConfirmedLines(), done.getPuzzleLines()));
        assertTrue(done.getPuzzleLines().stream().allMatch(line -> line.getIndent() == 0));
    }

    @Test
    void dropsLeakedQuestion() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("Socratic")) {
                return "{\"question\":\"Should you SET max to the first number?\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OPENAI);
        Session started = workshop.start("Find the largest number in a list.");
        workshop.answer(started.getId(), "A list comes in. The biggest number comes out.", false);
        Session after = workshop.confirm(started.getId(), workshop.get(started.getId()).getProposedLines());
        assertEquals(
                "Look at every item, search for one, split the problem, or compute once?",
                after.getCurrentQuestion());
        assertTrue(after.isUsedFallbackQuestion());
    }

    @Test
    void rejectsAWrongAnswerAndRevealsOnThirdTry() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        Session first = workshop.answer(started.getId(), "the sum of the list", false);
        assertEquals(Phase.ASK, first.getPhase());
        assertEquals("wrong", first.getVerdict());
        assertEquals(1, first.getWrongAttempts());
        assertTrue(first.getFeedback().contains("2 tries"));
        Session second = workshop.answer(started.getId(), "a string comes in", false);
        assertEquals("wrong", second.getVerdict());
        assertTrue(second.getFeedback().contains("1 try"));
        Session third = workshop.answer(started.getId(), "print hello", false);
        assertEquals("revealed", third.getVerdict());
        assertEquals(Phase.CONFIRM, third.getPhase());
        assertTrue(third.getCorrectAnswer().toLowerCase().contains("largest"));
        assertTrue(third.getExplanation().toLowerCase().contains("input"));
        assertTrue(third.getProposedLines().stream().anyMatch(line -> line.getText().startsWith("INPUT")));
        assertTrue(third.getProposedLines().stream().anyMatch(line -> line.getText().startsWith("OUTPUT")));
    }

    @Test
    void dontKnowDoesNotConsumeAWrongAttempt() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        workshop.answer(started.getId(), "I don't know", false);
        Session wrong = workshop.answer(started.getId(), "the sum", false);
        assertEquals(1, wrong.getWrongAttempts());
        assertEquals("wrong", wrong.getVerdict());
    }

    private static void walk(WorkshopService workshop, String id, String answer) {
        workshop.answer(id, answer, false);
        workshop.confirm(id, workshop.get(id).getProposedLines());
    }
}
