package com.training.order;

import com.training.order.model.Customer;
import com.training.order.model.Order;
import com.training.order.model.OrderItem;
import com.training.order.repository.CustomerRepository;
import com.training.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Loads sample customers and orders into orderdb on startup (only when both are empty).
 * Order items are historical SNAPSHOTS (name + price), so no call to product-service is needed;
 * productId is just a logical link to product-service's data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final CustomerRepository customers;
    private final OrderRepository orders;

    public DataSeeder(CustomerRepository customers, OrderRepository orders) {
        this.customers = customers;
        this.orders = orders;
    }

    @Override
    public void run(String... args) {
        if (customers.count() > 0 || orders.count() > 0) {
            log.info("orderdb already has data - seeding skipped");
            return;
        }
        Customer corazon = customer("cust-001", "Corazon de Guzman", "corazon@sample.com");
        Customer juan = customer("cust-002", "Juan Dela Cruz", "juan@sample.com");
        Customer pedro = customer("cust-003", "Pedro del Rosario", "pedro@sample.com");
        customers.saveAll(List.of(corazon, juan, pedro));

        orders.saveAll(List.of(
                order("order-001", pedro, "NEW", 2,
                        new OrderItem("prod-001", "Gaming Laptop", 40000, 1)),
                order("order-002", corazon, "PAID", 5,
                        new OrderItem("prod-003", "Wireless Headphones", 3500, 2),
                        new OrderItem("prod-004", "Mechanical Keyboard", 2500, 1)),
                order("order-003", juan, "PAID", 9,
                        new OrderItem("prod-002", "Smartphone X", 25000, 1))
        ));
        log.info("Seeded {} customers and {} orders into orderdb", customers.count(), orders.count());
    }

    private Customer customer(String id, String name, String email) {
        Customer c = new Customer();
        c.setId(id);
        c.setName(name);
        c.setEmail(email);
        return c;
    }

    private Order order(String id, Customer customer, String status, int daysAgo, OrderItem... items) {
        Order o = new Order();
        o.setId(id);
        o.setCustomer(customer);
        o.setItems(new ArrayList<>(List.of(items)));
        o.setTotal(Arrays.stream(items).mapToDouble(i -> i.unitPrice() * i.quantity()).sum());
        o.setStatus(status);
        o.setCreated(Date.from(Instant.now().minus(daysAgo, ChronoUnit.DAYS)));
        return o;
    }
}
