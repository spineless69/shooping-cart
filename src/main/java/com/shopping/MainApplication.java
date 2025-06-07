package com.shopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")
public class MainApplication {

    @Autowired
    private SimpleDataStore dataStore;

    private static final List<Map<String, Object>> products = new ArrayList<>();
    private static final Set<String> categories = new HashSet<>();

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

        products.forEach(p -> categories.add((String) p.get("category")));
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MainApplication.class);
        String port = System.getenv("PORT");
        if (port != null) {
            app.setDefaultProperties(Collections.singletonMap("server.port", port));
        }
        app.run(args);
    }

    // ==== AUTH ====

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username != null && password != null && !username.isEmpty()) {
            // Validate user credentials
            if (dataStore.validateUser(username, password)) {
                String sessionId = UUID.randomUUID().toString();
                dataStore.saveSession(sessionId, username);
                return Map.of("success", true, "sessionId", sessionId, "username", username);
            }
        }
        return Map.of("success", false, "message", "Invalid credentials");
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        if (sessionId != null) {
            dataStore.removeSession(sessionId);
        }
        return Map.of("success", true);
    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Map<String, String> userData) {
        String username = userData.get("username");
        String password = userData.get("password");
        String email = userData.get("email");

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return Map.of("success", false, "message", "Username and password are required");
        }

        if (dataStore.getUser(username) != null) {
            return Map.of("success", false, "message", "Username already exists");
        }

        Map<String, Object> userInfo = new HashMap<>();
        if (email != null) userInfo.put("email", email);
        
        dataStore.saveUser(username, password, userInfo);
        return Map.of("success", true, "message", "User registered successfully");
    }

    // ==== PRODUCTS ====

    @GetMapping("/products")
    public Map<String, Object> getProducts(
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String search
    ) {
        List<Map<String, Object>> filtered = products.stream()
            .filter(p -> category == null || category.equals(p.get("category")))
            .filter(p -> search == null ||
                ((String) p.get("name")).toLowerCase().contains(search.toLowerCase()) ||
                ((String) p.get("description")).toLowerCase().contains(search.toLowerCase()))
            .collect(Collectors.toList());

        return Map.of("products", filtered, "categories", categories);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> getProduct(@PathVariable int id) {
        return products.stream()
            .filter(p -> p.get("id").equals(id))
            .findFirst()
            .map(p -> Map.of("success", true, "product", p))
            .orElse(Map.of("success", false, "message", "Product not found"));
    }

    // ==== CART ====

    @GetMapping("/cart")
    public Map<String, Object> getCart(@RequestParam String sessionId) {
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        Map<Integer, Integer> cart = dataStore.getUserCart(username);
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

        totalPrice = items.stream().mapToDouble(i -> (Double) i.get("total")).sum();

        return Map.of(
            "success", true,
            "items", items,
            "totalPrice", totalPrice,
            "itemCount", cart.values().stream().mapToInt(Integer::intValue).sum()
        );
    }

    @PostMapping("/cart")
    public Map<String, Object> addToCart(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        Integer productId = (Integer) body.get("productId");
        Integer quantity = (Integer) body.get("quantity");

        if (productId == null || quantity == null || quantity <= 0)
            return Map.of("success", false, "message", "Invalid product or quantity");

        Map<Integer, Integer> cart = dataStore.getUserCart(username);
        cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
        dataStore.saveUserCart(username, cart);

        return Map.of("success", true, "message", "Item added to cart");
    }

    @PutMapping("/cart")
    public Map<String, Object> updateCartItem(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        Integer productId = (Integer) body.get("productId");
        Integer quantity = (Integer) body.get("quantity");

        Map<Integer, Integer> cart = dataStore.getUserCart(username);
        if (quantity <= 0) {
            cart.remove(productId);
        } else {
            cart.put(productId, quantity);
        }
        dataStore.saveUserCart(username, cart);

        return Map.of("success", true, "message", "Cart updated");
    }

    @DeleteMapping("/cart/{productId}")
    public Map<String, Object> removeFromCart(@PathVariable int productId, @RequestParam String sessionId) {
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        Map<Integer, Integer> cart = dataStore.getUserCart(username);
        cart.remove(productId);
        dataStore.saveUserCart(username, cart);

        return Map.of("success", true, "message", "Item removed from cart");
    }

    @DeleteMapping("/cart")
    public Map<String, Object> clearCart(@RequestParam String sessionId) {
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        dataStore.clearUserCart(username);
        return Map.of("success", true, "message", "Cart cleared");
    }

    // ==== ORDERS ====

    @PostMapping("/orders")
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        String sessionId = (String) body.get("sessionId");
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        Map<Integer, Integer> cart = dataStore.getUserCart(username);
        if (cart == null || cart.isEmpty())
            return Map.of("success", false, "message", "Cart is empty");

        List<Map<String, Object>> orderItems = new ArrayList<>();
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

        double totalPrice = orderItems.stream().mapToDouble(i -> (Double) i.get("total")).sum();

        Map<String, Object> order = new HashMap<>();
        order.put("id", dataStore.getNextOrderId());
        order.put("username", username);
        order.put("items", orderItems);
        order.put("totalPrice", totalPrice);
        order.put("status", "Pending");
        order.put("orderDate", LocalDateTime.now().toString());
        order.put("shippingAddress", body.get("shippingAddress"));
        order.put("paymentMethod", body.get("paymentMethod"));

        dataStore.saveOrder(order);
        dataStore.clearUserCart(username);

        return Map.of("success", true, "order", order, "message", "Order placed successfully");
    }

    @GetMapping("/orders")
    public Map<String, Object> getUserOrders(@RequestParam String sessionId) {
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        List<Map<String, Object>> userOrders = dataStore.getUserOrders(username);
        return Map.of("success", true, "orders", userOrders);
    }

    @GetMapping("/orders/{orderId}")
    public Map<String, Object> getOrder(@PathVariable int orderId, @RequestParam String sessionId) {
        String username = dataStore.getUserFromSession(sessionId);
        if (username == null) return Map.of("success", false, "message", "Invalid session");

        Map<String, Object> order = dataStore.getOrder(orderId, username);
        if (order != null) {
            return Map.of("success", true, "order", order);
        } else {
            return Map.of("success", false, "message", "Order not found");
        }
    }

    // ==== ADMIN ENDPOINTS ====

    @GetMapping("/admin/stats")
    public Map<String, Object> getStatistics() {
        return dataStore.getStatistics();
    }

    @PostMapping("/admin/backup")
    public Map<String, Object> createBackup(@RequestBody Map<String, String> body) {
        String filename = body.getOrDefault("filename", "backup_" + System.currentTimeMillis() + ".json");
        dataStore.createBackup(filename);
        return Map.of("success", true, "message", "Backup created: " + filename);
    }

    @PostMapping("/admin/restore")
    public Map<String, Object> restoreBackup(@RequestBody Map<String, String> body) {
        String filename = body.get("filename");
        if (filename == null) {
            return Map.of("success", false, "message", "Filename is required");
        }
        dataStore.restoreFromBackup(filename);
        return Map.of("success", true, "message", "Data restored from: " + filename);
    }

    @GetMapping("/admin/export")
    public Map<String, Object> exportData() {
        String data = dataStore.exportDataAsJson();
        return Map.of("success", true, "data", data);
    }

    @PostMapping("/admin/clear")
    public Map<String, Object> clearAllData() {
        dataStore.clearAllData();
        return Map.of("success", true, "message", "All data cleared");
    }

    @GetMapping("/admin/sessions/cleanup")
    public Map<String, Object> cleanupSessions() {
        dataStore.cleanupExpiredSessions();
        return Map.of("success", true, "message", "Session cleanup completed");
    }
}