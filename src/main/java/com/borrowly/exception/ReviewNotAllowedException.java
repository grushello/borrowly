package com.borrowly.exception;

public class ReviewNotAllowedException extends RuntimeException {

    public ReviewNotAllowedException(String reason) {
        super(reason);
    }
}