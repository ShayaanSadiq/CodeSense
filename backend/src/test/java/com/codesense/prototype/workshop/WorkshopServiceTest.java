package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.llm.LlmCompleter;
import com.codesense.prototype.model.LlmMode;
import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.model.Slot;
import com.codesense.prototype.model.WorkshopConstants;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkshopServiceTest {

    @Test
    void unknownSessionIsNotFound() {
        WorkshopService workshop = new WorkshopService();
        WorkshopException error = assertThrows(WorkshopException.class, () -> workshop.answer("missing", "x", false));
        assertEquals(404, error.getStatus());
        assertEquals("Session not found", error.getMessage());
    }

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
    void translateFlowchartMarksPuzzleKindAndShapes() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.translate(
                "Compute the area of a rectangle given length and width.",
                List.of(
                        new Translator.Mark("given", "INPUT"),
                        new Translator.Mark("once", "COMPUTE"),
                        new Translator.Mark("area", "OUTPUT")),
                "flow");
        assertEquals("flow", session.getPuzzleKind());
        assertEquals(Phase.ARRANGE, session.getPhase());
        assertTrue(session.getFlowchart().nodes().stream().anyMatch(node -> "io".equals(node.shape())));
        assertTrue(session.getFlowchart().nodes().stream().anyMatch(node -> "box".equals(node.shape())));
        assertTrue(session.getFlowchart().nodes().stream().noneMatch(node -> "diamond".equals(node.shape())));
    }

    @Test
    void startsWithAHandSolveQuestionAndALabBrief() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.start("Find the largest number in a list.");
        assertEquals(Phase.ASK, session.getPhase());
        assertEquals("path", session.getPuzzleKind());
        assertEquals(1, session.getSocraticStage());
        assertEquals(Slot.INPUT, session.getSlot());
        assertEquals("mcq", session.getAskKind());
        assertEquals(4, session.getChoices().size());
        assertTrue(session.getCurrentQuestion().toLowerCase().contains("by hand"));
        assertNotNull(session.getLabBrief());
        assertTrue(session.getLabBrief().getInput().toLowerCase().contains("array")
                || session.getLabBrief().getInput().toLowerCase().contains("list"));
        assertFalse(session.getLabBrief().getTests().isEmpty());
    }

    @Test
    void firstMcqThenAsksWhyInTheStudentsWords() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        assertEquals("mcq", started.getAskKind());
        Session picked = workshop.answer(started.getId(), started.getMcqKey(), false);
        assertEquals(1, picked.getSocraticStage());
        assertEquals("text", picked.getAskKind());
        assertTrue(picked.isWhyFollowUp());
        assertTrue(picked.getCurrentQuestion().toLowerCase().contains("why"));
        Session thin = workshop.answer(started.getId(), "Because B is the correct answer.", false);
        assertEquals(1, thin.getSocraticStage());
        assertTrue(thin.getHelpText().toLowerCase().contains("letter"));
        Session filler = workshop.answer(started.getId(), "it makes sense", false);
        assertEquals(1, filler.getSocraticStage());
        assertTrue(filler.isWhyFollowUp());
        Session why = workshop.answer(started.getId(),
                "I keep one number as the largest and replace it when I find a bigger one.", false);
        assertEquals(2, why.getSocraticStage());
    }

    @Test
    void misconceptionAfterMcqGetsAConcreteProbe() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        workshop.answer(started.getId(), started.getMcqKey(), false);
        Session probe = workshop.answer(started.getId(),
                "I'll compare every number with the previous number.", false);
        assertEquals(1, probe.getSocraticStage());
        assertEquals("text", probe.getAskKind());
        assertTrue(probe.getCurrentQuestion().toLowerCase().contains("array")
                || probe.getCurrentQuestion().contains("["));
        assertTrue(probe.getCurrentQuestion().toLowerCase().contains("previous")
                || probe.getCurrentQuestion().toLowerCase().contains("so far"));
        Session fixed = workshop.answer(started.getId(),
                "I should keep track of the largest value I have seen so far.", false);
        assertEquals(2, fixed.getSocraticStage());
    }

    @Test
    void concreteNumberWhyIsAccepted() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        Session picked = workshop.answer(started.getId(), started.getMcqKey(), false);
        assertTrue(picked.isWhyFollowUp());
        Session after = workshop.answer(started.getId(), "because 10 is bigger than 5", false);
        assertFalse(after.getHelpText().toLowerCase().contains("naming the letter"));
        assertFalse(after.isWhyFollowUp());
        assertTrue(after.getSocraticStage() >= 2);
    }

    @Test
    void storeFirstMcqUnlocksSetLine() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "I look through the list by hand and keep the biggest.");
        walk(workshop, started.getId(), "Look at every number. Scan once.");
        Session atStore = workshop.get(started.getId());
        assertEquals(3, atStore.getSocraticStage());
        Session after = "mcq".equals(atStore.getAskKind()) && !atStore.getChoices().isEmpty()
                ? workshop.answer(started.getId(), atStore.getMcqKey(), false)
                : workshop.answer(started.getId(),
                        "Store the first item from the input, not always zero.", false);
        var texts = after.getConfirmedLines().stream().map(line -> line.getText().toLowerCase()).toList();
        assertTrue(texts.stream().anyMatch(text -> text.contains("first") && text.startsWith("set")),
                texts.toString());
    }

    @Test
    void negativeNumbersWhyIsAcceptedOnStoreFirst() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "I look through the list by hand and keep the biggest.");
        walk(workshop, started.getId(), "Look at every number. Scan once.");
        Session atStore = workshop.get(started.getId());
        if ("mcq".equals(atStore.getAskKind()) && !atStore.getChoices().isEmpty()) {
            workshop.answer(started.getId(), atStore.getMcqKey(), false);
        }
        Session after = workshop.answer(started.getId(), "because negative numbers are possible", false);
        assertFalse(after.getHelpText().toLowerCase().contains("which one you keep"));
        assertFalse(after.getHelpText().toLowerCase().contains("already on the card"));
        assertTrue(after.getSocraticStage() >= 4, "stage=" + after.getSocraticStage());
    }

    @Test
    void storeFirstHelpDoesNotRepeatThreeVsEight() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "I look through the list by hand and keep the biggest.");
        walk(workshop, started.getId(), "Look at every number. Scan once.");
        Session atStore = workshop.get(started.getId());
        if ("mcq".equals(atStore.getAskKind()) && !atStore.getChoices().isEmpty()) {
            workshop.answer(started.getId(), atStore.getMcqKey(), false);
        }
        Session stuck = workshop.answer(started.getId(), "I don't know", false);
        assertFalse(Checker.alreadyAsked(stuck.getCurrentQuestion(), List.of(
                "You chose to look one at a time. After you see 3, then 8, which number would you keep in mind, and why that one?")),
                stuck.getCurrentQuestion());
        assertFalse(stuck.getCurrentQuestion().toLowerCase().contains("3 and then 8")
                && stuck.getCurrentQuestion().toLowerCase().contains("remember"),
                stuck.getCurrentQuestion());
    }

    @Test
    void dontKnowOnWhyDoesNotRepeatTheMcq() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        String mcq = started.getCurrentQuestion();
        Session picked = workshop.answer(started.getId(), started.getMcqKey(), false);
        assertTrue(picked.isWhyFollowUp());
        assertEquals("text", picked.getAskKind());
        Session stuck = workshop.answer(started.getId(), "I don't know", false);
        assertEquals("hint", stuck.getVerdict());
        assertEquals("text", stuck.getAskKind());
        assertTrue(stuck.getChoices() == null || stuck.getChoices().isEmpty());
        assertFalse(Checker.sameQuestion(mcq, stuck.getCurrentQuestion()));
        assertTrue(stuck.isWhyFollowUp());
    }

    @Test
    void whyHelpKeepsTheQuestionAndGivesARealHint() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        Session afterMcq = workshop.answer(started.getId(), started.getMcqKey(), false);
        String why = afterMcq.getCurrentQuestion();
        Session hint1 = workshop.answer(started.getId(), "I don't know", false);
        String q1 = hint1.getCurrentQuestion();
        String h1 = hint1.getHelpText();
        assertEquals(1, hint1.getHelpLevel());
        assertTrue(h1.length() > 40);
        assertFalse(Socratic.thinSlogan(h1));
        assertTrue(Checker.sameQuestion(why, q1), "q=" + q1);
        Session hint2 = workshop.answer(started.getId(), "I don't know", false);
        String q2 = hint2.getCurrentQuestion();
        String h2 = hint2.getHelpText();
        assertEquals(2, hint2.getHelpLevel());
        assertTrue(h2.length() > 40);
        assertFalse(Socratic.thinSlogan(h2));
        assertFalse(h2.toLowerCase().startsWith("let's make"));
        assertTrue(Checker.sameQuestion(why, q2));
        assertNotEquals(h1, h2);
        Session hint3 = workshop.answer(started.getId(), "I don't know", false);
        String q3 = hint3.getCurrentQuestion();
        String h3 = hint3.getHelpText();
        assertEquals(3, hint3.getHelpLevel());
        assertTrue(h3.length() > 40);
        assertFalse(Socratic.thinSlogan(h3));
        assertTrue(Checker.sameQuestion(why, q3));
        assertNotEquals(h2, h3);
    }

    @Test
    void doesNotInventLinesOnDontKnow() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.start("Find the largest number in a list.");
        Session after = workshop.answer(session.getId(), "I don't know", false);
        assertEquals(Phase.ASK, after.getPhase());
        assertEquals(1, after.getSocraticStage());
        assertEquals(1, after.getHelpLevel());
        assertEquals("hint", after.getVerdict());
        assertTrue(after.getProposedLines().isEmpty());
        assertTrue(after.getConfirmedLines().isEmpty());
        assertTrue(after.getHelpText().toLowerCase().contains("hand")
                || after.getHelpText().toLowerCase().contains("remember"));
        assertFalse(after.getHelpText().toLowerCase().contains("the answer is"));
        assertTrue(after.getCurrentQuestion().toLowerCase().contains("by hand"));
    }

    @Test
    void buildsLargestNumberPlanFromAnswersOnly() {
        WorkshopService workshop = new WorkshopService();
        Session done = talk(workshop, "Find the largest number in a list.");
        assertTrue(workshop.get(done.getId()).getTurns().get(0).isPassed());
        assertEquals(Phase.ARRANGE, done.getPhase());
        assertEquals("path", done.getPuzzleKind());
        var texts = done.getConfirmedLines().stream().map(line -> line.getText()).toList();
        assertTrue(texts.get(0).startsWith("INPUT"));
        assertTrue(texts.get(0).toLowerCase().contains("list"));
        assertTrue(texts.get(texts.size() - 1).startsWith("OUTPUT"));
        assertTrue(texts.get(texts.size() - 1).toLowerCase().contains("largest")
                || texts.get(texts.size() - 1).toLowerCase().contains("biggest"));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("FOR each")));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("SET max")));
        assertTrue(texts.contains("SET max to the first number"));
        assertTrue(texts.contains("IF this number is greater than max"));
        assertTrue(texts.contains("SET max to this number"));
        assertTrue(texts.indexOf("SET max to the first number") < texts.indexOf("FOR each number in the list"));
        assertTrue(texts.indexOf("FOR each number in the list") < texts.indexOf("IF this number is greater than max"));
        assertTrue(texts.indexOf("IF this number is greater than max") < texts.indexOf("SET max to this number"));
        var update = done.getConfirmedLines().stream()
                .filter(line -> "SET max to this number".equals(line.getText()))
                .findFirst()
                .orElseThrow();
        assertEquals(2, update.getIndent());
        assertTrue(texts.stream().noneMatch(text -> text.startsWith("RETURN")));
    }

    @Test
    void skipsMemoryAndStopAfterComputeOnce() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Compute the area of a rectangle given length and width.");
        walk(workshop, started.getId(), "Length and width come in.");
        Session afterMethod = workshop.get(started.getId());
        assertEquals(Phase.ASK, afterMethod.getPhase());
        assertEquals(2, afterMethod.getSocraticStage());
        walk(workshop, started.getId(), "Compute once. Just multiply.");
        Session afterApproach = workshop.get(started.getId());
        assertEquals(Phase.ASK, afterApproach.getPhase());
        assertEquals(6, afterApproach.getSocraticStage());
        walk(workshop, started.getId(), "INPUT the length and width. COMPUTE once. OUTPUT the area.");
        Session afterParts = workshop.get(started.getId());
        assertEquals(Phase.ASK, afterParts.getPhase());
        assertEquals(7, afterParts.getSocraticStage());
        walk(workshop, started.getId(), "4 times 6 is 24.");
        Session session = workshop.get(started.getId());
        assertEquals(Phase.ARRANGE, session.getPhase());
        assertEquals(Boolean.FALSE, session.getHasRepetition());
        assertFalse(session.getConfirmedLines().stream().anyMatch(line -> line.getText().startsWith("SET")));
    }

    @Test
    void computeOnceStageSevenDropsALeakedListWalkQuestion() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("SOCRATIC_NEXT") || system.contains("Grade whether")) {
                return "{\"kind\":\"text\",\"question\":\"After 3 then 8 then 2, what number are you holding? Then 10? Then 5?\",\"choices\":[],\"correct\":\"\",\"whyQuestion\":\"\",\"nextStage\":7,\"correct\":true,\"skipAhead\":false,\"expected\":\"24\",\"explanation\":\"ok\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Compute the area of a rectangle given length and width.");
        walk(workshop, started.getId(), "Length and width come in.");
        walk(workshop, started.getId(), "Compute once. Just multiply.");
        walk(workshop, started.getId(), "INPUT the length and width. COMPUTE once. OUTPUT the area.");
        Session atSeven = workshop.get(started.getId());
        assertEquals(7, atSeven.getSocraticStage());
        String q = atSeven.getCurrentQuestion().toLowerCase();
        assertFalse(q.contains("holding"), atSeven.getCurrentQuestion());
        assertFalse(q.contains("then 8"), atSeven.getCurrentQuestion());
        assertTrue(q.contains("multiply") || q.contains("times") || q.contains("4") || q.contains("6"),
                atSeven.getCurrentQuestion());
    }

    @Test
    void skipAheadFromStageOneDoesNotJumpToParsons() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("Grade whether")) {
                return "{\"correct\":true,\"skipAhead\":true,\"nextStage\":8,"
                        + "\"expected\":\"scan once\",\"explanation\":\"covers later stages.\","
                        + "\"question\":\"\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(),
                "I look through the list by hand and keep the biggest number I have seen.");
        Session afterMethod = workshop.get(started.getId());
        assertEquals(Phase.ASK, afterMethod.getPhase());
        assertEquals(2, afterMethod.getSocraticStage());
        assertTrue(afterMethod.getConfirmedLines().stream().anyMatch(line -> line.getText().startsWith("INPUT")));
        assertTrue(afterMethod.getConfirmedLines().stream().noneMatch(line -> line.getText().equals("SET max to this number")));
        Session afterApproach = workshop.answer(
                started.getId(),
                "Scan once. Store the first as max. Replace when bigger. Stop at the last. Output the max.",
                false);
        assertEquals(Phase.ASK, afterApproach.getPhase());
        assertEquals(3, afterApproach.getSocraticStage());
        assertNotEquals(Phase.ARRANGE, afterApproach.getPhase());
    }

    @Test
    void socraticVisitsNameAndTraceBeforeParsons() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        boolean sawSix = false;
        boolean sawSeven = false;
        int previous = 0;
        for (int n = 0; n < 24; n++) {
            Session current = workshop.get(started.getId());
            if (current.getPhase() != Phase.ASK) {
                break;
            }
            int stage = current.getSocraticStage();
            if (stage == 6) {
                sawSix = true;
            }
            if (stage == 7) {
                sawSeven = true;
            }
            if (previous > 0) {
                assertTrue(stage >= previous && stage <= previous + 1,
                        "jumped from stage " + previous + " to " + stage);
            }
            previous = stage;
            if (current.isWhyFollowUp()) {
                workshop.answer(started.getId(),
                        "I chose that because I look at each value one at a time and keep the largest I have seen so far.",
                        false);
                continue;
            }
            if ("mcq".equals(current.getAskKind()) && !current.getChoices().isEmpty()) {
                String key = current.getMcqKey();
                workshop.answer(started.getId(), key == null || key.isBlank() ? "B" : key, false);
                continue;
            }
            workshop.answer(started.getId(), talkAnswer(current.getSocraticStage(), false, Socratic.LabKind.BEST), false);
        }
        Session done = workshop.get(started.getId());
        assertTrue(sawSix);
        assertTrue(sawSeven);
        assertEquals(Phase.ARRANGE, done.getPhase());
    }

    @Test
    void keepsFixedIoQuestionWhenStudentDoesNotKnow() {
        LlmCompleter completer = (system, user) ->
                "{\"question\":\"Should you SET max to the first number?\"}";
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Smallest in a list");
        Session after = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(1, after.getSocraticStage());
        assertFalse(after.getCurrentQuestion().toUpperCase().contains("SET"));
        assertTrue(after.getProposedLines().isEmpty());
        assertTrue(after.isUsedFallbackQuestion());
    }

    @Test
    void doesNotRepeatTheSameHelpQuestionLater() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        workshop.answer(started.getId(), started.getMcqKey(), false);
        workshop.answer(started.getId(), "I don't know", false);
        Session small = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(2, small.getHelpLevel());
        String firstHelp = small.getCurrentQuestion();
        workshop.answer(started.getId(), "8", false);
        Session afterFollow = workshop.answer(started.getId(), "because 8 is bigger", false);
        assertTrue(afterFollow.getSocraticStage() >= 2);
        workshop.answer(started.getId(), "I don't know", false);
        Session laterSmall = workshop.answer(started.getId(), "I don't know", false);
        assertFalse(Checker.sameQuestion(firstHelp, laterSmall.getCurrentQuestion()),
                "first=" + firstHelp + " later=" + laterSmall.getCurrentQuestion());
    }

    @Test
    void secondDontKnowKeepsTheSameIoQuestion() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Smallest in a list");
        Session first = workshop.answer(started.getId(), "I don't know", false);
        String firstQuestion = first.getCurrentQuestion();
        String firstHint = first.getHelpText();
        Session second = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(1, second.getSocraticStage());
        assertEquals(2, second.getHelpLevel());
        assertEquals(firstQuestion, second.getCurrentQuestion());
        assertNotEquals(firstHint, second.getHelpText());
        assertTrue(second.getHelpText().length() > 40);
    }

    @Test
    void usesModelForSuccessiveIoQuestions() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("LAB_BRIEF")) {
                return "{\"input\":\"A list of numbers.\",\"output\":\"The smallest number.\",\"constraints\":\"At least one number.\",\"sample\":\"[3, 8, 2]\",\"passedTests\":[\"[3, 8, 2] → 2\"],\"failedTests\":[\"Starting at 0.\"]}";
            }
            if (system.contains("SOCRATIC_HELP") && user.contains("Help level: 1")) {
                return "{\"level\":1,\"hint\":\"Think about solving this by hand, one value at a time.\",\"question\":\"Suppose the input is [3, 8, 2]. How would you identify the answer if you were solving it by hand?\",\"followUp\":\"\",\"expected\":\"\"}";
            }
            if (system.contains("SOCRATIC_HELP")) {
                return "{\"level\":2,\"hint\":\"Keep whichever number is bigger. You saw 3 first, then 8, so you would keep 8 because it is larger.\",\"question\":\"Suppose the input is [3, 8, 2]. How would you identify the answer if you were solving it by hand?\",\"followUp\":\"Why 8?\",\"expected\":\"8\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Smallest in a list");
        Session first = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(1, first.getHelpLevel());
        assertEquals("Think about solving this by hand, one value at a time.", first.getHelpText());
        assertEquals(started.getCurrentQuestion(), first.getCurrentQuestion());
        String firstHint = first.getHelpText();
        Session second = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(started.getCurrentQuestion(), second.getCurrentQuestion());
        assertEquals(2, second.getHelpLevel());
        assertNotEquals(firstHint, second.getHelpText());
        assertEquals(LlmMode.OLLAMA, second.getLlmMode());
    }

    @Test
    void usesModelForSocraticHelpAfterIo() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("SOCRATIC_HELP")) {
                String original = user.contains("Original question:\n")
                        ? user.substring(user.indexOf("Original question:\n") + "Original question:\n".length())
                                .split("\n\n", 2)[0].trim()
                        : "How would you do it by hand?";
                return "{\"level\":1,\"hint\":\"You do not have to match every item with every other item. Is one walk through the list enough if you keep one remembered value?\",\"question\":\""
                        + original.replace("\"", "\\\"")
                        + "\",\"followUp\":\"\",\"expected\":\"\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "A list comes in.");
        Session afterWalk = workshop.get(started.getId());
        Session after = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(afterWalk.getCurrentQuestion(), after.getCurrentQuestion());
        assertTrue(after.getHelpText().toLowerCase().contains("pair")
                || after.getHelpText().toLowerCase().contains("walk")
                || after.getHelpText().toLowerCase().contains("once"),
                after.getHelpText());
        assertFalse(after.getHelpText().toLowerCase().contains("tallest"));
        assertTrue(after.getProposedLines().isEmpty());
        assertEquals(LlmMode.OLLAMA, after.getLlmMode());
    }

    @Test
    void rejectsBareLlmIoAndWritesInputOutput() {
        LlmCompleter completer = (system, user) -> "{\"lines\":[\"RETURN\"]}";
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session after = talk(workshop, "Smallest in a list");
        var texts = after.getConfirmedLines().stream().map(line -> line.getText()).toList();
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("INPUT")));
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("OUTPUT")));
        assertFalse(texts.contains("RETURN"));
    }

    @Test
    void finishedPlanComesFromTheHiddenReferenceNotTheModelDump() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("complete short")) {
                return """
                        {"lines":[
                          "SET max to this number",
                          "SET max to the first number",
                          "RETURN min"
                        ]}""";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session done = talk(workshop, "Smallest in a list");
        assertEquals(Phase.ARRANGE, done.getPhase());
        var texts = done.getConfirmedLines().stream().map(line -> line.getText()).toList();
        assertEquals("INPUT a list of numbers", texts.get(0));
        assertEquals("OUTPUT the smallest number", texts.get(texts.size() - 1));
        assertTrue(texts.indexOf("SET min to the first number") < texts.indexOf("FOR each number in the list"));
        assertTrue(texts.indexOf("IF this number is smaller than min") < texts.indexOf("SET min to this number"));
        assertTrue(texts.stream().noneMatch(text -> text.startsWith("RETURN")));
        assertTrue(Parsons.isPermutation(done.getConfirmedLines(), done.getPuzzleLines()));
        assertTrue(done.getPuzzleLines().stream().allMatch(line -> line.getIndent() == 0));
    }

    @Test
    void rewritesRememberedNameInTheModelPlan() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("complete short")) {
                return """
                        {"lines":[
                          "INPUT a list of numbers",
                          "OUTPUT the largest number",
                          "SET the remembered name to the first number",
                          "FOR each number in the list",
                          "IF this number is larger than the remembered name, SET the remembered name to this number",
                          "UNTIL there are no more items",
                          "RETURN the largest number"
                        ]}""";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        var texts = talk(workshop, "Find the largest number in a list.").getConfirmedLines().stream()
                .map(line -> line.getText())
                .toList();
        assertTrue(texts.stream().anyMatch(text -> text.contains("SET max")));
        assertFalse(texts.stream().anyMatch(text -> text.toLowerCase().contains("remembered name")));
    }

    @Test
    void arrangeSaysIndentWhenOnlyIndentIsWrong() {
        WorkshopService workshop = new WorkshopService();
        Session puzzle = talk(workshop, "Find the largest number in a list.");
        var flat = puzzle.getConfirmedLines().stream()
                .map(line -> new com.codesense.prototype.model.PseudoLine(line.getId(), line.getText(), 0))
                .toList();
        Session checked = workshop.arrange(puzzle.getId(), flat);
        assertEquals("wrong", checked.getVerdict());
        assertTrue(checked.getFeedback().toLowerCase().contains("indent"));
        assertTrue(checked.getFeedback().toLowerCase().contains("order is right"));
        assertFalse(checked.getArrangeNotes().isEmpty());
    }

    @Test
    void usesModelToExplainWrongCard() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("TASK=JUDGE_CARD")) {
                return "{\"correct\":false,\"explanation\":\"You would not calculate this in one step.\"}";
            }
            if (system.contains("TASK=VOICE_CARDS")) {
                return "{\"voiced\":\"You are given some numbers. Show the answer.\",\"needed\":[\"INPUT\",\"SET\",\"FOR\",\"IF\",\"UNTIL\",\"OUTPUT\"]}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        var judge = workshop.judgeCard(
                "Find the largest number in a list.", "COMPUTE", "do it in one step");
        assertFalse(judge.correct());
        assertEquals("You would not calculate this in one step.", judge.explanation());
    }

    @Test
    void keepsRuleCardVerdictWhenModelFlipsIt() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("TASK=VOICE_CARDS")) {
                return "{\"voiced\":\"You are given some numbers. Look at each number. Show the answer.\",\"needed\":[\"INPUT\",\"SET\",\"FOR\",\"IF\",\"UNTIL\",\"OUTPUT\"]}";
            }
            if (system.contains("TASK=JUDGE_CARD")) {
                return "{\"correct\":true,\"explanation\":\"Finding the largest number means looking at each number. You would not work the biggest one out in one step.\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        var judge = workshop.judgeCard(
                "Find the largest number in a list.", "COMPUTE", "do it in one step");
        assertFalse(judge.correct());
        assertTrue(judge.explanation().contains("largest number"));
    }

    @Test
    void explainsWrongCardsFromTheActualLab() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("TASK=VOICE_CARDS") && user.contains("vowels")) {
                return "{\"voiced\":\"You are given some text. Look at each letter. Show the answer.\",\"needed\":[\"INPUT\",\"SET\",\"FOR\",\"IF\",\"UNTIL\",\"OUTPUT\"]}";
            }
            if (system.contains("TASK=VOICE_CARDS") && user.contains("product")) {
                return "{\"voiced\":\"You are given two numbers. Do it in one step. Show the answer.\",\"needed\":[\"INPUT\",\"COMPUTE\",\"OUTPUT\"]}";
            }
            if (system.contains("TASK=JUDGE_CARD") && user.contains("vowels")) {
                return "{\"correct\":false,\"explanation\":\"Counting vowels means looking at each letter. You would not get the count in one step.\"}";
            }
            if (system.contains("TASK=JUDGE_CARD") && user.contains("product")) {
                return "{\"correct\":false,\"explanation\":\"The product uses the two numbers already given. There is no list to look through.\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        var vowels = workshop.judgeCard("How many vowels are in a word?", "COMPUTE", "do it in one step");
        var product = workshop.judgeCard("Multiply two integers and print the product.", "FOR", "look at each one");
        assertFalse(vowels.correct());
        assertFalse(product.correct());
        assertTrue(vowels.explanation().toLowerCase().contains("vowel"));
        assertTrue(product.explanation().toLowerCase().contains("product"));
        assertTrue(!vowels.explanation().equals(product.explanation()));
    }

    @Test
    void arrangeAcceptsSetBeforeForWithIfIndented() {
        WorkshopService workshop = new WorkshopService();
        Session puzzle = talk(workshop, "Find the largest number in a list.");
        Session checked = workshop.arrange(
                puzzle.getId(), Parsons.withCanonicalIndent(puzzle.getConfirmedLines()));
        assertEquals(Phase.DONE, checked.getPhase());
        assertEquals("ok", checked.getVerdict());
    }

    @Test
    void convertsFinishedPlanToPythonAndGradesAStep() {
        WorkshopService workshop = new WorkshopService();
        Session puzzle = talk(workshop, "Find the largest number in a list.");
        Session done = workshop.arrange(
                puzzle.getId(), Parsons.withCanonicalIndent(puzzle.getConfirmedLines()));
        Session python = workshop.convertPython(done.getId(), "3, 8, 2");
        assertEquals(Phase.PYTHON, python.getPhase());
        assertTrue(python.getPythonCode().contains("print(max_value)"));
        assertEquals("8", python.getExpectedOutput());
        assertFalse(python.getPythonWalk().isEmpty());
        assertTrue(python.getPythonWalk().get(0).getBody().length() > 8);
        assertFalse(python.getPythonWalk().get(0).getCode().trim().startsWith("#"));
        assertTrue(python.getPythonWalk().stream().noneMatch(step -> step.getCode().trim().startsWith("#")));
        Session first = workshop.predictPython(python.getId(), "3, 8, 2", false);
        assertEquals("ok", first.getVerdict());
        assertEquals(1, first.getPredictIndex());
        Session miss = workshop.predictPython(first.getId(), "the sum", false);
        assertEquals("wrong", miss.getVerdict());
    }

    @Test
    void predictIdkAsksAnEasierQuestionInsteadOfSkipping() {
        WorkshopService workshop = new WorkshopService();
        Session puzzle = talk(workshop, "Find the largest number in a list.");
        Session done = workshop.arrange(
                puzzle.getId(), Parsons.withCanonicalIndent(puzzle.getConfirmedLines()));
        Session python = workshop.convertPython(done.getId(), "3, 8, 2");
        String original = python.getPythonTrace().get(0).getPrompt();
        Session idk = workshop.predictPython(python.getId(), "I don't know", true);
        assertEquals(0, idk.getPredictIndex());
        assertEquals("easier", idk.getVerdict());
        assertNotEquals(original, idk.getPythonTrace().get(0).getPrompt());
        assertEquals(1, idk.getPythonTrace().get(0).getEasierLevel());
    }

    @Test
    void predictDoesNotCallASecondIfWhenTheScriptHasOne() {
        LlmCompleter completer = (system, user) -> {
            if (system != null && system.contains("PREDICT_STEPS")) {
                return """
                        {"steps":[
                          {"id":"P-INPUT-x","question":"Look at numbers = . What list do you start with?"},
                          {"id":"P-SET-x","question":"What do you remember first?"},
                          {"id":"P-IF-x","question":"Look at the first if statement. What number is checked?"},
                          {"id":"P-IF-y","question":"Look at the second if statement. What number is checked next?"},
                          {"id":"P-OUTPUT-x","question":"What number prints?"}
                        ]}""";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session puzzle = talk(workshop, "Find the largest number in a list.");
        Session done = workshop.arrange(
                puzzle.getId(), Parsons.withCanonicalIndent(puzzle.getConfirmedLines()));
        Session python = workshop.convertPython(done.getId(), "3, 8, 2");
        var steps = python.getPythonTrace();
        assertTrue(steps.stream().noneMatch(step ->
                String.valueOf(step.getPrompt()).toLowerCase().contains("second if")));
        assertTrue(steps.stream().anyMatch(step ->
                String.valueOf(step.getPrompt()).contains("2")
                        && String.valueOf(step.getPrompt()).toLowerCase().contains("check")));
    }

    @Test
    void convertsFinishedPlanToJavaAndC() {
        WorkshopService workshop = new WorkshopService();
        Session puzzle = talk(workshop, "Find the largest number in a list.");
        Session done = workshop.arrange(
                puzzle.getId(), Parsons.withCanonicalIndent(puzzle.getConfirmedLines()));
        Session java = workshop.convertCode(done.getId(), "3, 8, 2", "java");
        assertEquals("java", java.getCodeLanguage());
        assertTrue(java.getPythonCode().contains("public class Lab"));
        assertTrue(java.getPythonCode().contains("System.out.println"));
        assertEquals("8", java.getExpectedOutput());
        assertTrue(java.getPythonWalk().stream().noneMatch(step ->
                step.getCode().trim().startsWith("//") || step.getCode().trim().startsWith("/*")));
        Session c = workshop.convertCode(done.getId(), "3, 8, 2", "c");
        assertEquals("c", c.getCodeLanguage());
        assertTrue(c.getPythonCode().contains("#include <stdio.h>"));
        assertEquals("8", c.getExpectedOutput());
        assertTrue(c.getPythonWalk().stream().anyMatch(step -> step.getCode().contains("#include")));
        assertTrue(c.getPythonWalk().stream().noneMatch(step -> step.getCode().trim().startsWith("//")));
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
        walk(workshop, started.getId(), "A list comes in. I would look through it by hand and keep the biggest.");
        Session after = workshop.get(started.getId());
        assertTrue(after.getCurrentQuestion().toLowerCase().contains("once")
                || after.getCurrentQuestion().toLowerCase().contains("scan")
                || after.getCurrentQuestion().toLowerCase().contains("pair"));
        assertEquals(2, after.getSocraticStage());
        assertTrue(after.isUsedFallbackQuestion());
    }

    @Test
    void rejectsAWrongAnswerWithoutRevealing() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        Session first = workshop.answer(started.getId(), "the sum of the numbers", false);
        assertEquals(Phase.ASK, first.getPhase());
        assertEquals("hint", first.getVerdict());
        assertEquals(1, first.getHelpLevel());
        assertEquals(1, first.getSocraticStage());
        assertNull(first.getCorrectAnswer());
        Session second = workshop.answer(started.getId(), "a string comes in", false);
        assertEquals("hint", second.getVerdict());
        assertEquals(2, second.getHelpLevel());
        Session third = workshop.answer(started.getId(), "print hello", false);
        assertEquals(Phase.ASK, third.getPhase());
        assertEquals(Slot.INPUT, third.getSlot());
        assertEquals(1, third.getSocraticStage());
        assertEquals(3, third.getHelpLevel());
        assertTrue(third.getTurns().stream().noneMatch(turn -> turn.isRevealed()));
        assertNull(third.getCorrectAnswer());
        assertTrue(third.getConfirmedLines().isEmpty());
    }

    @Test
    void rejectsAnswerDumpingHelpFromTheModel() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("SOCRATIC_HELP")) {
                if (user.contains("Help level: 2")) {
                    return "{\"level\":2,\"hint\":\"8\",\"question\":\"Suppose the input is [3, 8, 2, 10, 5]. How would you identify the answer if you were solving it by hand?\",\"followUp\":\"\",\"expected\":\"8\"}";
                }
                return "{\"level\":1,\"hint\":\"Suppose the input is [3, 8, 2, 10, 5]. How would you identify the answer if you were solving it by hand?\",\"question\":\"Suppose the input is [3, 8, 2, 10, 5]. How would you identify the answer if you were solving it by hand?\",\"followUp\":\"\",\"expected\":\"\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        Session hint = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(1, hint.getHelpLevel());
        assertTrue(hint.getHelpText().toLowerCase().contains("hand"));
        assertFalse(hint.getHelpText().equals(hint.getCurrentQuestion()));
        Session smaller = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(2, smaller.getHelpLevel());
        assertEquals(started.getCurrentQuestion(), smaller.getCurrentQuestion());
        assertFalse(smaller.getHelpText().equals("8"));
        assertTrue(smaller.getHelpText().length() > 8);
        assertTrue(smaller.isUsedFallbackQuestion());
    }

    @Test
    void helpLadderKeepsTheOriginalQuestion() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        String original = started.getCurrentQuestion();
        Session hint = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(1, hint.getHelpLevel());
        assertTrue(hint.getHelpText().toLowerCase().contains("hand"));
        assertEquals(original, hint.getCurrentQuestion());
        String firstHint = hint.getHelpText();
        Session smaller = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(2, smaller.getHelpLevel());
        assertEquals(original, smaller.getCurrentQuestion());
        assertNotEquals(firstHint, smaller.getHelpText());
        Session afterMcq = workshop.answer(started.getId(), started.getMcqKey(), false);
        assertTrue(afterMcq.isWhyFollowUp());
        assertEquals(1, afterMcq.getSocraticStage());
        Session still = workshop.answer(started.getId(), "print hello", false);
        assertEquals(1, still.getSocraticStage());
        assertNotEquals("revealed", still.getVerdict());
        assertNull(still.getCorrectAnswer());
    }

    @Test
    void dontKnowOnATextQuestionKeepsTheSamePrompt() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "A list comes in. I would look through it by hand and keep the biggest.");
        Session atText = workshop.get(started.getId());
        assertEquals("text", atText.getAskKind());
        String question = atText.getCurrentQuestion();
        Session first = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(question, first.getCurrentQuestion());
        assertEquals("text", first.getAskKind());
        String firstHint = first.getHelpText();
        Session second = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(question, second.getCurrentQuestion());
        assertEquals(2, second.getHelpLevel());
        assertNotEquals(firstHint, second.getHelpText());
        assertTrue(second.getHelpText().length() > 40);
    }

    @Test
    void rejectsAProcessQuestionOnTheIoSlot() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("SOCRATIC_HELP")) {
                return "{\"question\":\"Should you SET max to the first number?\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        Session after = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(Slot.INPUT, after.getSlot());
        assertEquals(1, after.getSocraticStage());
        assertFalse(after.getCurrentQuestion().toUpperCase().contains("SET"));
        assertTrue(after.isUsedFallbackQuestion());
    }

    @Test
    void keepsAnAdaptedApproachQuestion() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("SOCRATIC_NEXT")) {
                return "{\"question\":\"Should you scan the list once, or compare every pair? Why?\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "A list comes in. I would look through it by hand and keep the biggest.");
        Session after = workshop.get(started.getId());
        assertEquals("Should you scan the list once, or compare every pair? Why?", after.getCurrentQuestion());
        assertEquals(2, after.getSocraticStage());
        assertFalse(after.isUsedFallbackQuestion());
    }

    @Test
    void keepsARuleCorrectStrategyAnswerWhenTheModelSaysWrong() {
        LlmCompleter completer = (system, user) -> {
            if (system.contains("Grade whether")) {
                return "{\"correct\":false,\"expected\":\"Look at every item.\",\"explanation\":\"The walk visits every number.\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, LlmMode.OLLAMA);
        Session started = workshop.start("Find the largest number in a list.");
        walk(workshop, started.getId(), "A list comes in.");
        Session after = workshop.answer(
                started.getId(),
                "yes they check every number in the list to find the biggest one",
                false);
        assertEquals(Phase.ASK, after.getPhase());
        assertEquals(Slot.MEMORY, after.getSlot());
        assertFalse("wrong".equals(after.getVerdict()));
    }

    @Test
    void dontKnowDoesNotConsumeAWrongAttempt() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start("Find the largest number in a list.");
        Session hint = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(0, hint.getWrongAttempts());
        Session wrong = workshop.answer(started.getId(), "the sum", false);
        assertEquals(1, wrong.getWrongAttempts());
        assertEquals("hint", wrong.getVerdict());
        assertEquals(1, wrong.getSocraticStage());
        assertNull(wrong.getCorrectAnswer());
    }

    @Test
    void translatorBuildsAPathReadyPlan() {
        WorkshopService workshop = new WorkshopService();
        Session session = workshop.translate(
                "Find the largest number in a list.",
                List.of(
                        new Translator.Mark("list", "INPUT"),
                        new Translator.Mark("largest", "OUTPUT")));
        assertEquals(Phase.ARRANGE, session.getPhase());
        assertEquals("flow", session.getPuzzleKind());
        var texts = session.getConfirmedLines().stream().map(line -> line.getText()).toList();
        assertEquals("INPUT a list of numbers", texts.get(0));
        assertEquals("OUTPUT the largest number", texts.get(texts.size() - 1));
        assertTrue(Parsons.isPermutation(session.getConfirmedLines(), session.getPuzzleLines()));
        assertTrue(session.getFlowchart().nodes().stream().anyMatch(node -> "io".equals(node.shape())));
    }

    private static void walk(WorkshopService workshop, String id, String answer) {
        Session current = workshop.get(id);
        if (current != null && "mcq".equals(current.getAskKind()) && !current.getChoices().isEmpty()) {
            workshop.answer(id, current.getMcqKey(), false);
            current = workshop.get(id);
            if (current != null && current.isWhyFollowUp()) {
                String why = Socratic.whyLooksThin(answer)
                        ? "I look at each value one at a time and keep the best I have seen so far. " + answer
                        : answer;
                workshop.answer(id, why, false);
            }
            return;
        }
        workshop.answer(id, answer, false);
    }

    private static Session talk(WorkshopService workshop, String lab) {
        boolean smallest = lab.toLowerCase().contains("smallest");
        Socratic.LabKind kind = Socratic.labKind(lab);
        Session started = workshop.start(lab);
        for (int n = 0; n < 24; n++) {
            Session current = workshop.get(started.getId());
            if (current.getPhase() != Phase.ASK) {
                break;
            }
            if (current.isWhyFollowUp()) {
                workshop.answer(started.getId(), talkWhy(kind, smallest), false);
                continue;
            }
            if ("mcq".equals(current.getAskKind()) && !current.getChoices().isEmpty()) {
                String key = current.getMcqKey();
                workshop.answer(started.getId(), key == null || key.isBlank() ? "B" : key, false);
                continue;
            }
            workshop.answer(started.getId(), talkAnswer(current.getSocraticStage(), smallest, kind), false);
        }
        return workshop.get(started.getId());
    }

    private static String talkWhy(Socratic.LabKind kind, boolean smallest) {
        if (kind == Socratic.LabKind.COUNT) {
            return "I look at each letter because a vowel can be anywhere in the word, and I add one to a count that starts at 0.";
        }
        if (kind == Socratic.LabKind.ONCE) {
            return "I multiply length and width once because both measurements are already given.";
        }
        return smallest
                ? "I chose that because I look at each value one at a time and keep the smallest I have seen so far."
                : "I chose that because I look at each value one at a time and keep the largest I have seen so far.";
    }

    private static String talkAnswer(int stage, boolean smallest, Socratic.LabKind kind) {
        if (kind == Socratic.LabKind.COUNT) {
            return switch (stage) {
                case 2 -> "Look at every letter once and add one when it is a vowel.";
                case 4 -> "If this letter is a vowel, add one. If not, leave the count.";
                case 6 -> "Start count at 0. Look at each letter. Add one if vowel. Show the count.";
                case 7 -> "h 0, e 1, l 1, l 1, o 2.";
                default -> "Look through hello by hand and count the vowels.";
            };
        }
        if (kind == Socratic.LabKind.ONCE) {
            return switch (stage) {
                case 2 -> "Compute once. Just multiply.";
                case 6 -> "INPUT length and width. COMPUTE once. OUTPUT the area.";
                case 7 -> "4 times 6 is 24.";
                default -> "Use the given length and width and multiply.";
            };
        }
        String best = smallest ? "smallest" : "biggest";
        String worse = smallest ? "larger" : "smaller";
        String cmp = smallest ? "smaller" : "larger";
        String name = smallest ? "min" : "max";
        return switch (stage) {
            case 2 -> "Look at every number. Scan once.";
            case 4 -> "If this number is " + cmp + ", store it. If " + worse + ", keep the old " + name + ".";
            case 6 -> "The " + best + " number comes out. INPUT the list. SET " + name
                    + " to first. FOR each. IF " + cmp + " SET " + name + ".";
            default -> smallest
                    ? "For [-5, -2, -9] it starts at -5, then -5, then -9."
                    : "For [-5, -2, -9] it starts at -5, then -2, stays -2.";
        };
    }
}
