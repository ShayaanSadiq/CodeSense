package com.codesense.prototype.web;

import com.codesense.prototype.model.Phase;
import com.codesense.prototype.model.Session;
import com.codesense.prototype.web.dto.AnswerRequest;
import com.codesense.prototype.web.dto.ErrorResponse;
import com.codesense.prototype.web.dto.LinesRequest;
import com.codesense.prototype.web.dto.StartRequest;
import com.codesense.prototype.workshop.WorkshopService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workshop")
public class WorkshopController {

    private final WorkshopService workshop;

    public WorkshopController(WorkshopService workshop) {
        this.workshop = workshop;
    }

    @PostMapping
    public ResponseEntity<Session> start(@RequestBody StartRequest request) {
        Session session = workshop.start(request.getQuestion());
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
    public Session answer(@PathVariable String id, @RequestBody AnswerRequest request) {
        return workshop.answer(id, request.getAnswer(), request.isDontKnow());
    }

    @PostMapping("/{id}/confirm")
    public Session confirm(@PathVariable String id, @RequestBody LinesRequest request) {
        return workshop.confirm(id, request.getLines());
    }

    @PostMapping("/{id}/arrange")
    public Session arrange(@PathVariable String id, @RequestBody LinesRequest request) {
        return workshop.arrange(id, request.getLines());
    }
}
