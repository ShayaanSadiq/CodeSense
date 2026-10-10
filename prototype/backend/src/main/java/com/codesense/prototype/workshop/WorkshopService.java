package com.codesense.prototype.workshop;

import com.codesense.prototype.llm.LlmCompleter;
import com.codesense.prototype.llm.LlmJson;
import com.codesense.prototype.llm.LlmPrompts;
import com.codesense.prototype.model.LlmMode;
import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.PseudoLine;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.model.Slot;
import com.codesense.prototype.model.Turn;
import com.codesense.prototype.model.WorkshopConstants;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkshopService {

    private final Map<String, Session> sessions = new LinkedHashMap<>();
    private final ObjectMapper mapper;
    private final Path persistFile;
    private LlmCompleter completer;
    private LlmMode reportedMode = LlmMode.RULES;

    @Autowired
    public WorkshopService(ObjectMapper mapper, com.codesense.prototype.llm.LlmService llm,
            com.codesense.prototype.config.CodeSenseProperties properties) {
        this.mapper = mapper;
        this.persistFile = properties.sessionsFile();
        this.completer = llm;
        this.reportedMode = llm.currentMode();
        load();
    }

    public WorkshopService() {
        this.mapper = new ObjectMapper();
        this.persistFile = null;
        this.completer = (system, user) -> null;
        this.reportedMode = LlmMode.RULES;
    }

    public WorkshopService(LlmCompleter completer, LlmMode mode) {
        this.mapper = new ObjectMapper();
        this.persistFile = null;
        this.completer = completer;
        this.reportedMode = mode == null ? LlmMode.RULES : mode;
    }

    public synchronized void useLlm(LlmCompleter completer, LlmMode mode) {
        this.completer = completer;
        this.reportedMode = mode == null ? LlmMode.RULES : mode;
    }

    public synchronized Session get(String id) {
        return sessions.get(id);
    }

    public synchronized Session start(String labQuestion) {
        String question = labQuestion == null ? "" : labQuestion.trim();
        String id = UUID.randomUUID().toString();
        Session session = new Session();
        session.setId(id);
        session.setLabQuestion(question);
        session.setSlot(Slot.INPUT);
        session.setProposedLines(new ArrayList<>());
        session.setConfirmedLines(new ArrayList<>());
        session.setPuzzleLines(new ArrayList<>());
        session.setTurns(new ArrayList<>());
        session.setDontKnowCount(0);
        session.setAskedQuestions(new ArrayList<>());
        session.setMemoryFollowUps(0);
        session.setUsedFallbackQuestion(false);
        session.setLlmMode(reportedMode);
        session.setWrongAttempts(0);
        session.setMaxAttempts(WorkshopConstants.MAX_ANSWER_TRIES);
        clearVerdict(session);
        String refusal = Suitability.refusal(question);
        if (refusal != null) {
            session.setPhase(Phase.REFUSED);
            session.setCurrentQuestion("");
            session.setRefusal(refusal);
            sessions.put(id, session);
            save();
            return session;
        }
        session.setPhase(Phase.ASK);
        session.setCurrentQuestion(WorkshopConstants.FIXED_QUESTIONS.get(Slot.INPUT));
        sessions.put(id, session);
        return keep(session);
    }

    public synchronized Session translate(String labQuestion, List<Translator.Mark> marks) {
        Session session = start(labQuestion);
        if (session.getPhase() == Phase.REFUSED) {
            return session;
        }
        List<String> texts = Translator.assemble(labQuestion, marks);
        List<PseudoLine> lines = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            lines.add(new PseudoLine(nextLineId(session, i), texts.get(i), 0));
        }
        session.setConfirmedLines(lines);
        session.setProposedLines(new ArrayList<>());
        session.setSlot(Slot.OUTPUT);
        preparePuzzle(session);
        session.setPhase(Phase.ARRANGE);
        session.setCurrentQuestion("");
        return keep(session);
    }

    public synchronized Session answer(String id, String rawAnswer, boolean dontKnow) {
        Session session = require(id);
        if (session.getPhase() != Phase.ASK) {
            return session;
        }
        String answer = rawAnswer == null ? "" : rawAnswer.trim();
        if (dontKnow || Checker.isDontKnow(answer)) {
            if (session.getCurrentQuestion() != null && !session.getCurrentQuestion().isBlank()) {
                session.getAskedQuestions().add(session.getCurrentQuestion());
            }
            session.setDontKnowCount(session.getDontKnowCount() + 1);
            session.setCurrentQuestion(narrowerQuestion(session));
            return keep(session);
        }
        Grader.Grade grade = gradeAnswer(session, answer);
        if (!grade.correct()) {
            session.setWrongAttempts(session.getWrongAttempts() + 1);
            int left = WorkshopConstants.MAX_ANSWER_TRIES - session.getWrongAttempts();
            if (left > 0) {
                session.setVerdict("wrong");
                session.setFeedback("That answer is wrong. You have " + left
                        + (left == 1 ? " try" : " tries") + " left.");
                session.setExplanation(null);
                session.setCorrectAnswer(null);
                session.setPhase(Phase.ASK);
                return keep(session);
            }
            session.setVerdict("revealed");
            session.setCorrectAnswer(grade.expected());
            session.setExplanation(grade.explanation());
            session.setFeedback("That answer is still wrong. Here is the correct answer.");
            proposeLines(session, grade.expected());
            attachReveal(session, answer, grade);
            return confirm(id, session.getProposedLines());
        }
        session.setWrongAttempts(0);
        session.setVerdict("ok");
        session.setFeedback(null);
        session.setExplanation(null);
        session.setCorrectAnswer(null);
        proposeLines(session, answer);
        markLastTurnCorrect(session);
        return confirm(id, session.getProposedLines());
    }

    private void proposeLines(Session session, String answer) {
        List<String> texts = phraseLines(session, answer);
        List<PseudoLine> proposed = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            proposed.add(new PseudoLine(nextLineId(session, i), texts.get(i), 0));
        }
        session.setProposedLines(proposed);
        session.setPhase(Phase.CONFIRM);
        session.getTurns().add(new Turn(session.getSlot(), session.getCurrentQuestion(), answer, proposed));
    }

    private static void markLastTurnCorrect(Session session) {
        if (session.getTurns().isEmpty()) {
            return;
        }
        Turn last = session.getTurns().get(session.getTurns().size() - 1);
        last.setPassed(true);
        last.setRevealed(false);
    }

    private static void attachReveal(Session session, String studentAnswer, Grader.Grade grade) {
        if (session.getTurns().isEmpty()) {
            return;
        }
        Turn last = session.getTurns().get(session.getTurns().size() - 1);
        last.setAnswer(studentAnswer);
        last.setPassed(false);
        last.setRevealed(true);
        Grader.Grade rules = Grader.fromRules(session.getSlot(), studentAnswer, session.getLabQuestion());
        String expected = grade.expected();
        if (expected == null || expected.isBlank()) {
            expected = rules.expected();
        }
        last.setCorrectAnswer(expected);
        last.setExplanation(rules.explanation());
    }

    public synchronized Session confirm(String id, List<PseudoLine> lines) {
        Session session = require(id);
        if (session.getPhase() != Phase.CONFIRM) {
            return session;
        }
        List<String> texts = new ArrayList<>();
        if (lines != null) {
            for (PseudoLine line : lines) {
                texts.add(line.getText() == null ? "" : line.getText().trim());
            }
        }
        List<String> cleaned = Phrasing.sanitizeLines(texts);
        if (cleaned.isEmpty() || !Phrasing.usableForSlot(session.getSlot(), cleaned)) {
            List<String> prior = session.getConfirmedLines().stream().map(PseudoLine::getText).toList();
            String lastAnswer = session.getTurns().isEmpty()
                    ? ""
                    : session.getTurns().get(session.getTurns().size() - 1).getAnswer();
            cleaned = Phrasing.phraseFromRules(session.getSlot(), lastAnswer, session.getLabQuestion(), prior);
        }
        if (cleaned.isEmpty()) {
            return session;
        }
        List<PseudoLine> confirmed = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i++) {
            String lineId = lines != null && i < lines.size() && lines.get(i).getId() != null
                    ? lines.get(i).getId()
                    : nextLineId(session, 0);
            confirmed.add(new PseudoLine(lineId, cleaned.get(i), 0));
        }
        session.getConfirmedLines().addAll(confirmed);
        session.setProposedLines(new ArrayList<>());
        session.setDontKnowCount(0);
        session.setAskedQuestions(new ArrayList<>());
        session.setWrongAttempts(0);
        clearVerdict(session);

        String lastAnswer = session.getTurns().isEmpty()
                ? ""
                : session.getTurns().get(session.getTurns().size() - 1).getAnswer();
        if (session.getSlot() == Slot.STRATEGY) {
            session.setHasRepetition(Checker.detectRepetition(lastAnswer, confirmed));
        }
        if (session.getSlot() == Slot.MEMORY
                && session.getMemoryFollowUps() < WorkshopConstants.MEMORY_FOLLOW_UPS.size()
                && Checker.memoryAnswerIsThin(lastAnswer)) {
            session.setCurrentQuestion(WorkshopConstants.MEMORY_FOLLOW_UPS.get(session.getMemoryFollowUps()));
            session.setMemoryFollowUps(session.getMemoryFollowUps() + 1);
            session.setPhase(Phase.ASK);
            return keep(session);
        }
        Slot upcoming = nextSlotAfter(session.getSlot(), session.getHasRepetition());
        if (upcoming == null) {
            applyGeneratedPlan(session);
            preparePuzzle(session);
            session.setPhase(Phase.ARRANGE);
            session.setCurrentQuestion("");
            return keep(session);
        }
        session.setSlot(upcoming);
        session.setPhase(Phase.ASK);
        session.setCurrentQuestion(nextQuestion(session));
        return keep(session);
    }

    public synchronized Session arrange(String id, List<PseudoLine> lines) {
        Session session = require(id);
        if (session.getPhase() != Phase.ARRANGE && session.getPhase() != Phase.DONE) {
            return session;
        }
        Map<String, PseudoLine> allowed = new LinkedHashMap<>();
        for (PseudoLine line : session.getConfirmedLines()) {
            allowed.put(line.getId(), line);
        }
        List<PseudoLine> ordered = new ArrayList<>();
        if (lines != null) {
            for (PseudoLine line : lines) {
                PseudoLine known = allowed.get(line.getId());
                if (known == null) {
                    continue;
                }
                int indent = Math.max(0, Math.min(4, line.getIndent()));
                ordered.add(new PseudoLine(known.getId(), known.getText(), indent));
            }
        }
        List<PseudoLine> expected = Parsons.withCanonicalIndent(session.getConfirmedLines());
        if (ordered.size() != expected.size() || !Parsons.isPermutation(expected, ordered)) {
            return session;
        }
        session.setPuzzleLines(ordered);
        if (!Parsons.matches(expected, ordered)) {
            session.setVerdict("wrong");
            if (Parsons.orderMatches(expected, ordered)) {
                session.setFeedback(
                        "The line order is right. Indent only the line under each FOR (the IF). Leave UNTIL at the left.");
            } else {
                session.setFeedback(
                        "Start with INPUT, then SET, then FOR, then IF, then UNTIL, then RETURN. Put OUTPUT last.");
            }
            return keep(session);
        }
        session.setConfirmedLines(ordered);
        session.setPhase(Phase.DONE);
        session.setVerdict("ok");
        session.setFeedback(null);
        return keep(session);
    }

    private Session keep(Session session) {
        save();
        return session;
    }

    private Session require(String id) {
        Session session = sessions.get(id);
        if (session == null) {
            throw new WorkshopException(404, "Session not found");
        }
        if (session.getPhase() == Phase.REFUSED) {
            throw new WorkshopException(400, WorkshopConstants.REFUSAL_MESSAGE);
        }
        return session;
    }

    private String nextLineId(Session session, int extra) {
        int n = session.getConfirmedLines().size() + session.getProposedLines().size() + extra + 1;
        return "L" + session.getId().substring(0, 8) + "-" + n;
    }

    private Slot nextSlotAfter(Slot slot, Boolean hasRepetition) {
        if (slot == Slot.INPUT) {
            return Slot.STRATEGY;
        }
        if (slot == Slot.STRATEGY) {
            return Boolean.FALSE.equals(hasRepetition) ? Slot.OUTPUT : Slot.MEMORY;
        }
        if (slot == Slot.MEMORY) {
            return Slot.STOP;
        }
        if (slot == Slot.STOP) {
            return Slot.OUTPUT;
        }
        return null;
    }

    private Grader.Grade gradeAnswer(Session session, String answer) {
        Grader.Grade fallback = Grader.fromRules(session.getSlot(), answer, session.getLabQuestion());
        String raw = complete(LlmPrompts.GRADE_SYSTEM,
                "Lab question:\n" + session.getLabQuestion()
                        + "\n\nSlot: " + session.getSlot().value()
                        + "\n\nCurrent question:\n" + session.getCurrentQuestion()
                        + "\n\nStudent answer:\n" + answer);
        Grader.Grade parsed = LlmJson.parseGrade(raw);
        if (parsed != null) {
            session.setLlmMode(reportedMode);
            if (fallback.correct() && !parsed.correct()) {
                return fallback;
            }
            return parsed.withFallback(fallback);
        }
        return fallback;
    }

    private static void clearVerdict(Session session) {
        session.setVerdict(null);
        session.setFeedback(null);
        session.setExplanation(null);
        session.setCorrectAnswer(null);
    }

    private String narrowerQuestion(Session session) {
        List<String> asked = session.getAskedQuestions();
        String already = asked.isEmpty() ? "(none)" : String.join("\n", asked);
        String raw = complete(LlmPrompts.NARROW_SYSTEM,
                "Lab question:\n" + session.getLabQuestion()
                        + "\n\nCurrent decision (" + session.getSlot().value() + "):\n"
                        + session.getCurrentQuestion()
                        + "\n\nQuestions already asked. Do not repeat them:\n"
                        + already
                        + "\n\nThe student said they do not know. Ask the same decision in new, simpler words.");
        String question = LlmJson.parseQuestion(raw);
        if (acceptNarrow(session.getSlot(), question, asked)) {
            session.setLlmMode(reportedMode);
            return question;
        }
        session.setUsedFallbackQuestion(true);
        if (raw == null) {
            session.setLlmMode(LlmMode.RULES);
        }
        return nextFallbackQuestion(session.getSlot(), asked);
    }

    private static boolean acceptNarrow(Slot slot, String question, List<String> asked) {
        return question != null
                && Checker.staysOnSlot(slot, question)
                && !Checker.alreadyAsked(question, asked);
    }

    private static String nextFallbackQuestion(Slot slot, List<String> asked) {
        List<String> ladder = WorkshopConstants.NARROW_LADDERS.getOrDefault(slot, List.of());
        for (String candidate : ladder) {
            if (!Checker.alreadyAsked(candidate, asked)) {
                return candidate;
            }
        }
        return ladder.isEmpty() ? WorkshopConstants.NARROW_QUESTIONS.get(slot) : ladder.get(ladder.size() - 1);
    }

    private List<String> phraseLines(Session session, String answer) {
        List<String> prior = session.getConfirmedLines().stream().map(PseudoLine::getText).toList();
        List<String> fallback = Phrasing.phraseFromRules(session.getSlot(), answer, session.getLabQuestion(), prior);
        String history = session.getTurns().isEmpty()
                ? "(none)"
                : String.join("\n\n", session.getTurns().stream()
                        .map(turn -> turn.getQuestion() + "\nStudent: " + turn.getAnswer())
                        .toList());
        String raw = complete(LlmPrompts.PHRASE_SYSTEM,
                "Lab question:\n" + session.getLabQuestion()
                        + "\n\nEarlier decisions:\n" + history
                        + "\n\nCurrent question:\n" + session.getCurrentQuestion()
                        + "\n\nStudent answer:\n" + answer);
        List<String> parsed = Phrasing.useConcreteNames(
                Phrasing.sanitizeLines(LlmJson.parseLines(raw)), session.getLabQuestion());
        if (Phrasing.usableForSlot(session.getSlot(), parsed) && !Phrasing.hasVagueNames(parsed)) {
            session.setLlmMode(reportedMode);
            return parsed;
        }
        session.setLlmMode(LlmMode.RULES);
        session.setUsedFallbackQuestion(true);
        return fallback;
    }

    private void preparePuzzle(Session session) {
        session.setConfirmedLines(Parsons.withCanonicalIndent(session.getConfirmedLines()));
        session.setPuzzleLines(Parsons.shuffle(session.getConfirmedLines()));
    }

    private void applyGeneratedPlan(Session session) {
        String history = session.getTurns().isEmpty()
                ? "(none)"
                : String.join("\n\n", session.getTurns().stream()
                        .map(turn -> turn.getSlot().value() + " — " + turn.getQuestion()
                                + "\nStudent: " + turn.getAnswer()
                                + "\nDraft: " + String.join("; ",
                                        turn.getLines().stream().map(PseudoLine::getText).toList()))
                        .toList());
        String soFar = String.join("\n", session.getConfirmedLines().stream().map(PseudoLine::getText).toList());
        String raw = complete(LlmPrompts.PLAN_SYSTEM,
                "Lab question:\n" + session.getLabQuestion()
                        + "\n\nDecisions:\n" + history
                        + "\n\nDraft lines so far:\n" + soFar);
        List<String> generated = Phrasing.useConcreteNames(
                Phrasing.sanitizeLines(LlmJson.parseLines(raw)), session.getLabQuestion());
        if (Phrasing.usablePlan(generated)) {
            List<PseudoLine> lines = new ArrayList<>();
            for (int i = 0; i < generated.size(); i++) {
                lines.add(new PseudoLine(nextLineId(session, i), generated.get(i), 0));
            }
            session.setConfirmedLines(lines);
            session.setLlmMode(reportedMode);
            return;
        }
        ensureIo(session);
    }

    private void ensureIo(Session session) {
        List<String> texts = session.getConfirmedLines().stream().map(PseudoLine::getText).toList();
        boolean hasInput = texts.stream().anyMatch(text -> text.startsWith("INPUT "));
        boolean hasOutput = texts.stream().anyMatch(text -> text.startsWith("OUTPUT "));
        if (hasInput && hasOutput) {
            return;
        }
        String inputAnswer = answerFor(session, Slot.INPUT);
        String outputAnswer = answerFor(session, Slot.OUTPUT);
        List<PseudoLine> lines = new ArrayList<>(session.getConfirmedLines());
        int extra = 0;
        if (!hasInput) {
            List<String> input = Phrasing.phraseFromRules(Slot.INPUT, inputAnswer, session.getLabQuestion(), List.of());
            lines.add(0, new PseudoLine(nextLineId(session, extra++), input.get(0), 0));
        }
        if (!hasOutput) {
            List<String> output = Phrasing.phraseFromRules(Slot.OUTPUT, outputAnswer, session.getLabQuestion(), List.of());
            lines.add(new PseudoLine(nextLineId(session, extra), output.get(0), 0));
        }
        session.setConfirmedLines(lines);
    }

    private static String answerFor(Session session, Slot slot) {
        return session.getTurns().stream()
                .filter(turn -> turn.getSlot() == slot)
                .map(Turn::getAnswer)
                .filter(answer -> answer != null && !answer.isBlank())
                .findFirst()
                .orElse("");
    }

    private String nextQuestion(Session session) {
        String fallback = WorkshopConstants.FIXED_QUESTIONS.get(session.getSlot());
        String history = String.join("\n", session.getTurns().stream()
                .map(turn -> turn.getSlot().value() + ": " + turn.getAnswer() + " → "
                        + String.join("; ", turn.getLines().stream().map(PseudoLine::getText).toList()))
                .toList());
        String raw = complete(LlmPrompts.NEXT_QUESTION_SYSTEM,
                "Lab question:\n" + session.getLabQuestion()
                        + "\n\nDecisions so far:\n" + history
                        + "\n\nAsk about the " + session.getSlot().value() + " decision next.");
        String question = LlmJson.parseQuestion(raw);
        if (question != null && Checker.staysOnSlot(session.getSlot(), question)) {
            session.setLlmMode(reportedMode);
            return question;
        }
        session.setUsedFallbackQuestion(true);
        if (raw == null) {
            session.setLlmMode(LlmMode.RULES);
        }
        return fallback;
    }

    private String complete(String system, String user) {
        if (completer == null) {
            return null;
        }
        try {
            return completer.complete(system, user);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void load() {
        if (persistFile == null || !Files.exists(persistFile)) {
            return;
        }
        try {
            List<Session> stored = mapper.readValue(persistFile.toFile(), new TypeReference<>() {
            });
            for (Session session : stored) {
                if (session.getId() != null) {
                    sessions.put(session.getId(), session);
                }
            }
        } catch (Exception ignored) {
            // first run
        }
    }

    private void save() {
        if (persistFile == null) {
            return;
        }
        try {
            Files.createDirectories(persistFile.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(persistFile.toFile(), sessions.values());
        } catch (IOException ignored) {
            // prototype store
        }
    }
}
