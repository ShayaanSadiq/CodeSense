package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class AnswerRequest {

    @Size(max = 4000)
    private String answer;
    private boolean dontKnow;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isDontKnow() {
        return dontKnow;
    }

    public void setDontKnow(boolean dontKnow) {
        this.dontKnow = dontKnow;
    }
}
