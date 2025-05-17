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
        products.add(Map.of("id", 1, "name", "Laptop", "price", 750));
        products.add(Map.of("id", 2, "name", "Mouse", "price", 25));
        products.add(Map.of("id", 3, "name", "Keyboard", "price", 45));
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    // Serve interactive frontend HTML at root "/"
    @GetMapping("/")
    public String home() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Shopping Cart</title>
                <style>
                    body { font-family: Arial; }
                    .product { margin-bottom: 10px; }
                    button { margin-left: 10px; }
                </style>
            </head>
            <body>
                <h2>Products</h2>
                <div id="product-list"></div>
                <h2>Cart</h2>
                <div id="cart"></div>

                <script>
                    async function loadProducts() {
                        let res = await fetch('/products');
                        let products = await res.json();
                        const list = document.getElementById('product-list');
                        list.innerHTML = '';
                        products.forEach(p => {
                            list.innerHTML += `
                                <div class="product">
                                    ${p.name} - $${p.price}
                                    <button onclick="addToCart(${p.id})">Add to Cart</button>
                                </div>`;
                        });
                    }

                    async function loadCart() {
                        let res = await fetch('/cart');
                        let data = await res.json();
                        const cartDiv = document.getElementById('cart');
                        cartDiv.innerHTML = '';
                        data.items.forEach(item => {
                            cartDiv.innerHTML += `${item.name} - Quantity: ${item.quantity}<br>`;
                        });
                    }

                    async function addToCart(id) {
                        await fetch('/cart', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ id: id, quantity: 1 })
                        });
                        loadCart();
                    }

                    loadProducts();
                    loadCart();
                </script>
            </body>
            </html>
        """;
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

    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
