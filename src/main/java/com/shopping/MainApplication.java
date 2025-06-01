package com.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class MainApplication {

    // Data stores (in production, use database)
    private static final List<Map<String, Object>> products = new ArrayList<>();
    private static final Map<String, Map<Integer, Integer>> userCarts = new HashMap<>();
    private static final List<Map<String, Object>> orders = new ArrayList<>();
    private static final Set<String> categories = new HashSet<>();
    
    // Simple user session store
    private static final Map<String, String> userSessions = new HashMap<>();
    private static int orderIdCounter = 1;

    static {
        initializeProducts();
    }

    private static void initializeProducts() {
        products.addAll(Arrays.asList(
            Map.of("id", 1, "name", "Gaming Laptop", "price", 1299.99, "category", "Electronics", "description", "High-performance gaming laptop with RTX graphics"),
            Map.of("id", 2, "name", "Wireless Mouse", "price", 29.99, "category", "Electronics", "description", "Ergonomic wireless mouse with RGB lighting"),
            Map.of("id", 3, "name", "Mechanical Keyboard", "price", 89.99, "category", "Electronics", "description", "Cherry MX switches mechanical keyboard"),
            Map.of("id", 4, "name", "4K Monitor", "price", 299.99, "category", "Electronics", "description", "27-inch 4K UHD monitor with HDR support"),
            Map.of("id", 5, "name", "Bluetooth Headphones", "price", 149.99, "category", "Electronics", "description", "Noise-cancelling wireless headphones"),
            Map.of("id", 6, "name", "Smart Watch", "price", 249.99, "category", "Electronics", "description", "Fitness tracking smartwatch with GPS"),
            Map.of("id", 7, "name", "Tablet", "price", 399.99, "category", "Electronics", "description", "10-inch tablet with stylus support"),
            Map.of("id", 8, "name", "Phone Case", "price", 19.99, "category", "Accessories", "description", "Protective phone case with card holder")
        ));
        
        // Extract categories
        products.forEach(p -> categories.add((String) p.get("category")));
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    // ============= AUTHENTICATION =============
    
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        // Simple auth - in production use proper authentication
        if (username != null && password != null && username.length() > 0) {
            String sessionId = UUID.randomUUID().toString();
            userSessions.put(sessionId, username);
            userCarts.putIfAbsent(username, new HashMap<>());
            return Map.of("success", true, "sessionId", sessionId, "username", username);
        }
        return Map.of("success", false, "message", "Invalid credentials");
    }
    
    @PostMapping("/auth/logout")
    public Map<String, Object> logout(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        userSessions.remove(sessionId);
        return Map.of("success", true);
    }

    // ============= PRODUCTS =============
    
    @GetMapping("/products")
    public Map<String, Object> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        
        List<Map<String, Object>> filteredProducts = products.stream()
            .filter(p -> category == null || category.equals(p.get("category")))
            .filter(p -> search == null || 
                ((String) p.get("name")).toLowerCase().contains(search.toLowerCase()) ||
                ((String) p.get("description")).toLowerCase().contains(search.toLowerCase()))
            .collect(Collectors.toList());
            
        return Map.of("products", filteredProducts, "categories", categories);
    }
    
    @GetMapping("/products/{id}")
    public Map<String, Object> getProduct(@PathVariable int id) {
        return products.stream()
            .filter(p -> p.get("id").equals(id))
            .findFirst()
            .map(p -> Map.of("success", true, "product", p))
            .orElse(Map.of("success", false, "message", "Product not found"));
    }

    // ============= CART =============
    
    @GetMapping("/cart")
    public Map<String, Object> getCart(@RequestParam String sessionId) {
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        Map<Integer, Integer> cart = userCarts.getOrDefault(username, new HashMap<>());
        List<Map<String, Object>> items = new ArrayList<>();
        double totalPrice = 0;
        
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            products.stream()
                .filter(p -> p.get("id").equals(entry.getKey()))
                .findFirst()
                .ifPresent(product -> {
                    Map<String, Object> item = new HashMap<>(product);
                    item.put("quantity", entry.getValue());
                    double itemTotal = (Double) product.get("price") * entry.getValue();
                    item.put("total", itemTotal);
                    items.add(item);
                });
        }
        
        totalPrice = items.stream().mapToDouble(item -> (Double) item.get("total")).sum();
        
        return Map.of("success", true, "items", items, "totalPrice", totalPrice, "itemCount", cart.values().stream().mapToInt(Integer::intValue).sum());
    }

    @PostMapping("/cart")
    public Map<String, Object> addToCart(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        Integer productId = (Integer) body.get("productId");
        Integer quantity = (Integer) body.get("quantity");
        
        if (productId == null || quantity == null || quantity <= 0) {
            return Map.of("success", false, "message", "Invalid product or quantity");
        }
        
        Map<Integer, Integer> cart = userCarts.computeIfAbsent(username, k -> new HashMap<>());
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
        
        return Map.of("success", true, "message", "Item added to cart");
    }

    @PutMapping("/cart")
    public Map<String, Object> updateCartItem(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        Integer productId = (Integer) body.get("productId");
        Integer quantity = (Integer) body.get("quantity");
        
        Map<Integer, Integer> cart = userCarts.get(username);
        if (cart == null) {
            return Map.of("success", false, "message", "Cart not found");
        }
        
        if (quantity <= 0) {
            cart.remove(productId);
        } else {
            cart.put(productId, quantity);
        }
        
        return Map.of("success", true, "message", "Cart updated");
    }

    @DeleteMapping("/cart/{productId}")
    public Map<String, Object> removeFromCart(@PathVariable int productId, @RequestParam String sessionId) {
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        Map<Integer, Integer> cart = userCarts.get(username);
        if (cart != null) {
            cart.remove(productId);
        }
        
        return Map.of("success", true, "message", "Item removed from cart");
    }

    @DeleteMapping("/cart")
    public Map<String, Object> clearCart(@RequestParam String sessionId) {
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        userCarts.put(username, new HashMap<>());
        return Map.of("success", true, "message", "Cart cleared");
    }

    // ============= ORDERS =============
    
    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        Map<Integer, Integer> cart = userCarts.get(username);
        if (cart == null || cart.isEmpty()) {
            return Map.of("success", false, "message", "Cart is empty");
        }
        
        // Calculate order details
        List<Map<String, Object>> orderItems = new ArrayList<>();
        double totalPrice = 0;
        
        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            products.stream()
                .filter(p -> p.get("id").equals(entry.getKey()))
                .findFirst()
                .ifPresent(product -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("productId", product.get("id"));
                    item.put("name", product.get("name"));
                    item.put("price", product.get("price"));
                    item.put("quantity", entry.getValue());
                    item.put("total", (Double) product.get("price") * entry.getValue());
                    orderItems.add(item);
                });
        }
        
        totalPrice = orderItems.stream().mapToDouble(item -> (Double) item.get("total")).sum();
        
        // Create order
        Map<String, Object> order = new HashMap<>();
        order.put("id", orderIdCounter++);
        order.put("username", username);
        order.put("items", orderItems);
        order.put("totalPrice", totalPrice);
        order.put("status", "Pending");
        order.put("orderDate", LocalDateTime.now().toString());
        order.put("shippingAddress", body.get("shippingAddress"));
        order.put("paymentMethod", body.get("paymentMethod"));
        
        orders.add(order);
        
        // Clear cart after order
        userCarts.put(username, new HashMap<>());
        
        return Map.of("success", true, "order", order, "message", "Order placed successfully");
    }
    
    @GetMapping("/orders")
    public Map<String, Object> getUserOrders(@RequestParam String sessionId) {
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        List<Map<String, Object>> userOrders = orders.stream()
            .filter(order -> username.equals(order.get("username")))
            .collect(Collectors.toList());
        
        return Map.of("success", true, "orders", userOrders);
    }
    
    @GetMapping("/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable int orderId, @RequestParam String sessionId) {
        String username = userSessions.get(sessionId);
        if (username == null) {
            return Map.of("success", false, "message", "Invalid session");
        }
        
        return orders.stream()
            .filter(order -> order.get("id").equals(orderId) && username.equals(order.get("username")))
            .findFirst()
            .map(order -> Map.of("success", true, "order", order))
            .orElse(Map.of("success", false, "message", "Order not found"));
    }
}
