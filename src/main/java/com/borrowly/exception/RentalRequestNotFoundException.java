package com.borrowly.exception;

import java.util.UUID;

public class RentalRequestNotFoundException extends RuntimeException {

    public RentalRequestNotFoundException(UUID id) {
        super("Rental request not found: " + id);
    }
}