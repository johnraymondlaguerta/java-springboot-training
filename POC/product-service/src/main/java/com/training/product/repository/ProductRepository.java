package com.training.product.repository;

import com.training.product.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(String category);

    Optional<Product> findBySku(String sku);
}
