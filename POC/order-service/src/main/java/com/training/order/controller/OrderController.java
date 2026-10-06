package com.training.order.controller;

import com.training.order.dto.CustomerDTO;
import com.training.order.dto.OrderDTO;
import com.training.order.dto.OrderRequest;
import com.training.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/customer")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerDTO postCustomer(@RequestBody CustomerDTO dto) {
        return service.createCustomer(dto);
    }

    @GetMapping("/customer/id/{id}")
    public CustomerDTO getCustomer(@PathVariable String id) {
        return service.getCustomer(id);
    }

    @GetMapping("/customers")
    public List<CustomerDTO> getCustomers() {
        return service.listCustomers();
    }

    @DeleteMapping("/customers")
    public long deleteCustomers() {
        return service.deleteCustomers();
    }

    @PostMapping("/order")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO postOrder(@RequestBody OrderRequest request) {
        return service.placeOrder(request);
    }

    @GetMapping("/order/id/{id}")
    public OrderDTO getOrder(@PathVariable String id) {
        return service.getOrder(id);
    }

    @GetMapping("/orders")
    public List<OrderDTO> getOrders() {
        return service.listOrders();
    }

    @DeleteMapping("/orders")
    public long deleteOrders() {
        return service.deleteOrders();
    }
}
