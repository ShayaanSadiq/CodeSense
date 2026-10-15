package com.codesense.prototype.web;

import com.codesense.prototype.account.AccountService;
import com.codesense.prototype.model.Account;
import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.Session;
import java.util.LinkedHashMap;
import java.util.List;
import com.codesense.prototype.web.dto.AnswerRequest;
import com.codesense.prototype.web.dto.CodeRequest;
import com.codesense.prototype.web.dto.ErrorResponse;
import com.codesense.prototype.web.dto.LinesRequest;
import com.codesense.prototype.web.dto.PythonRequest;
import com.codesense.prototype.web.dto.StartRequest;
import com.codesense.prototype.web.dto.TranslateRequest;
import com.codesense.prototype.web.dto.WhyCardRequest;
import com.codesense.prototype.workshop.Translator;
import com.codesense.prototype.workshop.WorkshopService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Map;
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

    private final WorkshopService workshop;
    private final AccountService accounts;

    public WorkshopController(WorkshopService workshop, AccountService accounts) {
        this.workshop = workshop;
        this.accounts = accounts;
    }

    @PostMapping
    public ResponseEntity<Session> start(
            @RequestBody StartRequest request,
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
    public Map<String, Object> whyCard(@RequestBody WhyCardRequest request) {
        Translator.CardJudge judge = workshop.judgeCard(
                request.getQuestion(), request.getConstruct(), request.getPhrase());
        return Map.of("correct", judge.correct(), "explanation", judge.explanation());
    }

    @PostMapping("/translate")
    public ResponseEntity<?> translate(
            @RequestBody TranslateRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        List<Translator.Mark> marks = request.getMarks().stream()
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
    public ResponseEntity<?> get(@PathVariable String id) {
        Session session = workshop.get(id);
        if (session == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Session not found"));
        }
        return ResponseEntity.ok(session);
    }

    @PostMapping("/{id}/answer")
    public Session answer(
            @PathVariable String id,
            @RequestBody AnswerRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        Session session = workshop.answer(id, request.getAnswer(), request.isDontKnow());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/confirm")
    public Session confirm(
            @PathVariable String id,
            @RequestBody LinesRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        Session session = workshop.confirm(id, request.getLines());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/arrange")
    public Session arrange(
            @PathVariable String id,
            @RequestBody LinesRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        Session session = workshop.arrange(id, request.getLines());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/python")
    public Session python(
            @PathVariable String id,
            @RequestBody(required = false) PythonRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        Session session = workshop.convertCode(id, request == null ? null : request.getInput(), "python");
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/code")
    public Session code(
            @PathVariable String id,
            @RequestBody(required = false) CodeRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        String input = request == null ? null : request.getInput();
        String language = request == null ? "python" : request.getLanguage();
        Session session = workshop.convertCode(id, input, language);
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/predict")
    public Session predict(
            @PathVariable String id,
            @RequestBody AnswerRequest request,
            @RequestHeader(value = "Authorization", required = false) String header) {
        Session session = workshop.predictPython(id, request.getAnswer(), request.isDontKnow());
        remember(header, session);
        return session;
    }

    @PostMapping("/{id}/run")
    public Map<String, Object> run(
            @PathVariable String id,
            @RequestBody(required = false) CodeRequest request) {
        return workshop.runCode(
                id,
                request == null ? "" : request.getStdin(),
                request == null ? null : request.getSource());
    }

    private void remember(String header, Session session) {
        Account account = accounts.findToken(header);
        if (account != null && session != null) {
            session.setOwnerId(account.getId());
            accounts.record(header, session);
        }
    }
}
