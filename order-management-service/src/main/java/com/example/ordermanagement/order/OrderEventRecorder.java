package com.example.ordermanagement.order;

public interface OrderEventRecorder {
    void recordCreated(String orderNumber);
    void recordUpdated(String orderNumber);
    void recordDeleted(String orderNumber);
}
