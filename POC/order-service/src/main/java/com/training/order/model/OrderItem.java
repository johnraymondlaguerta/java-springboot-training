package com.training.order.model;

/**
 * EMBEDDED inside Order. A SNAPSHOT of the product at purchase time (name + price), so old
 * orders stay correct if the product changes, and showing an order needs no call to product-service.
 * productId is a plain string: a logical link to ANOTHER service's data.
 */
public record OrderItem(String productId, String productName, double unitPrice, int quantity) {
}
