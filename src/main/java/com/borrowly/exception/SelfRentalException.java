package com.borrowly.exception;

public class SelfRentalException extends RuntimeException {

    public SelfRentalException() {
        super("You cannot rent your own item");
    }
}