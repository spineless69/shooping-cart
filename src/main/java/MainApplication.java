package com.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication
@RestController
public class MainApplication {

    // Product list
    private static final List<Map<String, Object>> products = new ArrayList<>();
    // Cart stores productId and quantity
    private static final Map<Integer, Integer> cart = new HashMap<>();

    static {
        products.add(Map.of("id", 1, "name", "Laptop", "price", 750));
        products.add(Map.of("id", 2, "name", "Mouse", "price", 20));
        products.add(Map.of("id", 3, "name", "Keyboard", "price", 30));
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    // Get all products
    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    // Get current cart items
    @GetMapping("/cart")
    public Map<String, Object> getCart() {
        Map<String, Object> response = new HashMap<>();
        // Map product info with quantities for nicer output
        List<Map<String, Object>> items = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();

            products.stream()
                    .filter(p -> p.get("id").equals(productId))
                    .findFirst()
                    .ifPresent(product -> {
                        Map<String, Object> item = new HashMap<>(product);
                        item.put("quantity", quantity);
                        items.add(item);
                    });
        }
        response.put("items", items);
        return response;
    }

    // Add to cart
    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> data) {
        Integer id = (Integer) data.get("id");
        Integer quantity = (Integer) data.get("quantity");
        if (id == null || quantity == null || quantity <= 0) {
            return "Invalid product ID or quantity";
        }

        boolean productExists = products.stream().anyMatch(p -> p.get("id").equals(id));
        if (!productExists) {
            return "Product not found";
        }

        cart.put(id, cart.getOrDefault(id, 0) + quantity);
        return "Added to cart";
    }

    // Clear cart
    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
