package com.codesense.prototype.workshop;

public class WorkshopException extends RuntimeException {

    private final int status;

    public WorkshopException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
