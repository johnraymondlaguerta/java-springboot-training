package com.training.product.controller;

import com.training.product.dto.ProductDTO;
import com.training.product.model.Review;
import com.training.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping("/product")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO post(@RequestBody ProductDTO dto) {
        return service.save(dto);
    }

    @GetMapping("/product/id/{id}")
    public ProductDTO getById(@PathVariable String id) {
        return service.findById(id);
    }

    @GetMapping("/product/sku/{sku}")
    public ProductDTO getBySku(@PathVariable String sku) {
        return service.findBySku(sku);
    }

    @GetMapping("/products")
    public List<ProductDTO> getAll(@RequestParam(required = false) String category) {
        return service.findAll(category);
    }

    @PostMapping("/product/id/{id}/review")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO addReview(@PathVariable String id, @RequestBody Review review) {
        return service.addReview(id, review);
    }

    /** Called by order-service through OpenFeign. */
    @PutMapping("/product/id/{id}/reduce-stock")
    public ProductDTO reduceStock(@PathVariable String id, @RequestParam int quantity) {
        return service.reduceStock(id, quantity);
    }

    @DeleteMapping("/products")
    public long deleteAll() {
        return service.deleteAll();
    }
}
