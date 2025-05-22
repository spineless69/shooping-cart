package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
public class MainApplication {

    private List<Map<String, Object>> products = Arrays.asList(
            Map.of("id", 1, "name", "Laptop", "price", 750),
            Map.of("id", 2, "name", "Mouse", "price", 25),
            Map.of("id", 3, "name", "Keyboard", "price", 45)
    );

    private Map<Integer, Integer> cart = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> data) {
        Number idNum = (Number) data.get("id");
        Number qtyNum = (Number) data.get("quantity");

        if (idNum == null || qtyNum == null) return "Invalid input";

        int id = idNum.intValue();
        int quantity = qtyNum.intValue();

        boolean exists = products.stream().anyMatch(p -> p.get("id").equals(id));
        if (!exists) return "Product not found";

        cart.put(id, cart.getOrDefault(id, 0) + quantity);
        return "Added to cart";
    }

    @GetMapping("/cart")
    public List<Map<String, Object>> viewCart() {
        List<Map<String, Object>> cartItems = new ArrayList<>();
        for (var entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();
            products.stream()
                .filter(p -> p.get("id").equals(productId))
                .findFirst()
                .ifPresent(product -> {
                    Map<String, Object> item = new HashMap<>(product);
                    item.put("quantity", quantity);
                    cartItems.add(item);
                });
        }
        return cartItems;
    }

    @PostMapping("/cart/clear")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
