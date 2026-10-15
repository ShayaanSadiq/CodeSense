package com.codesense.prototype.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LlmJsonTest {

    @Test
    void stripsThinkBlocks() {
        String raw = "<think>I should not leak SET max</think>\n{\"question\":\"What do you remember as you go?\"}";
        assertFalse(LlmJson.stripThink(raw).contains("<think>"));
        assertEquals("What do you remember as you go?", LlmJson.parseQuestion(raw));
    }

    @Test
    void readsFencedLineJson() {
        String raw = "```json\n{\"lines\":[\"INPUT a list\",\"OUTPUT the largest number\"]}\n```";
        assertEquals(List.of("INPUT a list", "OUTPUT the largest number"), LlmJson.parseLines(raw));
        assertNotNull(LlmJson.extractObject(raw));
    }

    @Test
    void readsGradeJson() {
        var grade = LlmJson.parseGrade("{\"correct\":false,\"expected\":\"A list comes in.\",\"explanation\":\"INPUT is the list.\"}");
        assertNotNull(grade);
        assertFalse(grade.correct());
        assertEquals("A list comes in.", grade.expected());
    }

    @Test
    void readsWrongCardAndOrderNotes() {
        assertEquals(
                "You would not use this card here.",
                LlmJson.parseExplanation("{\"explanation\":\"You would not use this card here.\"}"));
        var notes = LlmJson.parseArrangeNotes(
                "{\"summary\":\"Two blocks are swapped.\",\"notes\":[{\"id\":\"a\",\"why\":\"OUTPUT is last.\"}]}");
        assertEquals(1, notes.size());
        assertEquals("a", notes.get(0).getId());
        assertEquals("OUTPUT is last.", notes.get(0).getWhy());
    }

    @Test
    void readsVoiceAndCardJudge() {
        var voice = LlmJson.parseVoice(
                "{\"voiced\":[{\"text\":\"A list of numbers is waiting at the start.\",\"card\":\"INPUT\"},{\"text\":\"Give back the result you kept.\",\"card\":\"OUTPUT\"}],\"needed\":[\"INPUT\",\"OUTPUT\"],\"unused\":[{\"card\":\"FOR\",\"why\":\"There is no list to look through.\"}]}");
        assertEquals("A list of numbers is waiting at the start. Give back the result you kept.", voice.text());
        assertEquals(List.of("INPUT", "OUTPUT"), voice.needed());
        assertEquals(2, voice.lines().size());
        assertEquals("INPUT", voice.lines().get(0).card());
        assertEquals("There is no list to look through.", voice.unused().get("FOR"));
        var judge = LlmJson.parseCardJudge(
                "{\"correct\":true,\"explanation\":\"The list is what you are given.\"}");
        assertEquals(true, judge.correct());
        assertEquals("The list is what you are given.", judge.explanation());
    }

    @Test
    void readsLabBriefAndWalk() {
        var brief = LlmJson.parseLabBrief(
                "{\"input\":\"An array of integers, such as [3, 8, 2, 10, 5].\",\"expectedOutput\":\"10\",\"constraints\":[\"At least one element.\"],\"sample\":\"[3, 8, 2, 10, 5]\",\"tests\":[{\"input\":\"[3, 8, 2, 10, 5]\",\"expected\":\"10\",\"status\":\"Pass\"}]}");
        assertEquals("An array of integers, such as [3, 8, 2, 10, 5].", brief.getInput());
        assertEquals("10", brief.getOutput());
        assertEquals(1, brief.getTests().size());
        assertEquals("Pass", brief.getTests().get(0).getStatus());
        var walk = LlmJson.parseWalk(
                "{\"lines\":[{\"code\":\"numbers = [3, 8, 2]\",\"what\":\"Stores the list\",\"why\":\"The script needs the given numbers.\",\"needed\":\"Without this there is nothing to search.\"}]}");
        assertEquals(1, walk.size());
        assertEquals("numbers = [3, 8, 2]", walk.get(0).getCode());
        assertEquals("Stores the list", walk.get(0).getHeading());
        assertEquals("Without this there is nothing to search.", walk.get(0).getNeeded());
    }

    @Test
    void readsPredictStepQuestions() {
        var questions = LlmJson.parsePredictQuestions(
                "{\"steps\":[{\"id\":\"P-INPUT-1\",\"question\":\"Which numbers are sitting in the list first?\"}]}");
        assertEquals("Which numbers are sitting in the list first?", questions.get("P-INPUT-1"));
        var byPrompt = LlmJson.parsePredictQuestions(
                "{\"steps\":[{\"id\":\"P-SET-1\",\"prompt\":\"What do you remember first?\"}]}");
        assertEquals("What do you remember first?", byPrompt.get("P-SET-1"));
    }

    @Test
    void readsSocraticAskMcq() {
        var ask = LlmJson.parseAsk(
                "{\"kind\":\"mcq\",\"question\":\"Which approach would work?\",\"choices\":[{\"letter\":\"A\",\"text\":\"Sort then last\"},{\"letter\":\"B\",\"text\":\"Keep a current maximum\"},{\"letter\":\"C\",\"text\":\"Add them\"},{\"letter\":\"D\",\"text\":\"First and last\"}],\"correct\":\"B\",\"whyQuestion\":\"Why that one?\"}",
                true);
        assertNotNull(ask);
        assertTrue(ask.isMcq());
        assertEquals("B", ask.correct());
        assertEquals(4, ask.choices().size());
    }

    @Test
    void readsSocraticHelp() {
        var help = LlmJson.parseHelp(
                "{\"level\":2,\"hint\":\"Let's make it smaller.\",\"question\":\"3 or 8?\",\"followUp\":\"Why 8?\",\"expected\":\"8\"}",
                2,
                "How would you keep track of the largest?");
        assertNotNull(help);
        assertEquals(2, help.level());
        assertEquals("Let's make it smaller.", help.hint());
        assertEquals("3 or 8?", help.question());
        assertEquals("Why 8?", help.followUp());
        assertEquals("8", help.expected());
    }
}
