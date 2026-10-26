package com.codesense.prototype.web;

import com.codesense.prototype.web.dto.ErrorResponse;
import com.codesense.prototype.workshop.WorkshopException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
        String message = error.getMessage();
        if (message == null || message.isBlank() || message.length() > 180) {
            message = "That request is not valid.";
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> invalid() {
        return ResponseEntity.badRequest().body(new ErrorResponse("That request is too large or has invalid fields."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected() {
        return ResponseEntity.internalServerError().body(new ErrorResponse("Something went wrong."));
    }
}
