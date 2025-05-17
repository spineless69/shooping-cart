package com.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.*;

@SpringBootApplication
@RestController
public class MainApplication {

    private static final List<Map<String, Object>> products = new ArrayList<>();
    private static final Map<Integer, Integer> cart = new HashMap<>();

    static {
        products.add(Map.of("id", 1, "name", "Laptop", "price", 750));
        products.add(Map.of("id", 2, "name", "Mouse", "price", 20));
        products.add(Map.of("id", 3, "name", "Keyboard", "price", 30));
        products.add(Map.of("id", 4, "name", "Monitor", "price", 150));
        products.add(Map.of("id", 5, "name", "USB Cable", "price", 10));
        products.add(Map.of("id", 6, "name", "Webcam", "price", 80));
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MainApplication.class);
        app.setDefaultProperties(Map.of("server.port", "8070"));
        app.run(args);
    }

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/index.html");
    }

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    @GetMapping("/cart")
    public Map<String, Object> getCart() {
        List<Map<String, Object>> items = new ArrayList<>();
        double total = 0;
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();
            products.stream()
                    .filter(p -> p.get("id").equals(productId))
                    .findFirst()
                    .ifPresent(product -> {
                        Map<String, Object> item = new HashMap<>(product);
                        int price = (int) product.get("price");
                        item.put("quantity", quantity);
                        item.put("total", price * quantity);
                        items.add(item);
                    });
        }
        for (Map<String, Object> item : items) {
            total += (int) item.get("total");
        }
        return Map.of("items", items, "total", total);
    }

    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> data) {
        Integer id = (Integer) data.get("id");
        Integer quantity = (Integer) data.get("quantity");
        if (id == null || quantity == null || quantity <= 0) {
            return "Invalid product ID or quantity";
        }
        boolean exists = products.stream().anyMatch(p -> p.get("id").equals(id));
        if (!exists) return "Product not found";

        cart.put(id, cart.getOrDefault(id, 0) + quantity);
        return "Added to cart";
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestBody Map<String, Object> data) {
        Integer id = (Integer) data.get("id");
        Integer quantity = (Integer) data.get("quantity");
        if (id == null || quantity == null || quantity < 0) {
            return "Invalid product ID or quantity";
        }
        if (quantity == 0) {
            cart.remove(id);
        } else {
            cart.put(id, quantity);
        }
        return "Cart updated";
    }

    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
