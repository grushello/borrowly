package com.borrowly.exception;

public class CannotFavoriteOwnItemException extends RuntimeException {

    public CannotFavoriteOwnItemException() {
        super("You cannot favorite your own item");
    }
}