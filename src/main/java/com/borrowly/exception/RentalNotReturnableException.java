package com.borrowly.exception;

import com.borrowly.model.rental.RentalStatus;

import java.time.LocalDate;
import java.util.UUID;

public class RentalNotReturnableException extends RuntimeException {

    public RentalNotReturnableException(UUID rentalId, RentalStatus status) {
        super("Rental " + rentalId + " cannot be returned while it is " + status);
    }

    public RentalNotReturnableException(UUID rentalId, LocalDate startDate) {
        super("Rental " + rentalId + " cannot be returned before it starts on " + startDate);
    }
}
