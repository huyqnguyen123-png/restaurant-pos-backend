package com.restaurantpos.pos_backend.exception;

// Thrown when a requested entity (menu item, order, etc.) does not exist in the database
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}