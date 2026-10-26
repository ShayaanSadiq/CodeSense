package com.codesense.prototype.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codesense.prototype.config.CodeSenseProperties;
import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.PythonStep;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.workshop.WorkshopException;
import java.nio.file.Files;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AccountServiceTest {

    @Test
    void signupLoginAndRecordLab() throws Exception {
        var dir = Files.createTempDirectory("codesense-acc-");
        var properties = new CodeSenseProperties();
        properties.setDataDir(dir.toString());
        var accounts = new AccountService(new ObjectMapper(), properties);

        Account created = accounts.signup("Ada", "ada", "secret");
        assertEquals("Ada", created.getName());
        assertTrue(created.getToken().length() > 8);
        assertTrue(created.getPasswordHash().isEmpty());

        Account again = accounts.login("ada", "secret");
        assertEquals(created.getId(), again.getId());

        Session session = new Session();
        session.setId("lab-1");
        session.setLabQuestion("Find the largest number in a list.");
        session.setPhase(Phase.PYTHON);
        session.setCodeLanguage("java");
        PythonStep step = new PythonStep();
        step.setPassed(true);
        session.setPythonTrace(List.of(step));
        session.setPredictIndex(1);
        accounts.record(again.getToken(), session);

        Account stored = accounts.me(again.getToken());
        assertEquals(1, stored.getLabs().size());
        assertEquals("java", stored.getLabs().get(0).getLanguage());
        assertTrue(stored.getLabs().get(0).isCodeDone());
    }

    @Test
    void rejectsOddUsernames() throws Exception {
        var dir = Files.createTempDirectory("codesense-acc-");
        var properties = new CodeSenseProperties();
        properties.setDataDir(dir.toString());
        var accounts = new AccountService(new ObjectMapper(), properties);
        assertThrows(WorkshopException.class, () -> accounts.signup("Ada", "a", "secret"));
        assertThrows(WorkshopException.class, () -> accounts.signup("Ada", "ada space", "secret"));
    }

    @Test
    void rejectsTakenUsernameAndBadPassword() throws Exception {
        var dir = Files.createTempDirectory("codesense-acc-");
        var properties = new CodeSenseProperties();
        properties.setDataDir(dir.toString());
        var accounts = new AccountService(new ObjectMapper(), properties);
        accounts.signup("Ada", "ada", "secret");
        assertThrows(WorkshopException.class, () -> accounts.signup("Ada", "ada", "other"));
        assertThrows(WorkshopException.class, () -> accounts.login("ada", "nope"));
    }
}
