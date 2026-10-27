package com.example.ordermanagement.order.dto;

import java.math.BigDecimal;
import java.io.Serializable;

public class OrderItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productCode;
    private BigDecimal unitPrice;
    private Integer quantity;

    public OrderItemResponse() {
    }

    public OrderItemResponse(String productCode, BigDecimal unitPrice, Integer quantity) {
        this.productCode = productCode;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductCode() { return productCode; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
}
