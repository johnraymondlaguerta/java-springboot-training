package com.training.order.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    /** REFERENCE: only the customer's _id is stored in the order document. */
    @DocumentReference
    private Customer customer;

    /** EMBEDDED: items never exist without their order. */
    private List<OrderItem> items = new ArrayList<>();

    private double total;

    /** Indexed: "show me all NEW / PAID orders". */
    @Indexed
    private String status;

    /** Indexed: orders are listed newest first. */
    @Indexed
    private Date created;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCreated() { return created; }
    public void setCreated(Date created) { this.created = created; }
}
