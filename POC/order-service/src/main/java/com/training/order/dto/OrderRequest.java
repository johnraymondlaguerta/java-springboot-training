package com.training.order.dto;

import java.util.List;

public record OrderRequest(String customerId, List<Item> items) {
    public record Item(String productId, int quantity) {
    }
}
