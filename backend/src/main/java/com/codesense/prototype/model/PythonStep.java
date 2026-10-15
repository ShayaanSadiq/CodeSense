package com.codesense.prototype.model;

public class PythonStep {

    private String id;
    private String lineId;
    private String prompt;
    private String expected;
    private String reveal;
    private String originalPrompt;
    private String lastGuess;
    private int easierLevel;
    private boolean passed;
    private boolean revealed;

    public PythonStep() {
    }

    public PythonStep(String id, String lineId, String prompt, String expected, String reveal) {
        this.id = id;
        this.lineId = lineId;
        this.prompt = prompt;
        this.expected = expected;
        this.reveal = reveal;
        this.originalPrompt = prompt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLineId() {
        return lineId;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected;
    }

    public String getReveal() {
        return reveal;
    }

    public void setReveal(String reveal) {
        this.reveal = reveal;
    }

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public void setOriginalPrompt(String originalPrompt) {
        this.originalPrompt = originalPrompt;
    }

    public String getLastGuess() {
        return lastGuess;
    }

    public void setLastGuess(String lastGuess) {
        this.lastGuess = lastGuess;
    }

    public int getEasierLevel() {
        return easierLevel;
    }

    public void setEasierLevel(int easierLevel) {
        this.easierLevel = easierLevel;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }
}
