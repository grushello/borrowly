package com.borrowly.exception;

import java.util.UUID;

public class ItemHasActiveRentalException extends RuntimeException {
    public ItemHasActiveRentalException(UUID itemId) {
        super("Item with id " + itemId + " cannot be modified while it has an active rental");
    }
}
