package com.example.ordermanagement.order;

import jakarta.validation.Valid;
import com.example.ordermanagement.order.dto.CreateOrderRequest;
import com.example.ordermanagement.order.dto.OrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderResponse> findAll(@RequestParam Optional<String> customerEmail) {
        return customerEmail
                .map(service::getOrdersByCustomerEmail)
                .orElseGet(service::findAll);
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> findByOrderNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(service.getOrderByNumber(orderNumber));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = service.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{orderNumber}")
    public OrderResponse update(@PathVariable String orderNumber, @Valid @RequestBody CreateOrderRequest request) {
        return service.updateByNumber(orderNumber, request);
    }

    @DeleteMapping("/{orderNumber}")
    public ResponseEntity<Void> delete(@PathVariable String orderNumber) {
        service.deleteByNumber(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
