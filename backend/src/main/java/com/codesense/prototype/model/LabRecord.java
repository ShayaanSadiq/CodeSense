package com.codesense.prototype.model;

public class LabRecord {

    private String sessionId = "";
    private String labQuestion = "";
    private String language = "python";
    private String puzzleKind = "path";
    private boolean planDone;
    private boolean codeDone;
    private int predictCorrect;
    private int predictTotal;
    private String updated = "";

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId == null ? "" : sessionId;
    }

    public String getLabQuestion() {
        return labQuestion;
    }

    public void setLabQuestion(String labQuestion) {
        this.labQuestion = labQuestion == null ? "" : labQuestion;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language == null || language.isBlank() ? "python" : language;
    }

    public String getPuzzleKind() {
        return puzzleKind;
    }

    public void setPuzzleKind(String puzzleKind) {
        this.puzzleKind = puzzleKind == null ? "path" : puzzleKind;
    }

    public boolean isPlanDone() {
        return planDone;
    }

    public void setPlanDone(boolean planDone) {
        this.planDone = planDone;
    }

    public boolean isCodeDone() {
        return codeDone;
    }

    public void setCodeDone(boolean codeDone) {
        this.codeDone = codeDone;
    }

    public int getPredictCorrect() {
        return predictCorrect;
    }

    public void setPredictCorrect(int predictCorrect) {
        this.predictCorrect = Math.max(0, predictCorrect);
    }

    public int getPredictTotal() {
        return predictTotal;
    }

    public void setPredictTotal(int predictTotal) {
        this.predictTotal = Math.max(0, predictTotal);
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated == null ? "" : updated;
    }
}
