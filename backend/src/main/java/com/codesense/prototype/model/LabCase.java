package com.codesense.prototype.model;

public class LabCase {

    private String input = "";
    private String expected = "";
    private String status = "Pass";

    public LabCase() {
    }

    public LabCase(String input, String expected, String status) {
        this.input = input == null ? "" : input;
        this.expected = expected == null ? "" : expected;
        this.status = status == null || status.isBlank() ? "Pass" : status;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input == null ? "" : input;
    }

    public String getExpected() {
        return expected;
    }

    public void setExpected(String expected) {
        this.expected = expected == null ? "" : expected;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null || status.isBlank() ? "Pass" : status;
    }
}
