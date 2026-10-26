package com.codesense.prototype.web;

import com.codesense.prototype.account.AccountService;
import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.web.dto.AnswerRequest;
import com.codesense.prototype.web.dto.CodeRequest;
import com.codesense.prototype.web.dto.ErrorResponse;
import com.codesense.prototype.web.dto.LinesRequest;
import com.codesense.prototype.web.dto.PythonRequest;
import com.codesense.prototype.web.dto.StartRequest;
import com.codesense.prototype.web.dto.TranslateRequest;
import com.codesense.prototype.web.dto.WhyCardRequest;
import com.codesense.prototype.workshop.Translator;
import com.codesense.prototype.workshop.WorkshopException;
import com.codesense.prototype.workshop.WorkshopService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workshop")
public class WorkshopController {

    private static final int MAX_QUESTION = 4000;

    private final WorkshopService workshop;
    private final AccountService accounts;

    public WorkshopController(WorkshopService workshop, AccountService accounts) {
        this.workshop = workshop;
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<Session> start(
            @Valid @RequestBody StartRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        Session session = workshop.start(request.getQuestion(), request.getKind());
        remember(header, session);
        HttpStatus status = session.getPhase() == Phase.REFUSED ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(session);
    }

    @GetMapping("/glossary")
    public List<Translator.GlossaryEntry> glossary() {
        return Translator.glossary();
    }

    @GetMapping("/rephrase")
    public Map<String, Object> rephrase(@RequestParam String question) {
        if (question != null && question.length() > MAX_QUESTION) {
            throw new WorkshopException(400, "That question is too long.");
        }
        Translator.Voice voice = workshop.voice(question);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("text", voice.text());
        body.put("needed", voice.needed());
        body.put("unused", voice.unused());
        body.put("lines", voice.lines());
        return body;
    }

    @GetMapping("/roles")
    public List<Translator.Role> roles() {
        return Translator.roles();
    }

    @PostMapping("/why-card")
    public Map<String, Object> whyCard(@Valid @RequestBody WhyCardRequest request) {
        Translator.CardJudge judge = workshop.judgeCard(
                request.getQuestion(), request.getConstruct(), request.getPhrase());
        return Map.of("correct", judge.correct(), "explanation", judge.explanation());
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(
            @Valid @RequestBody TranslateRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        List<TranslateRequest.Mark> rawMarks =
                request.getMarks() == null ? List.of() : request.getMarks();
        List<Translator.Mark> marks = rawMarks.stream()
                .filter(Objects::nonNull)
                .map(mark -> new Translator.Mark(mark.getWord(), mark.getConstruct()))
                .toList();
        String problem = workshop.markProblem(request.getQuestion(), marks);
        if (problem != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse(problem));
        }
        Session session = workshop.translate(request.getQuestion(), marks, request.getKind());
        remember(header, session);
        HttpStatus status = session.getPhase() == Phase.REFUSED ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(session);
    }

    @GetMapping("/{id}")
    public Session get(
            @PathVariable String id,
            @RequestHeader(value = "Authorization", required = false) String header) {
        return requireAccessible(id, header);
    }

    @PostMapping("/{id}/answer")
    public Session answer(
            @PathVariable String id,
            @Valid @RequestBody AnswerRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        Session session = workshop.answer(id, request.getAnswer(), request.isDontKnow());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/confirm")
    public Session confirm(
            @PathVariable String id,
            @Valid @RequestBody LinesRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        Session session = workshop.confirm(id, request.getLines());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/arrange")
    public Session arrange(
            @PathVariable String id,
            @Valid @RequestBody LinesRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        Session session = workshop.arrange(id, request.getLines());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/python")
    public Session python(
            @PathVariable String id,
            @Valid @RequestBody(required = false) PythonRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        Session session = workshop.convertCode(id, request == null ? null : request.getInput(), "python");
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/code")
    public Session code(
            @PathVariable String id,
            @Valid @RequestBody(required = false) CodeRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        String input = request == null ? null : request.getInput();
        String language = request == null ? "python" : request.getLanguage();
        Session session = workshop.convertCode(id, input, language);
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/predict")
    public Session predict(
            @PathVariable String id,
            @Valid @RequestBody AnswerRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        Session session = workshop.predictPython(id, request.getAnswer(), request.isDontKnow());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/run")
    public Map<String, Object> run(
            @PathVariable String id,
            @Valid @RequestBody(required = false) CodeRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        requireAccessible(id, header);
        return workshop.runCode(
                id,
                request == null ? "" : request.getStdin(),
                request == null ? null : request.getSource());
    }

    private Session requireAccessible(String id, String header) {
        if (id == null || id.length() > 64 || !id.matches("[A-Za-z0-9_-]+")) {
            throw new WorkshopException(404, "Session not found");
        }
        Session session = workshop.get(id);
        if (session == null) {
            throw new WorkshopException(404, "Session not found");
        }
        String owner = session.getOwnerId();
        if (owner == null || owner.isBlank()) {
            return session;
        }
        Account account = accounts.findToken(header);
        if (account == null || !owner.equals(account.getId())) {
            throw new WorkshopException(404, "Session not found");
        }
        return session;
    }

    private void remember(String header, Session session) {
        Account account = accounts.findToken(header);
        if (account != null && session != null) {
            session.setOwnerId(account.getId());
            accounts.record(header, session);
        }
    }
}
