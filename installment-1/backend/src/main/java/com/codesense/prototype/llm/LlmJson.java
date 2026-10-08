package com.codesense.prototype.llm;

import com.codesense.prototype.workshop.Grader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public final class LlmJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LlmJson() {
    }

    public static String stripThink(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("(?is)<think>[\\s\\S]*?</think>", "").trim();
    }

    public static JsonNode extractObject(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = stripThink(text).replace("```json", "").replace("```", "");
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(cleaned.substring(start, end + 1));
            return node.isObject() ? node : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static List<String> parseLines(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("lines") || !node.get("lines").isArray()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode item : node.get("lines")) {
            if (item.isTextual()) {
                lines.add(item.asText());
            }
        }
        return lines;
    }

    public static String parseQuestion(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("question") || !node.get("question").isTextual()) {
            return null;
        }
        String question = node.get("question").asText().trim();
        return question.isEmpty() ? null : question;
    }

    public static Grader.Grade parseGrade(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("correct")) {
            return null;
        }
        JsonNode flag = node.get("correct");
        Boolean correct = null;
        if (flag.isBoolean()) {
            correct = flag.asBoolean();
        } else if (flag.isString()) {
            String raw = flag.asText().trim();
            if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
                correct = Boolean.parseBoolean(raw);
            }
        }
        if (correct == null) {
            return null;
        }
        String expected = node.has("expected") && node.get("expected").isTextual()
                ? node.get("expected").asText().trim()
                : "";
        String explanation = node.has("explanation") && node.get("explanation").isTextual()
                ? node.get("explanation").asText().trim()
                : "";
        return new Grader.Grade(correct, expected, explanation);
    }
}
