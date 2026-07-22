package com.borrowly.exception;

public class DuplicateRentalRequestException extends RuntimeException {
    public DuplicateRentalRequestException() {
        super("You have already requested a rental for this item.");
    }
}
