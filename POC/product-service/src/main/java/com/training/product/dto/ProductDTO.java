package com.training.product.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.training.product.model.Review;

import java.util.List;
import java.util.Map;

/** Request and response body of the product API. specs/reviews only appear when they have content. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDTO(String id, String sku, String name, String category, double price, int stock,
                         @JsonInclude(JsonInclude.Include.NON_EMPTY) Map<String, String> specs,
                         @JsonInclude(JsonInclude.Include.NON_EMPTY) List<Review> reviews) {
}
