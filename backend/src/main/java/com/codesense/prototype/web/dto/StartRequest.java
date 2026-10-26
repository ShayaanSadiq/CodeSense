package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class StartRequest {

    @Size(max = 4000)
    private String question;
    @Size(max = 32)
    private String kind;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}
