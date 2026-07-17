package com.borrowly.exception;

import java.util.UUID;

public class RentalNotFoundException extends RuntimeException {

    public RentalNotFoundException(UUID rentalId) {
        super("Rental not found: " + rentalId);
    }
}
