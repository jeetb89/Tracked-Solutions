package com.example.ordermanagement.order;

import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import com.example.ordermanagement.order.dto.CreateOrderRequest;
import com.example.ordermanagement.order.dto.OrderItemRequest;
import com.example.ordermanagement.order.dto.OrderResponse;

import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.time.Duration;

@Service
@Transactional
public class OrderService {

    private static final String CUSTOMER_ORDERS_CACHE_PREFIX = "orders:customer:";

    private final OrderRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration customerOrdersCacheTtl;

    public OrderService(
            OrderRepository repository,
            RedisTemplate<String, Object> redisTemplate,
            @Value("${spring.cache.redis.time-to-live:10m}") Duration customerOrdersCacheTtl) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.customerOrdersCacheTtl = customerOrdersCacheTtl;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerEmail(String email) {
        String cacheKey = customerOrdersCacheKey(email);
        Object cachedValue = redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue instanceof List<?> cachedOrders) {
            return cachedOrders.stream()
                    .map(OrderResponse.class::cast)
                    .toList();
        }

        List<OrderResponse> orders = repository.findByCustomerEmailOrderByCreatedAtDesc(email).stream()
                .map(OrderResponse::from)
                .toList();
        redisTemplate.opsForValue().set(cacheKey, new ArrayList<>(orders), customerOrdersCacheTtl);
        return orders;
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
        Order savedOrder = repository.save(order);
        evictCustomerOrdersAfterCommit(request.getCustomerEmail());
        return OrderResponse.from(savedOrder);
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public OrderResponse update(Long id, CreateOrderRequest request) {
        Order order = findEntityById(id);
        String previousEmail = order.getCustomerEmail();
        applyRequest(order, request);
        Order savedOrder = repository.save(order);
        evictCustomerOrdersAfterCommit(previousEmail, savedOrder.getCustomerEmail());
        return OrderResponse.from(savedOrder);
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public OrderResponse updateByNumber(String orderNumber, CreateOrderRequest request) {
        Order order = findEntityByNumber(orderNumber);
        String previousEmail = order.getCustomerEmail();
        applyRequest(order, request);
        Order savedOrder = repository.save(order);
        evictCustomerOrdersAfterCommit(previousEmail, savedOrder.getCustomerEmail());
        return OrderResponse.from(savedOrder);
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
        Order order = findEntityById(id);
        evictCustomerOrdersAfterCommit(order.getCustomerEmail());
        repository.delete(order);
    }

    @CacheEvict(cacheNames = {"ordersById", "ordersByNumber"}, allEntries = true)
    public void deleteByNumber(String orderNumber) {
        Order order = findEntityByNumber(orderNumber);
        evictCustomerOrdersAfterCommit(order.getCustomerEmail());
        repository.delete(order);
    }

    private Order findEntityById(Long id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    private Order findEntityByNumber(String orderNumber) {
        return repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    private String customerOrdersCacheKey(String email) {
        return CUSTOMER_ORDERS_CACHE_PREFIX + email;
    }

    private void evictCustomerOrdersAfterCommit(String... emails) {
        Set<String> cacheKeys = Arrays.stream(emails)
                .filter(email -> email != null && !email.isBlank())
                .map(this::customerOrdersCacheKey)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (cacheKeys.isEmpty()) {
            return;
        }

        Runnable eviction = () -> redisTemplate.delete(cacheKeys);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eviction.run();
                }
            });
        } else {
            eviction.run();
        }
    }
}
