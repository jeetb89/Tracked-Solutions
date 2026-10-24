package com.example.ordermanagement.order.dto;

import com.example.ordermanagement.order.Order;
import com.example.ordermanagement.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    private String orderNumber;
    private String customerEmail;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public OrderResponse(String orderNumber, String customerEmail, OrderStatus status,
                         BigDecimal totalAmount, LocalDateTime createdAt,
                         List<OrderItemResponse> items) {
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.items = items;
    }

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProductCode(), item.getUnitPrice(), item.getQuantity()))
                .toList();
        return new OrderResponse(order.getOrderNumber(), order.getCustomerEmail(),
                order.getStatus(), order.getTotalAmount(), order.getCreatedAt(), items);
    }

    public String getOrderNumber() { return orderNumber; }
    public String getCustomerEmail() { return customerEmail; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<OrderItemResponse> getItems() { return items; }
}
