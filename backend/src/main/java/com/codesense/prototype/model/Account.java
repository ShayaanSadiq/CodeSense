package com.codesense.prototype.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "accounts")
public class Account {

    @Id
    private String id = "";
    private String name = "";
    private String username = "";
    private String passwordHash = "";
    private String token = "";
    private String created = "";
    private List<LabRecord> labs = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash == null ? "" : passwordHash;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token == null ? "" : token;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created == null ? "" : created;
    }

    public List<LabRecord> getLabs() {
        if (labs == null) {
            labs = new ArrayList<>();
        }
        return labs;
    }

    public void setLabs(List<LabRecord> labs) {
        this.labs = labs != null ? labs : new ArrayList<>();
    }
}
