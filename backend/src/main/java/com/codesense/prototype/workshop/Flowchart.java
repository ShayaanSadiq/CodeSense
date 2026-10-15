package com.codesense.prototype.workshop;

import com.codesense.prototype.model.PseudoLine;
import java.util.ArrayList;
import java.util.List;

public final class Flowchart {

    public record Node(String id, String text, String verb, String shape, String title, String join) {
    }

    public record Edge(String from, String to, String label) {
    }

    public record Chart(List<Node> nodes, List<Edge> edges) {
    }

    private Flowchart() {
    }

    public static Chart from(List<PseudoLine> lines) {
        List<PseudoLine> ordered = Parsons.withCanonicalIndent(lines);
        List<Node> nodes = new ArrayList<>();
        for (PseudoLine line : ordered) {
            String verb = verb(line.getText());
            nodes.add(new Node(
                    line.getId(),
                    line.getText(),
                    verb,
                    shapeOf(verb),
                    titleOf(verb),
                    joinOf(verb)));
        }
        List<Edge> edges = new ArrayList<>();
        String forId = null;
        for (int i = 0; i < ordered.size(); i++) {
            PseudoLine from = ordered.get(i);
            String fromVerb = verb(from.getText());
            if ("FOR".equals(fromVerb) || "WHILE".equals(fromVerb)) {
                forId = from.getId();
            }
            if (i + 1 >= ordered.size()) {
                continue;
            }
            PseudoLine to = ordered.get(i + 1);
            String label = "next";
            if ("IF".equals(fromVerb) && to.getIndent() > from.getIndent()) {
                label = "yes";
            } else if ("IF".equals(fromVerb)) {
                label = "no";
            } else if (to.getIndent() > from.getIndent()) {
                label = "yes";
            }
            edges.add(new Edge(from.getId(), to.getId(), label));
        }
        if (forId != null) {
            for (PseudoLine line : ordered) {
                if ("UNTIL".equals(verb(line.getText()))) {
                    edges.add(new Edge(line.getId(), forId, "again"));
                    break;
                }
            }
        }
        return new Chart(List.copyOf(nodes), List.copyOf(edges));
    }

    public static String shapeOf(String verb) {
        String key = verb == null ? "" : verb.trim().toUpperCase();
        return switch (key) {
            case "INPUT", "OUTPUT" -> "io";
            case "IF" -> "diamond";
            default -> "box";
        };
    }

    public static String titleOf(String verb) {
        return switch (verb == null ? "" : verb.trim().toUpperCase()) {
            case "INPUT" -> "What you are given";
            case "SET" -> "Remember";
            case "FOR", "WHILE" -> "Look at each";
            case "COMPUTE" -> "One calculation";
            case "IF" -> "Yes or no?";
            case "UNTIL" -> "Stop?";
            case "OUTPUT" -> "Show the answer";
            default -> "Next step";
        };
    }

    public static String joinOf(String verb) {
        return switch (verb == null ? "" : verb.trim().toUpperCase()) {
            case "INPUT" -> "Start here. Join the first arrow down from this parallelogram.";
            case "IF" -> "This is a diamond. Join it under the step that looks at each item. The yes arm is the check.";
            case "OUTPUT" -> "Join this parallelogram last, after the work is finished.";
            case "UNTIL" -> "Join this box after the check. The again arrow goes back to look at the next item.";
            default -> "Join this box to the arrow that comes next.";
        };
    }

    private static String verb(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().split("\\s+")[0].toUpperCase();
    }
}
