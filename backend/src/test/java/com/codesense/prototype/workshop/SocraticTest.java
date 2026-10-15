package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SocraticTest {

    private static final String LARGEST = "Find the largest number in a list.";
    private static final String AREA = "Compute the area of a rectangle given length and width.";

    @Test
    void fallbackHelpOnWhyKeepsTheQuestionAndIsNotASlogan() {
        var brief = Socratic.briefFromRules(LARGEST);
        String original = "In your own words, why does looking one at a time work?";
        for (int level = 1; level <= 4; level++) {
            var help = Socratic.fallbackHelp(1, level, brief, original, true);
            assertFalse(Socratic.thinSlogan(help.hint()), "level " + level + " hint=" + help.hint());
            assertTrue(help.hint().length() > 40);
            assertTrue(Checker.sameQuestion(original, help.question()), "level " + level + " q=" + help.question());
        }
        var storeWhy = Socratic.fallbackHelp(3, 1, brief, original, true);
        assertTrue(Checker.sameQuestion(original, storeWhy.question()), storeWhy.question());
        assertTrue(storeWhy.hint().toLowerCase().contains("0")
                || storeWhy.hint().toLowerCase().contains("zero")
                || storeWhy.hint().toLowerCase().contains("first"),
                storeWhy.hint());
        assertFalse(storeWhy.hint().toLowerCase().contains("then 8")
                && storeWhy.hint().toLowerCase().contains("remember"),
                storeWhy.hint());
        var why1 = Socratic.fallbackHelp(1, 1, brief, original, true);
        var why2 = Socratic.fallbackHelp(1, 2, brief, original, true);
        assertTrue(Checker.sameQuestion(original, why1.question()));
        assertTrue(Checker.sameQuestion(original, why2.question()));
        assertNotEquals(why1.hint(), why2.hint());
        var firstHint = Socratic.fallbackHelp(1, 1, brief, original, false);
        assertTrue(Checker.sameQuestion(original, firstHint.question()));
        assertFalse(Socratic.thinSlogan(firstHint.hint()));
        var decompose = Socratic.fallbackHelp(1, 2, brief, original, false);
        assertTrue(Checker.sameQuestion(original, decompose.question()));
        assertFalse(Socratic.thinSlogan(decompose.hint()));
        assertNotEquals(firstHint.hint(), decompose.hint());
    }

    @Test
    void stageSevenQuestionIsByHandNotAnAlgorithmTrace() {
        var brief = Socratic.briefFromRules(LARGEST);
        String q = Socratic.fallbackQuestion(7, brief, LARGEST);
        String lower = q.toLowerCase();
        assertFalse(lower.contains("algorithm"));
        assertFalse(lower.contains("trace"));
        assertFalse(lower.contains("maximum is updated"));
        assertFalse(lower.contains("current maximum"));
        assertFalse(lower.contains("running max"));
        assertTrue(q.contains("holding"));
        assertTrue(q.contains("3") && q.contains("8") && q.contains("2"));
        assertTrue(Checker.questionIsSafe(q));
    }

    @Test
    void computeOnceStageSevenAsksMultiplyNotAListWalk() {
        var brief = Socratic.briefFromRules(AREA);
        String q = Socratic.fallbackQuestion(7, brief, AREA);
        String lower = q.toLowerCase();
        assertFalse(lower.contains("holding"));
        assertFalse(lower.contains("then 8"));
        assertTrue(lower.contains("multiply") || lower.contains("times"));
        assertTrue(Checker.questionFitsLab(q, AREA, brief));
        var help = Socratic.fallbackHelp(7, 1, brief, q, false, AREA);
        assertFalse(help.hint().toLowerCase().contains("holding"));
        assertTrue(help.hint().toLowerCase().contains("multiply")
                || help.hint().toLowerCase().contains("measurements"));
    }

    @Test
    void holdingHintsSteerWithoutNamingTheLargest() {
        var brief = Socratic.briefFromRules(LARGEST);
        String q = "After the first three numbers, what number are you holding in your head?";
        for (int level = 1; level <= 4; level++) {
            var help = Socratic.fallbackHelp(7, level, brief, q, false, LARGEST);
            assertFalse(Checker.hintDumpsAnswer(help.hint(), q), "level " + level + " hint=" + help.hint());
            assertFalse(help.hint().toLowerCase().contains("you should be holding"));
            assertFalse(help.hint().toLowerCase().contains("holding is the largest"));
        }
    }

    @Test
    void stopStageHintStaysOnStopping() {
        var brief = Socratic.briefFromRules(LARGEST);
        var help = Socratic.fallbackHelp(5, 1, brief, "When should you stop looking through the rest?", false);
        String hint = help.hint().toLowerCase();
        assertTrue(hint.contains("one more") || hint.contains("skip checking") || hint.contains("look at it too"),
                help.hint());
        assertFalse(hint.contains("keep track"));
        assertFalse(Checker.hintMissesTheQuestion(help.hint(), "When should you stop looking through the rest?"));
    }

    @Test
    void stageSixAsksGivenFirstAndShownLast() {
        var brief = Socratic.briefFromRules(LARGEST);
        String q = Socratic.fallbackQuestion(6, brief, LARGEST);
        String lower = q.toLowerCase();
        assertTrue(lower.contains("given first") || lower.contains("be given"), q);
        assertTrue(lower.contains("write down") || lower.contains("at the end"), q);
        assertFalse(lower.contains("which part"));
        var help = Socratic.fallbackHelp(6, 1, brief, q, false, LARGEST);
        assertFalse(help.hint().toLowerCase().contains("still have to look at 8"), help.hint());
        assertFalse(help.hint().toLowerCase().contains("first and last values"), help.hint());
        assertTrue(help.hint().toLowerCase().contains("you"), help.hint());
        assertFalse(Checker.hintMissesTheQuestion(help.hint(), q), help.hint());
    }

    @Test
    void whyLooksThinAllowsAShortNumberComparison() {
        assertFalse(Socratic.whyLooksThin("because 10 is bigger than 5"));
        assertFalse(Socratic.whyLooksThin("10 is larger so I keep it"));
        assertFalse(Socratic.whyLooksThin("because negative numbers are possible"));
        assertFalse(Socratic.whyLooksThin("zero might not be in the list"));
        assertFalse(Socratic.whyLooksThin(
                "I look at each letter because a vowel can be anywhere in the word, and I add one to a count that starts at 0."));
        assertTrue(Socratic.whyLooksThin("because C"));
        assertTrue(Socratic.whyLooksThin("C is the correct answer"));
        assertTrue(Socratic.whyLooksThin("because it is correct"));
    }

    @Test
    void capAdvancementIgnoresSkipAheadToParsons() {
        assertEquals(2, Socratic.capAdvancement(1, 8, true, LARGEST));
        assertEquals(3, Socratic.capAdvancement(2, 8, true, LARGEST));
        assertEquals(7, Socratic.capAdvancement(6, 8, true, LARGEST));
        assertEquals(8, Socratic.capAdvancement(7, 8, true, LARGEST));
    }

    @Test
    void capAdvancementSkipsOnlyMemoryStopOnComputeOnce() {
        assertEquals(2, Socratic.capAdvancement(1, 8, true, AREA));
        assertEquals(6, Socratic.capAdvancement(2, 8, true, AREA));
        assertEquals(7, Socratic.capAdvancement(6, 8, true, AREA));
        assertEquals(8, Socratic.capAdvancement(7, 8, false, AREA));
    }

    @Test
    void judgeFromRulesDoesNotFinishAfterStageOne() {
        var brief = Socratic.briefFromRules(LARGEST);
        var grade = Socratic.judgeFromRules(1,
                "I look through the list by hand and keep the biggest.",
                LARGEST, brief);
        assertTrue(grade.correct());
        assertFalse(grade.skipAhead());
        assertEquals(2, grade.nextStage());
    }

    @Test
    void judgeFromRulesStillTracesAfterComputeOncePlan() {
        var brief = Socratic.briefFromRules(AREA);
        var stageOne = Socratic.judgeFromRules(1, "Use the given length and width and multiply.", AREA, brief);
        assertEquals(2, stageOne.nextStage());
        var stageTwo = Socratic.judgeFromRules(2, "Compute once. Just multiply.", AREA, brief);
        assertTrue(stageTwo.skipAhead());
        assertEquals(6, stageTwo.nextStage());
        var stageSix = Socratic.judgeFromRules(6,
                "INPUT length and width. COMPUTE once. OUTPUT the area.", AREA, brief);
        assertFalse(stageSix.skipAhead());
        assertEquals(7, stageSix.nextStage());
        var stageSeven = Socratic.judgeFromRules(7, "4 times 6 is 24.", AREA, brief);
        assertEquals(8, stageSeven.nextStage());
    }
}
