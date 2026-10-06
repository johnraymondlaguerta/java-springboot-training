package com.training.gateway;

import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET http://localhost:8080/ returns products, customers and orders as one JSON document.
 * Uses WebClient; "http://product-service" is a Eureka service name, not a host or port.
 */
@RestController
public class HomeController {

    private static final ParameterizedTypeReference<List<Object>> LIST = new ParameterizedTypeReference<>() {
    };

    private final WebClient client;

    public HomeController(ReactorLoadBalancerExchangeFilterFunction loadBalancer) {
        this.client = WebClient.builder().filter(loadBalancer).build();
    }

    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<Map<String, Object>> home() {
        return Mono.zip(fetch("http://product-service/api/products"),
                        fetch("http://order-service/api/customers"),
                        fetch("http://order-service/api/orders"))
                   .map(t -> {
                       Map<String, Object> body = new LinkedHashMap<>();
                       body.put("products", t.getT1());
                       body.put("customers", t.getT2());
                       body.put("orders", t.getT3());
                       return body;
                   });
    }

    private Mono<List<Object>> fetch(String url) {
        return client.get().uri(url).retrieve().bodyToMono(LIST).onErrorReturn(List.of());
    }
}
