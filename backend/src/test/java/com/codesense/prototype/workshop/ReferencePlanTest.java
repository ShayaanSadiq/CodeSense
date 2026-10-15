package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import com.codesense.prototype.model.Session;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferencePlanTest {

    @Test
    void largestPutsTheUpdateInsideTheCheck() {
        List<PseudoLine> plan = ReferencePlan.build("Find the largest number in a list.");
        var texts = plan.stream().map(PseudoLine::getText).toList();
        assertEquals("INPUT a list of numbers", texts.get(0));
        assertEquals("SET max to the first number", texts.get(1));
        assertEquals("FOR each number in the list", texts.get(2));
        assertEquals("IF this number is greater than max", texts.get(3));
        assertEquals("SET max to this number", texts.get(4));
        assertEquals(1, plan.get(3).getIndent());
        assertEquals(2, plan.get(4).getIndent());
        assertTrue(texts.indexOf("SET max to the first number") < texts.indexOf("FOR each number in the list"));
        assertTrue(texts.indexOf("FOR each number in the list") < texts.indexOf("SET max to this number"));
    }

    @Test
    void firstCorrectAnswerOnlyUnlocksInput() {
        Session session = new Session();
        session.setReferenceLines(ReferencePlan.build("Find the largest number in a list."));
        ReferencePlan.reveal(session, 1);
        assertEquals(List.of("INPUT a list of numbers"),
                session.getConfirmedLines().stream().map(PseudoLine::getText).toList());
    }

    @Test
    void updateStaysHiddenUntilTheCheckStage() {
        Session session = new Session();
        session.setReferenceLines(ReferencePlan.build("Find the largest number in a list."));
        ReferencePlan.reveal(session, 3);
        var texts = session.getConfirmedLines().stream().map(PseudoLine::getText).toList();
        assertTrue(texts.contains("SET max to the first number"));
        assertTrue(texts.contains("FOR each number in the list"));
        assertTrue(texts.stream().noneMatch(text -> text.equals("SET max to this number")));
        ReferencePlan.reveal(session, 4);
        texts = session.getConfirmedLines().stream().map(PseudoLine::getText).toList();
        assertTrue(texts.contains("IF this number is greater than max"));
        assertTrue(texts.contains("SET max to this number"));
        assertTrue(texts.indexOf("FOR each number in the list") < texts.indexOf("SET max to this number"));
    }
}
