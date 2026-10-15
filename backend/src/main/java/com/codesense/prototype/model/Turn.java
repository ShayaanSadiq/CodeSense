package com.codesense.prototype.model;

import java.util.ArrayList;
import java.util.List;

public class Turn {

    private Slot slot;
    private String question;
    private String answer;
    private List<PseudoLine> lines = new ArrayList<>();
    private boolean revealed;
    private boolean passed;
    private String correctAnswer;
    private String explanation;

    public Turn() {
    }

    public Turn(Slot slot, String question, String answer, List<PseudoLine> lines) {
        this.slot = slot;
        this.question = question;
        this.answer = answer;
        this.lines = lines;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) {
        this.slot = slot;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<PseudoLine> getLines() {
        return lines;
    }

    public void setLines(List<PseudoLine> lines) {
        this.lines = lines;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
