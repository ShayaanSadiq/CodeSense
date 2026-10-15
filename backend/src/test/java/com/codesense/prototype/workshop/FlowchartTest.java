package com.codesense.prototype.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.model.PseudoLine;
import java.util.List;
import org.junit.jupiter.api.Test;

class FlowchartTest {

    @Test
    void largestUsesDiamondAndIoShapes() {
        var chart = Flowchart.from(List.of(
                new PseudoLine("a", "INPUT a list of numbers", 0),
                new PseudoLine("b", "SET max to the first number", 0),
                new PseudoLine("c", "FOR each number in the list", 0),
                new PseudoLine("d", "IF this number is greater than max, SET max to this number", 1),
                new PseudoLine("e", "UNTIL there are no more items", 0),
                new PseudoLine("f", "OUTPUT the largest number", 0)));
        assertEquals("io", shape("a", chart));
        assertEquals("box", shape("b", chart));
        assertEquals("diamond", shape("d", chart));
        assertEquals("io", shape("f", chart));
        assertTrue(chart.edges().stream().anyMatch(edge ->
                "c".equals(edge.from()) && "d".equals(edge.to()) && "yes".equals(edge.label())));
        assertTrue(chart.edges().stream().anyMatch(edge ->
                "e".equals(edge.from()) && "c".equals(edge.to()) && "again".equals(edge.label())));
    }

    @Test
    void areaIsThreeBoxesNoDiamond() {
        var chart = Flowchart.from(List.of(
                new PseudoLine("in", "INPUT the length and the width", 0),
                new PseudoLine("once", "COMPUTE the result in one step", 0),
                new PseudoLine("out", "OUTPUT the area", 0)));
        assertEquals(List.of("io", "box", "io"), chart.nodes().stream().map(Flowchart.Node::shape).toList());
        assertTrue(chart.nodes().stream().noneMatch(node -> "diamond".equals(node.shape())));
        assertEquals(2, chart.edges().size());
    }

    private static String shape(String id, Flowchart.Chart chart) {
        return chart.nodes().stream()
                .filter(node -> id.equals(node.id()))
                .findFirst()
                .orElseThrow()
                .shape();
    }
}
