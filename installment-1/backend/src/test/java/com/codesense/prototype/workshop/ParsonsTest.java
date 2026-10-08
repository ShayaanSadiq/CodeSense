package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class ParsonsTest {

    @Test
    void indentsTheBodyOfALoop() {
        List<PseudoLine> lines = List.of(
                new PseudoLine("1", "INPUT a list", 0),
                new PseudoLine("3", "SET min to the first number", 0),
                new PseudoLine("4", "FOR each number in the list", 0),
                new PseudoLine("5", "IF this number is smaller than min, SET min to this number", 0),
                new PseudoLine("6", "UNTIL there are no more items", 0),
                new PseudoLine("7", "RETURN min", 0),
                new PseudoLine("2", "OUTPUT the smallest number", 0));
        Parsons.applyIndent(lines);
        assertEquals(0, lines.get(0).getIndent());
        assertEquals(0, lines.get(2).getIndent());
        assertEquals(1, lines.get(3).getIndent());
        assertEquals(0, lines.get(4).getIndent());
        assertEquals(0, lines.get(6).getIndent());
    }

    @Test
    void scoresInitBeforeTheLoopAsCorrect() {
        List<PseudoLine> interviewOrder = List.of(
                new PseudoLine("1", "INPUT a list", 0),
                new PseudoLine("2", "OUTPUT the largest number", 0),
                new PseudoLine("3", "FOR each number in the list", 0),
                new PseudoLine("4", "SET max to the first number", 0),
                new PseudoLine("5", "IF this number is greater than max, SET max to this number", 0),
                new PseudoLine("6", "UNTIL there are no more items", 0),
                new PseudoLine("7", "RETURN max", 0));
        List<PseudoLine> expected = Parsons.withCanonicalIndent(interviewOrder);
        assertEquals("INPUT a list", expected.get(0).getText());
        assertEquals("SET max to the first number", expected.get(1).getText());
        assertEquals("FOR each number in the list", expected.get(2).getText());
        assertEquals(1, expected.get(3).getIndent());
        assertEquals("OUTPUT the largest number", expected.get(6).getText());
        List<PseudoLine> student = List.of(
                new PseudoLine("1", "INPUT a list", 0),
                new PseudoLine("4", "SET max to the first number", 0),
                new PseudoLine("3", "FOR each number in the list", 0),
                new PseudoLine("5", "IF this number is greater than max, SET max to this number", 1),
                new PseudoLine("6", "UNTIL there are no more items", 0),
                new PseudoLine("7", "RETURN max", 0),
                new PseudoLine("2", "OUTPUT the largest number", 0));
        assertTrue(Parsons.matches(expected, student));
    }

    @Test
    void orderCanMatchWhenIndentDoesNot() {
        List<PseudoLine> solution = List.of(
                new PseudoLine("1", "FOR each number", 0),
                new PseudoLine("2", "IF this number is greater than max, SET max to this number", 1));
        List<PseudoLine> flat = List.of(
                new PseudoLine("1", "FOR each number", 0),
                new PseudoLine("2", "IF this number is greater than max, SET max to this number", 0));
        assertTrue(Parsons.orderMatches(solution, flat));
        assertFalse(Parsons.matches(solution, flat));
    }

    @Test
    void shuffleChangesOrderAndClearsIndent() {
        List<PseudoLine> solution = List.of(
                new PseudoLine("1", "INPUT a list", 0),
                new PseudoLine("2", "FOR each number", 0),
                new PseudoLine("3", "RETURN min", 1));
        List<PseudoLine> puzzle = Parsons.shuffle(solution);
        assertEquals(3, puzzle.size());
        assertTrue(puzzle.stream().allMatch(line -> line.getIndent() == 0));
        assertFalse(Parsons.matches(solution, puzzle));
        assertTrue(Parsons.isPermutation(solution, puzzle));
    }
}
