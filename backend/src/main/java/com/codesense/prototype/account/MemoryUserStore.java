package com.codesense.prototype.account;

import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.Session;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MemoryUserStore implements UserStore {

    private final Map<String, Account> accounts = new LinkedHashMap<>();
    private final Map<String, Session> sessions = new LinkedHashMap<>();

    @Override
    public synchronized void saveAccount(Account account) {
        if (account == null || account.getId() == null || account.getId().isBlank()) {
            return;
        }
        accounts.put(account.getId(), account);
    }

    @Override
    public synchronized Account findAccountById(String id) {
        return id == null ? null : accounts.get(id);
    }

    @Override
    public synchronized Account findAccountByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        for (Account account : accounts.values()) {
            if (username.equals(account.getUsername())) {
                return account;
            }
        }
        return null;
    }

    @Override
    public synchronized Account findAccountByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        for (Account account : accounts.values()) {
            if (token.equals(account.getToken())) {
                return account;
            }
        }
        return null;
    }

    @Override
    public synchronized void saveSession(Session session) {
        if (session == null || session.getId() == null || session.getId().isBlank()) {
            return;
        }
        sessions.put(session.getId(), session);
    }

    @Override
    public synchronized Session findSession(String id) {
        return id == null ? null : sessions.get(id);
    }

    @Override
    public synchronized List<Session> loadSessions() {
        return new ArrayList<>(sessions.values());
    }
}
