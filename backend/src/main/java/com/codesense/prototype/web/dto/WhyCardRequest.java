package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class WhyCardRequest {

    @Size(max = 4000)
    private String question;
    @Size(max = 40)
    private String construct;
    @Size(max = 400)
    private String phrase;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getConstruct() {
        return construct;
    }

    public void setConstruct(String construct) {
        this.construct = construct;
    }

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }
}
