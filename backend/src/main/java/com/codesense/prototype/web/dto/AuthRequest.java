package com.codesense.prototype.web.dto;

import jakarta.validation.constraints.Size;

public class AuthRequest {

    @Size(max = 80)
    private String name = "";
    @Size(max = 32)
    private String username = "";
    @Size(max = 128)
    private String password = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
