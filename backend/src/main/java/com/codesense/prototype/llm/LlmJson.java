package com.codesense.prototype.llm;

import com.codesense.prototype.model.ArrangeNote;
import com.codesense.prototype.workshop.Checker;
import com.codesense.prototype.workshop.Grader;
import com.codesense.prototype.workshop.Socratic;
import com.codesense.prototype.workshop.Translator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static Map<String, String> parsePredictQuestions(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("steps") || !node.get("steps").isArray()) {
            return Map.of();
        }
        Map<String, String> questions = new LinkedHashMap<>();
        for (JsonNode item : node.get("steps")) {
            String id = textOf(item, "id");
            if (id.isEmpty()) {
                id = "step-" + questions.size();
            }
            String question = textOf(item, "question");
            if (question.isEmpty()) {
                question = textOf(item, "prompt");
            }
            if (!question.isEmpty()) {
                questions.put(id, question);
            }
        }
        return questions;
    }

    public static String parseQuestion(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("question") || !node.get("question").isTextual()) {
            return null;
        }
        String question = node.get("question").asText().trim();
        return question.isEmpty() ? null : question;
    }

    public static Socratic.Ask parseAsk(String text, boolean preferMcq) {
        JsonNode node = extractObject(text);
        if (node == null) {
            return null;
        }
        String question = textOf(node, "question");
        if (question.isEmpty()) {
            String parsed = parseQuestion(text);
            question = parsed == null ? "" : parsed;
        }
        if (question.isEmpty()) {
            return null;
        }
        String kind = textOf(node, "kind").toLowerCase();
        List<com.codesense.prototype.model.McqOption> choices = new ArrayList<>();
        JsonNode items = node.get("choices");
        if (items != null && items.isArray()) {
            for (JsonNode item : items) {
                if (!item.isObject()) {
                    continue;
                }
                String letter = textOf(item, "letter");
                String body = textOf(item, "text");
                if (letter.isEmpty() || body.isEmpty()) {
                    continue;
                }
                choices.add(new com.codesense.prototype.model.McqOption(letter.toUpperCase(), body));
            }
        }
        if (kind.isBlank()) {
            kind = choices.size() >= 2 ? "mcq" : "text";
        }
        String correct = textOf(node, "correct").toUpperCase();
        String why = textOf(node, "whyQuestion");
        if (why.isEmpty()) {
            why = Socratic.defaultWhy();
        }
        if (!preferMcq) {
            if (!Checker.questionIsSafe(question)) {
                return null;
            }
            return new Socratic.Ask("text", question, List.of(), "", why);
        }
        if (!"mcq".equals(kind) || choices.size() < 4 || correct.isEmpty()) {
            return null;
        }
        if (!Checker.questionIsSafe(question)) {
            return null;
        }
        for (com.codesense.prototype.model.McqOption option : choices) {
            if (Checker.questionLeaksStep(option.getText())) {
                return null;
            }
        }
        return new Socratic.Ask("mcq", question, choices, correct, why);
    }

    public static Socratic.Help parseHelp(String text, int level, String originalQuestion) {
        JsonNode node = extractObject(text);
        if (node == null) {
            return null;
        }
        String hint = textOf(node, "hint");
        String question = textOf(node, "question");
        if (question.isEmpty()) {
            question = parseQuestion(text);
        }
        if (hint.isEmpty() && (question == null || question.isEmpty())) {
            return null;
        }
        String followUp = textOf(node, "followUp");
        String expected = textOf(node, "expected");
        int parsedLevel = level;
        if (node.has("level") && node.get("level").isNumber()) {
            parsedLevel = node.get("level").asInt(level);
        }
        if (question == null || question.isEmpty()) {
            question = originalQuestion;
        }
        return new Socratic.Help(parsedLevel, hint, question, followUp, expected);
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

    public static String parseExplanation(String text) {
        JsonNode node = extractObject(text);
        if (node == null) {
            return null;
        }
        for (String field : List.of("explanation", "summary")) {
            if (node.has(field) && node.get(field).isTextual()) {
                String value = node.get(field).asText().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    public static List<ArrangeNote> parseArrangeNotes(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("notes") || !node.get("notes").isArray()) {
            return List.of();
        }
        List<ArrangeNote> notes = new ArrayList<>();
        for (JsonNode item : node.get("notes")) {
            if (!item.isObject() || !item.has("id") || !item.has("why")) {
                continue;
            }
            String id = item.get("id").asText("").trim();
            String why = item.get("why").asText("").trim();
            if (!id.isEmpty() && !why.isEmpty()) {
                notes.add(new ArrangeNote(id, why));
            }
        }
        return notes;
    }

    public static Translator.Voice parseVoice(String text) {
        JsonNode node = extractObject(text);
        if (node == null) {
            return null;
        }
        List<Translator.VoiceLine> lines = readVoiceLines(node);
        String voiced = node.has("voiced") && node.get("voiced").isTextual()
                ? node.get("voiced").asText().trim()
                : "";
        if (voiced.isEmpty() && node.has("text") && node.get("text").isTextual()) {
            voiced = node.get("text").asText().trim();
        }
        if (voiced.isEmpty() && !lines.isEmpty()) {
            voiced = Translator.joinVoice(lines);
        }
        List<String> needed = new ArrayList<>();
        if (node.has("needed") && node.get("needed").isArray()) {
            for (JsonNode item : node.get("needed")) {
                if (item.isTextual()) {
                    needed.add(item.asText().trim().toUpperCase());
                }
            }
        }
        if (needed.isEmpty()) {
            for (Translator.VoiceLine line : lines) {
                needed.add(line.card());
            }
        }
        if (voiced.isEmpty() || needed.isEmpty()) {
            return null;
        }
        Map<String, String> unused = new LinkedHashMap<>();
        if (node.has("unused") && node.get("unused").isArray()) {
            for (JsonNode item : node.get("unused")) {
                if (!item.isObject()) {
                    continue;
                }
                String card = textOf(item, "card").toUpperCase();
                String why = textOf(item, "why");
                if (why.isEmpty()) {
                    why = textOf(item, "explanation");
                }
                if (!card.isEmpty() && !why.isEmpty()) {
                    unused.put(card, why);
                }
            }
        } else if (node.has("unused") && node.get("unused").isObject()) {
            JsonNode unusedNode = node.get("unused");
            unusedNode.propertyNames().forEach(name -> {
                JsonNode value = unusedNode.get(name);
                if (value != null && value.isTextual()) {
                    String card = name.trim().toUpperCase();
                    String why = value.asText("").trim();
                    if (!card.isEmpty() && !why.isEmpty()) {
                        unused.put(card, why);
                    }
                }
            });
        }
        return new Translator.Voice(voiced, needed, unused, lines);
    }

    private static List<Translator.VoiceLine> readVoiceLines(JsonNode node) {
        List<Translator.VoiceLine> lines = new ArrayList<>();
        JsonNode source = null;
        if (node.has("voiced") && node.get("voiced").isArray()) {
            source = node.get("voiced");
        } else if (node.has("lines") && node.get("lines").isArray()) {
            source = node.get("lines");
        }
        if (source == null) {
            return lines;
        }
        for (JsonNode item : source) {
            if (!item.isObject()) {
                continue;
            }
            String sentence = textOf(item, "text");
            if (sentence.isEmpty()) {
                sentence = textOf(item, "sentence");
            }
            String card = textOf(item, "card").toUpperCase();
            if (card.isEmpty()) {
                card = textOf(item, "construct").toUpperCase();
            }
            if ("RETURN".equals(card)) {
                card = "OUTPUT";
            }
            if (sentence.isEmpty() || card.isEmpty()) {
                continue;
            }
            lines.add(new Translator.VoiceLine(sentence, card));
        }
        return lines;
    }

    public static Translator.CardJudge parseCardJudge(String text) {
        JsonNode node = extractObject(text);
        if (node == null || !node.has("correct") || !node.has("explanation")) {
            return null;
        }
        Boolean correct = null;
        JsonNode flag = node.get("correct");
        if (flag.isBoolean()) {
            correct = flag.asBoolean();
        } else if (flag.isString()) {
            String raw = flag.asText().trim();
            if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
                correct = Boolean.parseBoolean(raw);
            }
        }
        String explanation = node.get("explanation").isTextual() ? node.get("explanation").asText().trim() : "";
        if (correct == null || explanation.isEmpty()) {
            return null;
        }
        return new Translator.CardJudge(correct, explanation);
    }

    public static com.codesense.prototype.model.LabBrief parseLabBrief(String text) {
        JsonNode node = extractObject(text);
        if (node == null) {
            return null;
        }
        com.codesense.prototype.model.LabBrief brief = new com.codesense.prototype.model.LabBrief();
        brief.setInput(textOf(node, "input"));
        String expected = textOf(node, "expectedOutput");
        if (expected.isBlank()) {
            expected = textOf(node, "output");
        }
        brief.setOutput(expected);
        List<String> bullets = stringList(node, "constraints");
        if (!bullets.isEmpty()) {
            brief.setConstraintItems(bullets);
        } else {
            brief.setConstraints(textOf(node, "constraints"));
        }
        brief.setSample(textOf(node, "sample"));
        brief.setPassedTests(stringList(node, "passedTests"));
        brief.setFailedTests(stringList(node, "failedTests"));
        brief.setTests(caseList(node, "tests"));
        brief.finish();
        if (brief.getInput().isBlank()) {
            return null;
        }
        return brief;
    }

    public static List<com.codesense.prototype.model.WalkLine> parseWalk(String text) {
        JsonNode node = extractObject(text);
        if (node == null) {
            return List.of();
        }
        JsonNode items = node.has("lines") && node.get("lines").isArray()
                ? node.get("lines")
                : node.has("steps") && node.get("steps").isArray() ? node.get("steps") : null;
        if (items == null) {
            return List.of();
        }
        List<com.codesense.prototype.model.WalkLine> steps = new ArrayList<>();
        for (JsonNode item : items) {
            if (!item.isObject()) {
                continue;
            }
            String code = textOf(item, "code");
            String heading = textOf(item, "what");
            if (heading.isBlank()) {
                heading = textOf(item, "heading");
            }
            String body = textOf(item, "why");
            if (body.isBlank()) {
                body = textOf(item, "body");
            }
            String needed = textOf(item, "needed");
            if (heading.isBlank() && body.isBlank() && needed.isBlank()) {
                continue;
            }
            steps.add(new com.codesense.prototype.model.WalkLine(
                    code,
                    heading.isBlank() ? "This line" : heading,
                    body,
                    needed));
        }
        return steps;
    }

    public static Socratic.TurnJudge parseTurnJudge(String text, int currentStage) {
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
        boolean skip = node.has("skipAhead") && node.get("skipAhead").asBoolean(false);
        int next = currentStage + 1;
        if (node.has("nextStage") && node.get("nextStage").isNumber()) {
            next = node.get("nextStage").asInt(next);
        }
        String expected = textOf(node, "expected");
        String explanation = textOf(node, "explanation");
        String question = textOf(node, "question");
        if (question.isEmpty()) {
            question = parseQuestion(text);
        }
        return new Socratic.TurnJudge(correct, skip, next, expected, explanation, question);
    }

    private static String textOf(JsonNode node, String field) {
        if (node == null || !node.has(field) || !node.get(field).isTextual()) {
            return "";
        }
        return node.get(field).asText("").trim();
    }

    private static List<String> stringList(JsonNode node, String field) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.has(field) || !node.get(field).isArray()) {
            return values;
        }
        for (JsonNode item : node.get(field)) {
            if (item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private static List<com.codesense.prototype.model.LabCase> caseList(JsonNode node, String field) {
        List<com.codesense.prototype.model.LabCase> values = new ArrayList<>();
        if (node == null || !node.has(field) || !node.get(field).isArray()) {
            return values;
        }
        for (JsonNode item : node.get(field)) {
            if (!item.isObject()) {
                continue;
            }
            String input = textOf(item, "input");
            String expected = textOf(item, "expected");
            String status = textOf(item, "status");
            if (input.isBlank() && expected.isBlank()) {
                continue;
            }
            values.add(new com.codesense.prototype.model.LabCase(
                    input, expected, status.isBlank() ? "Pass" : status));
        }
        return values;
    }
}
