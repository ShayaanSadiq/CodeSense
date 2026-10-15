package com.codesense.prototype.web;

import com.codesense.prototype.web.dto.ErrorResponse;
import com.codesense.prototype.workshop.WorkshopException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WorkshopException.class)
    public ResponseEntity<ErrorResponse> workshop(WorkshopException error) {
        return ResponseEntity.status(error.getStatus()).body(new ErrorResponse(error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(new ErrorResponse(error.getMessage()));
    }
}
