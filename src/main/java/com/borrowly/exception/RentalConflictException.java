package com.borrowly.exception;

public class RentalConflictException extends RuntimeException {

    public RentalConflictException(String message) {
        super(message);
    }
}