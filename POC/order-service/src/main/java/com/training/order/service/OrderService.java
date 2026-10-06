package com.training.order.service;

import com.training.order.client.ProductClient;
import com.training.order.dto.CustomerDTO;
import com.training.order.dto.OrderDTO;
import com.training.order.dto.OrderRequest;
import com.training.order.dto.ProductDto;
import com.training.order.model.Customer;
import com.training.order.model.Order;
import com.training.order.model.OrderItem;
import com.training.order.repository.CustomerRepository;
import com.training.order.repository.OrderRepository;
import feign.FeignException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ProductClient productClient;   // REST call to another service (OpenFeign)

    public OrderService(OrderRepository orders, CustomerRepository customers, ProductClient productClient) {
        this.orders = orders;
        this.customers = customers;
        this.productClient = productClient;
    }

    // ---------- customers ----------
    public CustomerDTO createCustomer(CustomerDTO dto) {
        if (dto.name() == null || dto.name().isBlank() || dto.email() == null || dto.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name and email are required");
        }
        Customer c = new Customer();
        c.setName(dto.name());
        c.setEmail(dto.email());
        try {
            return toDto(customers.save(c));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered: " + dto.email());
        }
    }

    public CustomerDTO getCustomer(String id) {
        return toDto(customers.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id)));
    }

    public List<CustomerDTO> listCustomers() {
        return customers.findAll().stream().map(this::toDto).toList();
    }

    public long deleteCustomers() {
        long count = customers.count();
        customers.deleteAll();
        return count;
    }

    // ---------- orders ----------
    public OrderDTO placeOrder(OrderRequest request) {
        Customer customer = customers.findById(request.customerId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + request.customerId()));
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An order needs at least one item");
        }

        // 1) Validate everything first, using product data obtained over REST
        List<OrderItem> items = new ArrayList<>();
        double total = 0;
        for (OrderRequest.Item requested : request.items()) {
            if (requested.quantity() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be > 0");
            }
            ProductDto product = getProduct(requested.productId());
            if (product.stock() < requested.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for " + product.name() + " (available: " + product.stock() + ")");
            }
            // 2) Snapshot the product into the embedded OrderItem
            items.add(new OrderItem(product.id(), product.name(), product.price(), requested.quantity()));
            total += product.price() * requested.quantity();
        }

        // 3) Ask product-service to reduce stock (it owns that data; we never write to it)
        for (OrderItem item : items) {
            try {
                productClient.reduceStock(item.productId(), item.quantity());
            } catch (FeignException.Conflict e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock changed for " + item.productName());
            } catch (FeignException e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "product-service is not available");
            }
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setItems(items);
        order.setTotal(total);
        order.setStatus("NEW");
        order.setCreated(new Date());
        return toDto(orders.save(order));
    }

    public OrderDTO getOrder(String id) {
        return toDto(orders.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id)));
    }

    public List<OrderDTO> listOrders() {
        return orders.findAllByOrderByCreatedDesc().stream().map(this::toDto).toList();
    }

    public long deleteOrders() {
        long count = orders.count();
        orders.deleteAll();
        return count;
    }

    private ProductDto getProduct(String id) {
        try {
            return productClient.getProduct(id);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id);
        } catch (FeignException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "product-service is not available");
        }
    }

    private CustomerDTO toDto(Customer c) {
        return new CustomerDTO(c.getId(), c.getName(), c.getEmail());
    }

    private OrderDTO toDto(Order o) {
        Customer c = o.getCustomer();
        return new OrderDTO(o.getId(), c == null ? null : c.getId(), c == null ? null : c.getName(),
                            c == null ? null : c.getEmail(), o.getItems(), o.getTotal(), o.getStatus(), o.getCreated());
    }
}
