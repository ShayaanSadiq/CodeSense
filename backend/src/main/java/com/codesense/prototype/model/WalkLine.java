package com.codesense.prototype.model;

public class WalkLine {

    private String code = "";
    private String heading;
    private String body;
    private String needed = "";

    public WalkLine() {
    }

    public WalkLine(String heading, String body) {
        this("", heading, body, "");
    }

    public WalkLine(String code, String heading, String body, String needed) {
        this.code = code == null ? "" : code;
        this.heading = heading;
        this.body = body;
        this.needed = needed == null ? "" : needed;
    }

    public String getCode() {
        return code == null ? "" : code;
    }

    public void setCode(String code) {
        this.code = code == null ? "" : code;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getNeeded() {
        return needed == null ? "" : needed;
    }

    public void setNeeded(String needed) {
        this.needed = needed == null ? "" : needed;
    }
}
