package com.codesense.prototype.model;

public class McqOption {

    private String letter = "";
    private String text = "";

    public McqOption() {
    }

    public McqOption(String letter, String text) {
        this.letter = letter == null ? "" : letter;
        this.text = text == null ? "" : text;
    }

    public String getLetter() {
        return letter == null ? "" : letter;
    }

    public void setLetter(String letter) {
        this.letter = letter == null ? "" : letter;
    }

    public String getText() {
        return text == null ? "" : text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
    }
}
