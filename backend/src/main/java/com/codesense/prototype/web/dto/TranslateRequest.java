package com.codesense.prototype.web.dto;

import java.util.ArrayList;
import java.util.List;

public class TranslateRequest {

    private String question;
    private String kind;
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
        private String word;
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
