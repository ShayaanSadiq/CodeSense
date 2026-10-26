package com.codesense.prototype.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class TranslateRequest {

    @Size(max = 4000)
    private String question;
    @Size(max = 32)
    private String kind;
    @Valid
    @Size(max = 50)
    private List<Mark> marks = new ArrayList<>();

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

    public List<Mark> getMarks() {
        return marks;
    }

    public void setMarks(List<Mark> marks) {
        this.marks = marks == null ? new ArrayList<>() : marks;
    }

    public static class Mark {
        @Size(max = 200)
        private String word;
        @Size(max = 40)
        private String construct;

        public String getWord() {
            return word;
        }

        public void setWord(String word) {
            this.word = word;
        }

        public String getConstruct() {
            return construct;
        }

        public void setConstruct(String construct) {
            this.construct = construct;
        }
    }
}
