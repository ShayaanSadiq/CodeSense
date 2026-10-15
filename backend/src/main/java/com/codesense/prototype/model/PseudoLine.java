package com.codesense.prototype.model;

public class PseudoLine {

    private String id;
    private String text;
    private int indent;

    public PseudoLine() {
    }

    public PseudoLine(String id, String text, int indent) {
        this.id = id;
        this.text = text;
        this.indent = indent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getIndent() {
        return indent;
    }

    public void setIndent(int indent) {
        this.indent = indent;
    }
}
