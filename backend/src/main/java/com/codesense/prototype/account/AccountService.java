package com.codesense.prototype.account;

import com.codesense.prototype.config.CodeSenseProperties;
import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.LabRecord;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.workshop.LangConverter;
import com.codesense.prototype.workshop.WorkshopException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final UserStore store;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public AccountService(UserStore store) {
        this.store = store;
    }

    public AccountService(ObjectMapper mapper, CodeSenseProperties properties) {
        this(new MemoryUserStore());
        loadJson(mapper, properties);
    }

    public synchronized Account signup(String name, String username, String password) {
        String user = cleanUser(username);
        if (user.length() < 3) {
            throw new WorkshopException(400, "Pick a username of at least 3 letters.");
        }
        if (password == null || password.length() < 4) {
            throw new WorkshopException(400, "Pick a password of at least 4 characters.");
        }
        if (findUser(user) != null) {
            throw new WorkshopException(400, "That username is already taken.");
        }
        Account account = new Account();
        account.setId(UUID.randomUUID().toString());
        account.setName(name == null || name.isBlank() ? user : name.trim());
        account.setUsername(user);
        account.setPasswordHash(hash(password, account.getId()));
        account.setToken(newToken());
        account.setCreated(LocalDateTime.now().format(STAMP));
        store.saveAccount(account);
        return publicCopy(account);
    }

    public synchronized Account login(String username, String password) {
        Account account = findUser(cleanUser(username));
        if (account == null || !account.getPasswordHash().equals(hash(password, account.getId()))) {
            throw new WorkshopException(401, "Username or password is wrong.");
        }
        account.setToken(newToken());
        store.saveAccount(account);
        return publicCopy(account);
    }

    public synchronized Account me(String token) {
        Account account = findToken(token);
        if (account == null) {
            throw new WorkshopException(401, "Sign in to continue.");
        }
        return publicCopy(account);
    }

    public synchronized void logout(String token) {
        Account account = findToken(token);
        if (account != null) {
            account.setToken("");
            store.saveAccount(account);
        }
    }

    public synchronized Account require(String token) {
        Account account = findToken(token);
        if (account == null) {
            throw new WorkshopException(401, "Sign in to continue.");
        }
        return account;
    }

    public Account findToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String raw = token.startsWith("Bearer ") ? token.substring(7).trim() : token.trim();
        if (raw.isBlank()) {
            return null;
        }
        return store.findAccountByToken(raw);
    }

    public synchronized void record(String token, Session session) {
        Account account = findToken(token);
        if (account == null || session == null || session.getLabQuestion() == null) {
            return;
        }
        String language = LangConverter.normalize(session.getCodeLanguage());
        LabRecord match = null;
        for (LabRecord row : account.getLabs()) {
            if (session.getLabQuestion().equalsIgnoreCase(row.getLabQuestion())
                    && language.equals(LangConverter.normalize(row.getLanguage()))) {
                match = row;
                break;
            }
        }
        if (match == null) {
            match = new LabRecord();
            match.setLabQuestion(session.getLabQuestion());
            match.setLanguage(language);
            account.getLabs().add(match);
        }
        match.setSessionId(session.getId());
        match.setPuzzleKind(session.getPuzzleKind());
        match.setPlanDone(session.getPhase() != null && !"ask".equals(session.getPhase().value())
                && !"refused".equals(session.getPhase().value()));
        boolean code = "python".equals(session.getPhase() == null ? "" : session.getPhase().value())
                && session.getPythonTrace() != null
                && !session.getPythonTrace().isEmpty()
                && session.getPredictIndex() >= session.getPythonTrace().size();
        if (code) {
            match.setCodeDone(true);
        }
        int total = session.getPythonTrace() == null ? 0 : session.getPythonTrace().size();
        int correct = 0;
        if (session.getPythonTrace() != null) {
            for (var step : session.getPythonTrace()) {
                if (step.isPassed()) {
                    correct++;
                }
            }
        }
        match.setPredictTotal(total);
        match.setPredictCorrect(correct);
        match.setUpdated(LocalDateTime.now().format(STAMP));
        store.saveAccount(account);
    }

    private Account findUser(String username) {
        return store.findAccountByUsername(username);
    }

    private static Account publicCopy(Account account) {
        Account copy = new Account();
        copy.setId(account.getId());
        copy.setName(account.getName());
        copy.setUsername(account.getUsername());
        copy.setCreated(account.getCreated());
        copy.setToken(account.getToken());
        copy.setLabs(account.getLabs());
        return copy;
    }

    private String newToken() {
        return UUID.randomUUID().toString().replace("-", "") + hex(randomBytes(8));
    }

    private static String cleanUser(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((salt + ":" + (password == null ? "" : password))
                    .getBytes(StandardCharsets.UTF_8));
            return hex(hashed);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private byte[] randomBytes(int n) {
        byte[] bytes = new byte[n];
        random.nextBytes(bytes);
        return bytes;
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    private void loadJson(ObjectMapper mapper, CodeSenseProperties properties) {
        if (mapper == null || properties == null) {
            return;
        }
        var file = properties.dataPath().resolve("accounts.json");
        if (!Files.exists(file)) {
            return;
        }
        try {
            List<Account> loaded = mapper.readValue(file.toFile(), new TypeReference<List<Account>>() {
            });
            if (loaded != null) {
                for (Account account : loaded) {
                    store.saveAccount(account);
                }
            }
        } catch (Exception ignored) {
            // first run
        }
    }
}
