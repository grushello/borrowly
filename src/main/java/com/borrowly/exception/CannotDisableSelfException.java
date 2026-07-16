package com.borrowly.exception;

public class CannotDisableSelfException extends RuntimeException {

    public CannotDisableSelfException() {
        super("An admin cannot disable their own account");
    }
}