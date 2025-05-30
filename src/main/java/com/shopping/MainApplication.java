package com.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
public class MainApplication {

    private static final List<Map<String, Object>> products = new ArrayList<>();
    private static final Map<Integer, Integer> cart = new HashMap<>();

    static {
        products.add(Map.of("id", 1, "name", "Laptop", "price", 799.99));
        products.add(Map.of("id", 2, "name", "Mouse", "price", 19.99));
        products.add(Map.of("id", 3, "name", "Keyboard", "price", 39.99));
        products.add(Map.of("id", 4, "name", "Monitor", "price", 149.99));
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    // ⚠️ REMOVED the manual index.html loader
    // Spring Boot will automatically serve index.html from static/

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    @GetMapping("/cart")
    public Map<String, Object> getCart() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            products.stream()
                .filter(p -> p.get("id").equals(entry.getKey()))
                .findFirst()
                .ifPresent(product -> {
                    Map<String, Object> item = new HashMap<>(product);
                    item.put("quantity", entry.getValue());
                    items.add(item);
                });
        }
        return Map.of("items", items);
    }

    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> body) {
        Object idObj = body.get("id");
        Object qtyObj = body.get("quantity");

        if (!(idObj instanceof Integer) || !(qtyObj instanceof Integer)) {
            return "Invalid data type";
        }

        int id = (Integer) idObj;
        int qty = (Integer) qtyObj;

        if (qty <= 0) return "Quantity must be positive";

        cart.put(id, cart.getOrDefault(id, 0) + qty);
        return "Item added";
    }

    @DeleteMapping("/cart/{id}")
    public String removeItem(@PathVariable int id) {
        cart.remove(id);
        return "Item removed";
    }

    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
