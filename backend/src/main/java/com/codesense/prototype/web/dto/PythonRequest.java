package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class PythonRequest {

    @Size(max = 4000)
    private String input;

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }
}
