package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.Slot;
import org.junit.jupiter.api.Test;

class GraderTest {

    @Test
    void acceptsACorrectIoAnswer() {
        assertTrue(Grader.fromRules(
                        Slot.IO,
                        "A list comes in. The biggest number comes out.",
                        "Find the largest number in a list.")
                .correct());
    }

    @Test
    void rejectsAWrongIoAnswer() {
        assertFalse(Grader.fromRules(Slot.IO, "the sum of the list", "Find the largest number in a list.")
                .correct());
        assertFalse(Grader.fromRules(Slot.IO, "asdfasdf", "Smallest in a list").correct());
    }
}
