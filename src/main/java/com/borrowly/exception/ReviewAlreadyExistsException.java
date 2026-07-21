package com.borrowly.exception;

import java.util.UUID;

public class ReviewAlreadyExistsException extends RuntimeException {

    public ReviewAlreadyExistsException(UUID rentalId) {
        super("A review already exists for rental: " + rentalId);
    }
}