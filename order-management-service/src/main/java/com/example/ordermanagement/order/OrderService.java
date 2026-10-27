package com.example.ordermanagement.order;

import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.ordermanagement.order.dto.CreateOrderRequest;
import com.example.ordermanagement.order.dto.OrderItemRequest;
import com.example.ordermanagement.order.dto.OrderResponse;

import java.util.UUID;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerEmail(String email) {
        return repository.findByCustomerEmailOrderByCreatedAtDesc(email).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ordersById", key = "#id")
    public OrderResponse getOrderById(Long id) {
        return OrderResponse.from(findEntityById(id));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "ordersByNumber", key = "#orderNumber")
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
        return OrderResponse.from(order);
    }

    @Transactional
    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order();
        applyRequest(order, request);
        order.setOrderNumber(UUID.randomUUID().toString());
        return OrderResponse.from(repository.save(order));
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public OrderResponse update(Long id, CreateOrderRequest request) {
        Order order = findEntityById(id);
        applyRequest(order, request);
        return OrderResponse.from(repository.save(order));
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public OrderResponse updateByNumber(String orderNumber, CreateOrderRequest request) {
        Order order = findEntityByNumber(orderNumber);
        applyRequest(order, request);
        return OrderResponse.from(repository.save(order));
    }

    private void applyRequest(Order order, CreateOrderRequest request) {
        order.setCustomerEmail(request.getCustomerEmail());
        order.setStatus(request.getStatus() == null ? OrderStatus.PENDING : request.getStatus());

        List<OrderItem> items = request.getItems().stream()
                .map(this::toEntity)
                .toList();
        order.setItems(items);

        order.setTotalAmount(items.stream()
                .map(item -> item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
    }

    private OrderItem toEntity(OrderItemRequest request) {
        OrderItem item = new OrderItem();
        item.setProductCode(request.getProductCode());
        item.setUnitPrice(request.getUnitPrice());
        item.setQuantity(request.getQuantity());
        return item;
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public void delete(Long id) {
        repository.delete(findEntityById(id));
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public void deleteByNumber(String orderNumber) {
        repository.delete(findEntityByNumber(orderNumber));
    }

    private Order findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Order findEntityByNumber(String orderNumber) {
        return repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }
}
