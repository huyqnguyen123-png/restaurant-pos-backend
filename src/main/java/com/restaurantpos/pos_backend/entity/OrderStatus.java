package com.restaurantpos.pos_backend.entity;

public enum OrderStatus {
    OPEN,   // order is still open, not fully paid yet
    PAID    // order has been fully paid
}