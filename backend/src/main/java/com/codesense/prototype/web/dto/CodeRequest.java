package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class CodeRequest {

    @Size(max = 4000)
    private String input;
    @Size(max = 32)
    private String language = "python";
    @Size(max = 4000)
    private String stdin = "";
    @Size(max = 20000)
    private String source = "";

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getStdin() {
        return stdin;
    }

    public void setStdin(String stdin) {
        this.stdin = stdin;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
