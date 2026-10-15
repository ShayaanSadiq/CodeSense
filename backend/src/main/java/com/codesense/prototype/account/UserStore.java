package com.codesense.prototype.account;

import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.Session;
import java.util.List;

public interface UserStore {

    void saveAccount(Account account);

    Account findAccountById(String id);

    Account findAccountByUsername(String username);

    Account findAccountByToken(String token);

    void saveSession(Session session);

    Session findSession(String id);

    List<Session> loadSessions();
}
