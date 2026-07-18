package com.restaurantpos.pos_backend.exception;

// Thrown for business-rule violations, e.g. missing tableNumber for a dine-in order,
// or trying to modify an order that has already been paid.
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}