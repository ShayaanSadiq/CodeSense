package com.codesense.prototype.model;

public class ArrangeNote {

    private String id;
    private String why;

    public ArrangeNote() {
    }

    public ArrangeNote(String id, String why) {
        this.id = id;
        this.why = why;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWhy() {
        return why;
    }

    public void setWhy(String why) {
        this.why = why;
    }
}
