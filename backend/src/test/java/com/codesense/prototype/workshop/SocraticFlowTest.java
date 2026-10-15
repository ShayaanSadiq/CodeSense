package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SocraticFlowTest {

    private static final String LARGEST = "Find the largest number in a list.";
    private static final String VOWELS = "Count the vowels in a string.";
    private static final String AREA = "Compute the area of a rectangle given length and width.";

    @Test
    void labKindsMatchTheThreeSampleLabs() {
        assertEquals(Socratic.LabKind.BEST, Socratic.labKind(LARGEST));
        assertEquals(Socratic.LabKind.COUNT, Socratic.labKind(VOWELS));
        assertEquals(Socratic.LabKind.ONCE, Socratic.labKind(AREA));
    }

    @Test
    void vowelFallbacksNeverAskLargestOrFirstVsZero() {
        var brief = Socratic.briefFromRules(VOWELS);
        for (int stage = 1; stage <= 7; stage++) {
            var ask = Socratic.fallbackAsk(stage, brief, VOWELS, Socratic.preferMcq(stage, 0, stage == 1 ? 0 : 2));
            assertNoForeignLab(ask.question(), VOWELS);
            assertTrue(Checker.questionFitsStage(ask.question(), stage), ask.question());
            if (ask.whyQuestion() != null && !ask.whyQuestion().isBlank()) {
                assertNoForeignLab(ask.whyQuestion(), VOWELS);
            }
            var help = Socratic.fallbackHelp(stage, 1, brief, ask.question(), false, VOWELS);
            assertNoForeignLab(help.hint(), VOWELS);
            assertFalse(Checker.hintCopiesQuestion(help.hint(), ask.question()), help.hint());
        }
        var store = Socratic.fallbackAsk(3, brief, VOWELS, true);
        assertTrue(store.question().toLowerCase(Locale.ROOT).contains("count"));
        assertEquals("B", store.correct());
        assertTrue(store.whyQuestion().toLowerCase(Locale.ROOT).contains("0")
                || store.whyQuestion().toLowerCase(Locale.ROOT).contains("zero"));
    }

    @Test
    void areaFallbacksNeverWalkAList() {
        var brief = Socratic.briefFromRules(AREA);
        for (int stage : List.of(1, 2, 6, 7)) {
            var ask = Socratic.fallbackAsk(stage, brief, AREA, stage == 1);
            assertNoForeignLab(ask.question(), AREA);
            var help = Socratic.fallbackHelp(stage, 1, brief, ask.question(), false, AREA);
            assertNoForeignLab(help.hint(), AREA);
            assertFalse(help.hint().toLowerCase(Locale.ROOT).contains("3 then 8"));
        }
    }

    @Test
    void largestWhyDoesNotJumpToKeepVsReplace() {
        var brief = Socratic.briefFromRules(LARGEST);
        var ask = Socratic.fallbackAsk(1, brief, LARGEST, true);
        assertFalse(ask.whyQuestion().contains("then 8") && ask.whyQuestion().toLowerCase().contains("keep"));
        assertTrue(ask.whyQuestion().toLowerCase().contains("why"));
    }

    @Test
    void threeLabsReachParsonsOnCorrectAnswers() {
        WorkshopService workshop = new WorkshopService();
        Session largest = talkLab(workshop, LARGEST);
        assertEquals(Phase.ARRANGE, largest.getPhase(),
                "largest stage=" + largest.getSocraticStage() + " q=" + largest.getCurrentQuestion());
        Session vowels = talkLab(workshop, VOWELS);
        assertEquals(Phase.ARRANGE, vowels.getPhase(),
                "vowels stage=" + vowels.getSocraticStage() + " q=" + vowels.getCurrentQuestion());
        Session area = talkLab(workshop, AREA);
        assertEquals(Phase.ARRANGE, area.getPhase(),
                "area stage=" + area.getSocraticStage() + " q=" + area.getCurrentQuestion());
    }

    @Test
    void vowelPlanUsesCountZeroNotFirstItem() {
        WorkshopService workshop = new WorkshopService();
        Session done = talkLab(workshop, VOWELS);
        var texts = done.getConfirmedLines().stream().map(line -> line.getText().toLowerCase(Locale.ROOT)).toList();
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("set count to 0")), texts.toString());
        assertTrue(texts.stream().anyMatch(text -> text.contains("vowel")), texts.toString());
        assertFalse(texts.stream().anyMatch(text -> text.contains("set max")), texts.toString());
    }

    @Test
    void idkWrongAndThinWhyStayOnStageAndDoNotLeakOldMcq() {
        assertAnswerTypes(LARGEST);
        assertAnswerTypes(VOWELS);
        assertAnswerTypes(AREA);
    }

    @Test
    void questionsOnlyMoveForward() {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start(LARGEST);
        List<Integer> stages = new ArrayList<>();
        List<String> asked = new ArrayList<>();
        for (int n = 0; n < 20; n++) {
            Session current = workshop.get(started.getId());
            if (current.getPhase() != Phase.ASK) {
                break;
            }
            stages.add(current.getSocraticStage());
            String q = current.getCurrentQuestion();
            int got = Checker.stageOfQuestion(q);
            if (got > 0) {
                assertTrue(got >= current.getSocraticStage() - (current.isWhyFollowUp() ? 0 : 0),
                        "backward q=" + q + " stage=" + current.getSocraticStage());
                assertFalse(got < current.getSocraticStage(), q);
            }
            for (String previous : asked) {
                assertFalse(Checker.similarQuestion(q, previous) && !current.isWhyFollowUp()
                                && Checker.sameQuestion(q, previous),
                        "repeat: " + q);
            }
            asked.add(q);
            if (current.isWhyFollowUp()) {
                workshop.answer(started.getId(),
                        "I look at each value one at a time and keep the largest I have seen so far.", false);
            } else if ("mcq".equals(current.getAskKind()) && current.getMcqKey() != null) {
                workshop.answer(started.getId(), current.getMcqKey(), false);
            } else {
                workshop.answer(started.getId(),
                        "Look at every number. Scan once. Replace when bigger. Stop at the last. The largest comes out.",
                        false);
            }
        }
        for (int i = 1; i < stages.size(); i++) {
            assertTrue(stages.get(i) >= stages.get(i - 1), stages.toString());
        }
    }

    @Test
    void leakedLargestQuestionIsDroppedOnVowels() {
        com.codesense.prototype.llm.LlmCompleter completer = (system, user) -> {
            if (system.contains("SOCRATIC_NEXT") || system.contains("SOCRATIC_GRADE") || system.contains("Grade whether")) {
                return "{\"kind\":\"mcq\",\"question\":\"Before you start comparing, what should you store first?\",\"choices\":[{\"letter\":\"A\",\"text\":\"The first item from the input\"},{\"letter\":\"B\",\"text\":\"Always zero\"},{\"letter\":\"C\",\"text\":\"The last item\"},{\"letter\":\"D\",\"text\":\"How many items there are\"}],\"correct\":\"A\",\"whyQuestion\":\"If the list starts with 3, why is 3 a safer start than 0?\",\"nextStage\":3,\"correct\":true,\"skipAhead\":false,\"expected\":\"0\",\"explanation\":\"ok\"}";
            }
            return null;
        };
        WorkshopService workshop = new WorkshopService(completer, com.codesense.prototype.model.LlmMode.OLLAMA);
        Session started = workshop.start(VOWELS);
        assertFalse(started.getCurrentQuestion().toLowerCase(Locale.ROOT).contains("comparing"));
        assertTrue(started.getCurrentQuestion().toLowerCase(Locale.ROOT).contains("count")
                || started.getCurrentQuestion().toLowerCase(Locale.ROOT).contains("letter")
                || started.getCurrentQuestion().toLowerCase(Locale.ROOT).contains("hand"),
                started.getCurrentQuestion());
    }

    private static void assertAnswerTypes(String lab) {
        WorkshopService workshop = new WorkshopService();
        Session started = workshop.start(lab);
        String mcq = started.getCurrentQuestion();
        Session wrong = workshop.answer(started.getId(), "C", false);
        assertEquals(1, wrong.getSocraticStage());
        assertEquals("hint", wrong.getVerdict());
        assertFalse(Checker.hintCopiesQuestion(wrong.getHelpText(), mcq), wrong.getHelpText());
        assertNoForeignLab(wrong.getHelpText(), lab);
        int wrongLevel = wrong.getHelpLevel();
        Session idk = workshop.answer(started.getId(), "I don't know", false);
        assertEquals(mcq, idk.getCurrentQuestion());
        assertTrue(idk.getHelpLevel() > wrongLevel,
                "wrongLevel=" + wrongLevel + " idk=" + idk.getHelpLevel()
                        + " wrongHint=" + wrong.getHelpText() + " idkHint=" + idk.getHelpText());
        assertFalse(idk.getHelpText().isBlank());
        Session picked = workshop.answer(started.getId(), workshop.get(started.getId()).getMcqKey(), false);
        assertTrue(picked.isWhyFollowUp());
        Session thin = workshop.answer(started.getId(), "because C", false);
        assertTrue(thin.isWhyFollowUp());
        assertFalse(Checker.hintCopiesQuestion(thin.getHelpText(), thin.getCurrentQuestion()), thin.getHelpText());
        Session idkWhy = workshop.answer(started.getId(), "I don't know", false);
        assertTrue(idkWhy.isWhyFollowUp());
        assertFalse(Checker.hintLeaksEarlierAsk(idkWhy.getHelpText(), idkWhy.getCurrentQuestion(), List.of(mcq))
                        && idkWhy.getHelpText().toLowerCase(Locale.ROOT).contains("after you see 3"),
                idkWhy.getHelpText());
        assertNoForeignLab(idkWhy.getHelpText(), lab);
    }

    private static Session talkLab(WorkshopService workshop, String lab) {
        Socratic.LabKind kind = Socratic.labKind(lab);
        Session started = workshop.start(lab);
        for (int n = 0; n < 32; n++) {
            Session current = workshop.get(started.getId());
            if (current.getPhase() != Phase.ASK) {
                break;
            }
            assertNoForeignLab(current.getCurrentQuestion(), lab);
            if (current.getHelpText() != null && !current.getHelpText().isBlank()) {
                assertNoForeignLab(current.getHelpText(), lab);
            }
            if (current.isWhyFollowUp()) {
                workshop.answer(started.getId(), switch (kind) {
                    case COUNT -> "I look at each letter because a vowel can be anywhere in the word, and I add one to a count that starts at 0.";
                    case ONCE -> "I multiply length and width once because both measurements are already given.";
                    default -> "I look at each value one at a time and keep the largest I have seen so far.";
                }, false);
                continue;
            }
            if ("mcq".equals(current.getAskKind()) && current.getChoices() != null && !current.getChoices().isEmpty()) {
                String key = current.getMcqKey();
                workshop.answer(started.getId(), key == null || key.isBlank() ? "B" : key, false);
                continue;
            }
            workshop.answer(started.getId(), switch (kind) {
                case COUNT -> switch (current.getSocraticStage()) {
                    case 2 -> "Look at every letter once and add one when it is a vowel.";
                    case 4 -> "If this letter is a vowel, add one. If not, leave the count.";
                    case 6 -> "Start count at 0. Look at each letter. Add one if vowel. Show the count.";
                    case 7 -> "h 0, e 1, l 1, l 1, o 2.";
                    default -> "Look through hello by hand and count the vowels.";
                };
                case ONCE -> switch (current.getSocraticStage()) {
                    case 2 -> "Compute once. Just multiply.";
                    case 6 -> "INPUT length and width. COMPUTE once. OUTPUT the area.";
                    default -> "4 times 6 is 24.";
                };
                default -> switch (current.getSocraticStage()) {
                    case 2 -> "Look at every number. Scan once.";
                    case 4 -> "If this number is larger, store it. If smaller, keep the old max.";
                    case 6 -> "The biggest number comes out. INPUT the list. SET max to first. FOR each. IF larger SET max.";
                    default -> "For [-5, -2, -9] it starts at -5, then -2, stays -2.";
                };
            }, false);
        }
        return workshop.get(started.getId());
    }

    private static void assertNoForeignLab(String text, String lab) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        Socratic.LabKind kind = Socratic.labKind(lab);
        if (kind == Socratic.LabKind.COUNT) {
            assertFalse(t.contains("safer start than 0"), text);
            assertFalse(t.contains("largest number"), text);
            assertFalse(t.contains("then 8 then 2"), text);
        }
        if (kind == Socratic.LabKind.ONCE) {
            assertFalse(t.contains("holding"), text);
            assertFalse(t.contains("then 8"), text);
            assertFalse(t.contains("vowel"), text);
        }
        if (kind == Socratic.LabKind.BEST) {
            assertFalse(t.contains("vowel"), text);
            assertFalse(t.contains("length times width"), text);
        }
    }
}
