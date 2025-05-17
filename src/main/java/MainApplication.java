package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@SpringBootApplication(scanBasePackages = {"com.example"})
@RestController
public class MainApplication {

    private static final List<Map<String, Object>> products = new ArrayList<>();
    private static final Map<Integer, Integer> cart = new HashMap<>();

    static {
        products.add(Map.of("id", 1, "name", "Product 1", "price", 10.0));
        products.add(Map.of("id", 2, "name", "Product 2", "price", 20.0));
        products.add(Map.of("id", 3, "name", "Product 3", "price", 30.0));
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @GetMapping("/")
    public String home() {
        return "Welcome to the Shopping Cart API. Use /products or /cart endpoints.";
    }

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    @GetMapping("/cart")
    public Map<String, Object> getCart() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();

            if (quantity <= 0) continue;

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

    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> data) {
        Integer id = (Integer) data.get("id");
        Integer quantity = (Integer) data.get("quantity");
        Boolean replace = (Boolean) data.get("replace");

        if (id == null || quantity == null) {
            return "Invalid product ID or quantity";
        }

        boolean productExists = products.stream().anyMatch(p -> p.get("id").equals(id));
        if (!productExists) {
            return "Product not found";
        }

        if (Boolean.TRUE.equals(replace)) {
            if (quantity <= 0) {
                cart.remove(id);
                return "Item removed from cart";
            } else {
                cart.put(id, quantity);
                return "Cart item updated";
            }
        } else {
            if (quantity <= 0) {
                return "Invalid quantity";
            }
            cart.put(id, cart.getOrDefault(id, 0) + quantity);
            return "Cart updated";
        }
    }

    @DeleteMapping("/cart/{id}")
    public String removeFromCart(@PathVariable Integer id) {
        if (cart.containsKey(id)) {
            cart.remove(id);
            return "Item removed from cart";
        }
        return "Item not in cart";
    }

    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
