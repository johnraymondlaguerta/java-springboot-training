package com.training.product.service;

import com.mongodb.client.result.UpdateResult;
import com.training.product.dto.ProductDTO;
import com.training.product.model.Product;
import com.training.product.model.Review;
import com.training.product.repository.ProductRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final MongoTemplate mongo;

    public ProductService(ProductRepository repository, MongoTemplate mongo) {
        this.repository = repository;
        this.mongo = mongo;
    }

    public ProductDTO save(ProductDTO dto) {
        if (dto.sku() == null || dto.sku().isBlank() || dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sku and name are required");
        }
        Product p = new Product();
        p.setSku(dto.sku());
        p.setName(dto.name());
        p.setCategory(dto.category());
        p.setPrice(dto.price());
        p.setStock(dto.stock());
        if (dto.specs() != null) p.setSpecs(new HashMap<>(dto.specs()));
        if (dto.reviews() != null) p.setReviews(new ArrayList<>(dto.reviews()));
        try {
            return toDto(repository.save(p));
        } catch (DuplicateKeyException e) {   // raised by the unique index on sku
            throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists: " + dto.sku());
        }
    }

    public List<ProductDTO> findAll(String category) {
        List<Product> products = category == null || category.isBlank()
                ? repository.findAll() : repository.findByCategory(category);
        return products.stream().map(this::toDto).toList();
    }

    public ProductDTO findById(String id) {
        return toDto(get(id));
    }

    public ProductDTO findBySku(String sku) {
        return repository.findBySku(sku).map(this::toDto).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + sku));
    }

    /** Embedded write: one atomic $push, no second collection involved. */
    public ProductDTO addReview(String id, Review review) {
        Review withDate = new Review(review.author(), review.rating(), review.comment(), new Date());
        UpdateResult result = mongo.updateFirst(query(where("_id").is(id)),
                new Update().push("reviews", withDate), Product.class);
        if (result.getMatchedCount() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        }
        return findById(id);
    }

    /**
     * Stock belongs to product-service, so order-service must call this endpoint.
     * The check "stock >= quantity" and the decrement are ONE atomic MongoDB update,
     * so two simultaneous orders can never oversell.
     */
    public ProductDTO reduceStock(String id, int quantity) {
        if (quantity <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0");
        }
        get(id);   // 404 if the product does not exist
        UpdateResult result = mongo.updateFirst(
                query(where("_id").is(id).and("stock").gte(quantity)),
                new Update().inc("stock", -quantity), Product.class);
        if (result.getModifiedCount() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for product " + id);
        }
        return findById(id);
    }

    public long deleteAll() {
        long count = repository.count();
        repository.deleteAll();
        return count;
    }

    private Product get(String id) {
        return repository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    private ProductDTO toDto(Product p) {
        return new ProductDTO(p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getPrice(), p.getStock(),
                              p.getSpecs(), p.getReviews());
    }
}
