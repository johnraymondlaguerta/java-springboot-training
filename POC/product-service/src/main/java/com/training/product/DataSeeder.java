package com.training.product;

import com.training.product.model.Product;
import com.training.product.model.Review;
import com.training.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads sample products into productdb on startup, only if the collection is empty. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final ProductRepository repository;

    public DataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("productdb already has data - seeding skipped");
            return;
        }
        repository.saveAll(List.of(
                product("prod-001", "LAP-001", "Gaming Laptop", "computers", 40000, 5,
                        Map.of("ram", "16GB", "cpu", "Ryzen 7", "gpu", "RTX 4060"),
                        new Review("Corazon", 5, "Runs every game smoothly", new Date())),
                product("prod-002", "PHN-001", "Smartphone X", "phones", 25000, 20,
                        Map.of("storage", "256GB", "screen", "6.5 inch"),
                        new Review("Juan", 4, "Great camera", new Date())),
                product("prod-003", "HDP-001", "Wireless Headphones", "audio", 3500, 50,
                        Map.of("battery", "30 hours", "noise-cancelling", "yes")),
                product("prod-004", "KBD-001", "Mechanical Keyboard", "accessories", 2500, 30,
                        Map.of("switch", "Brown", "layout", "TKL")),
                product("prod-005", "MON-001", "27-inch Monitor", "computers", 12000, 12,
                        Map.of("resolution", "2560x1440", "refresh", "144Hz"))
        ));
        log.info("Seeded {} products into productdb", repository.count());
    }

    private Product product(String id, String sku, String name, String category, double price, int stock,
                            Map<String, String> specs, Review... reviews) {
        Product p = new Product();
        p.setId(id);
        p.setSku(sku);
        p.setName(name);
        p.setCategory(category);
        p.setPrice(price);
        p.setStock(stock);
        p.setSpecs(new HashMap<>(specs));
        p.setReviews(new ArrayList<>(List.of(reviews)));
        return p;
    }
}
