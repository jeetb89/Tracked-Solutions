package com.example.ordermanagement.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String email);

    List<Order> findByStatus(OrderStatus status);
}
