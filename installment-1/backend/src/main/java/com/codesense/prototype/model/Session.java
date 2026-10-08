package com.codesense.prototype.model;

import java.util.ArrayList;
import java.util.List;

public class Session {

    private String id;
    private String labQuestion;
    private Phase phase;
    private Slot slot;
    private String currentQuestion;
    private List<PseudoLine> proposedLines = new ArrayList<>();
    private List<PseudoLine> confirmedLines = new ArrayList<>();
    private List<PseudoLine> puzzleLines = new ArrayList<>();
    private List<Turn> turns = new ArrayList<>();
    private Boolean hasRepetition;
    private int dontKnowCount;
    private List<String> askedQuestions = new ArrayList<>();
    private int memoryFollowUps;
    private boolean usedFallbackQuestion;
    private String refusal;
    private LlmMode llmMode;
    private int wrongAttempts;
    private int maxAttempts = WorkshopConstants.MAX_ANSWER_TRIES;
    private String verdict;
    private String feedback;
    private String explanation;
    private String correctAnswer;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabQuestion() {
        return labQuestion;
    }

    public void setLabQuestion(String labQuestion) {
        this.labQuestion = labQuestion;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public String getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(String currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public List<PseudoLine> getProposedLines() {
        return proposedLines;
    }

    public void setProposedLines(List<PseudoLine> proposedLines) {
        this.proposedLines = proposedLines != null ? proposedLines : new ArrayList<>();
    }

    public List<PseudoLine> getConfirmedLines() {
        return confirmedLines;
    }

    public void setConfirmedLines(List<PseudoLine> confirmedLines) {
        this.confirmedLines = confirmedLines != null ? confirmedLines : new ArrayList<>();
    }

    public List<PseudoLine> getPuzzleLines() {
        if (puzzleLines == null) {
            puzzleLines = new ArrayList<>();
        }
        return puzzleLines;
    }

    public void setPuzzleLines(List<PseudoLine> puzzleLines) {
        this.puzzleLines = puzzleLines != null ? puzzleLines : new ArrayList<>();
    }

    public List<Turn> getTurns() {
        return turns;
    }

    public void setTurns(List<Turn> turns) {
        this.turns = turns != null ? turns : new ArrayList<>();
    }

    public Boolean getHasRepetition() {
        return hasRepetition;
    }

    public void setHasRepetition(Boolean hasRepetition) {
        this.hasRepetition = hasRepetition;
    }

    public int getDontKnowCount() {
        return dontKnowCount;
    }

    public void setDontKnowCount(int dontKnowCount) {
        this.dontKnowCount = dontKnowCount;
    }

    public List<String> getAskedQuestions() {
        if (askedQuestions == null) {
            askedQuestions = new ArrayList<>();
        }
        return askedQuestions;
    }

    public void setAskedQuestions(List<String> askedQuestions) {
        this.askedQuestions = askedQuestions != null ? askedQuestions : new ArrayList<>();
    }

    public int getMemoryFollowUps() {
        return memoryFollowUps;
    }

    public void setMemoryFollowUps(int memoryFollowUps) {
        this.memoryFollowUps = memoryFollowUps;
    }

    public boolean isUsedFallbackQuestion() {
        return usedFallbackQuestion;
    }

    public void setUsedFallbackQuestion(boolean usedFallbackQuestion) {
        this.usedFallbackQuestion = usedFallbackQuestion;
    }

    public String getRefusal() {
        return refusal;
    }

    public void setRefusal(String refusal) {
        this.refusal = refusal;
    }

    public LlmMode getLlmMode() {
        return llmMode;
    }

    public void setLlmMode(LlmMode llmMode) {
        this.llmMode = llmMode;
    }

    public int getWrongAttempts() {
        return wrongAttempts;
    }

    public void setWrongAttempts(int wrongAttempts) {
        this.wrongAttempts = wrongAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}
