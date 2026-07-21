package com.borrowly.exception;

import com.borrowly.model.rental.RentalStatus;

import java.util.UUID;

public class RentalNotReturnableException extends RuntimeException {

    public RentalNotReturnableException(UUID rentalId, RentalStatus status) {
        super("Rental " + rentalId + " cannot be returned while it is " + status);
    }
}
