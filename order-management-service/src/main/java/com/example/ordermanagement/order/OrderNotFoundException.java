package com.example.ordermanagement.order;

import com.example.ordermanagement.exception.ResourceNotFoundException;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(Long id) {
        super("Order with id " + id + " was not found");
    }

    public OrderNotFoundException(String orderNumber) {
        super("Order with number " + orderNumber + " was not found");
    }
}
