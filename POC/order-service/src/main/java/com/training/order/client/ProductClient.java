package com.training.order.client;

import com.training.order.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Declarative REST client. name = "product-service" is the Eureka service id:
 * NO host, IP or port here. Feign + Spring Cloud LoadBalancer ask Eureka where it runs.
 */
@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/product/id/{id}")
    ProductDto getProduct(@PathVariable("id") String id);

    @PutMapping("/api/product/id/{id}/reduce-stock")
    ProductDto reduceStock(@PathVariable("id") String id, @RequestParam("quantity") int quantity);
}
