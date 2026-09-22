package com.codesense.prototype.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
