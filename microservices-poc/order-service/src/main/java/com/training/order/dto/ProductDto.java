package com.training.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * order-service's OWN view of a product. Deliberately NOT shared with product-service
 * (no common library, no common entity) -> loose coupling.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDto(String id, String sku, String name, double price, int stock) {
}
