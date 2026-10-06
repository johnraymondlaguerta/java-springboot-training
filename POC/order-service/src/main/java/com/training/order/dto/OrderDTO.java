package com.training.order.dto;

import com.training.order.model.OrderItem;

import java.util.Date;
import java.util.List;

public record OrderDTO(String id, String customerId, String customerName, String customerEmail,
                       List<OrderItem> items, double total, String status, Date created) {
}
