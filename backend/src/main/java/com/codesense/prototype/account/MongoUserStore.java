package com.codesense.prototype.account;

import com.codesense.prototype.config.CodeSenseProperties;
import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.workshop.WorkshopException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Repository
public class MongoUserStore implements UserStore {

    private static final Logger LOG = LoggerFactory.getLogger(MongoUserStore.class);
    private final MongoTemplate mongo;
    private final ObjectMapper mapper;
    private final Path dataDir;

    public MongoUserStore(MongoTemplate mongo, ObjectMapper mapper, CodeSenseProperties properties) {
        this.mongo = mongo;
        this.mapper = mapper;
        this.dataDir = properties.dataPath();
        ensureIndexes();
        migrateJsonIfEmpty();
    }

    private void ensureIndexes() {
        try {
            mongo.indexOps(Account.class).createIndex(
                    new Index().on("username", Sort.Direction.ASC).unique());
            mongo.indexOps(Account.class).createIndex(
                    new Index().on("token", Sort.Direction.ASC).sparse());
        } catch (Exception error) {
            LOG.warn("Could not create MongoDB indexes: {}", error.getMessage());
        }
    }

    private void migrateJsonIfEmpty() {
        try {
            if (mongo.getCollection("accounts").countDocuments() == 0) {
                Path file = dataDir.resolve("accounts.json");
                if (Files.exists(file)) {
                    List<Account> loaded = mapper.readValue(file.toFile(), new TypeReference<List<Account>>() {
                    });
                    if (loaded != null) {
                        for (Account account : loaded) {
                            if (account.getId() != null && !account.getId().isBlank()) {
                                mongo.save(account);
                            }
                        }
                        LOG.info("Moved {} accounts from accounts.json into MongoDB.", loaded.size());
                    }
                }
            }
            if (mongo.getCollection("sessions").countDocuments() == 0) {
                Path file = dataDir.resolve("sessions.json");
                if (Files.exists(file)) {
                    List<Session> loaded = mapper.readValue(file.toFile(), new TypeReference<List<Session>>() {
                    });
                    if (loaded != null) {
                        for (Session session : loaded) {
                            saveSession(session);
                        }
                        LOG.info("Moved {} lab sessions from sessions.json into MongoDB.", loaded.size());
                    }
                }
            }
        } catch (Exception error) {
            LOG.warn("Could not migrate JSON files into MongoDB: {}", error.getMessage());
        }
    }

    @Override
    public void saveAccount(Account account) {
        if (account == null || account.getId() == null || account.getId().isBlank()) {
            return;
        }
        try {
            mongo.save(account);
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    @Override
    public Account findAccountById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return mongo.findById(id, Account.class);
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    @Override
    public Account findAccountByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            return mongo.findOne(new Query(Criteria.where("username").is(username)), Account.class);
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    @Override
    public Account findAccountByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return mongo.findOne(new Query(Criteria.where("token").is(token)), Account.class);
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    @Override
    public void saveSession(Session session) {
        if (session == null || session.getId() == null || session.getId().isBlank()) {
            return;
        }
        try {
            mongo.save(session);
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    @Override
    public Session findSession(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return mongo.findById(id, Session.class);
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    @Override
    public List<Session> loadSessions() {
        try {
            List<Session> found = mongo.findAll(Session.class);
            return found == null ? new ArrayList<>() : found;
        } catch (Exception error) {
            throw new WorkshopException(503, mongoDown(error));
        }
    }

    private String mongoDown(Exception error) {
        LOG.warn("MongoDB is not reachable: {}", error.toString());
        return "MongoDB is not reachable. Start it with docker compose, then try again.";
    }
}
