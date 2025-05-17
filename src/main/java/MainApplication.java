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
                    body { font-family: Arial; max-width: 700px; margin: 20px auto; padding: 0 10px; }
                    .product, .cart-item { margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #ccc; }
                    button { margin-left: 10px; }
                    input[type=number] { width: 50px; margin-left: 10px; }
                    #cart { margin-top: 20px; }
                    #cart .cart-item button { margin-left: 20px; }
                </style>
            </head>
            <body>
                <h1>Products</h1>
                <div id="product-list"></div>

                <h1>Cart</h1>
                <div id="cart"></div>
                <button onclick="clearCart()">Clear Cart</button>

                <script>
                    async function loadProducts() {
                        const res = await fetch('/products');
                        const products = await res.json();
                        const list = document.getElementById('product-list');
                        list.innerHTML = '';
                        products.forEach(p => {
                            list.innerHTML += `
                                <div class="product">
                                    <strong>${p.name}</strong> - $${p.price}
                                    <input type="number" id="qty-${p.id}" min="1" value="1" />
                                    <button onclick="addToCart(${p.id})">Add to Cart</button>
                                </div>`;
                        });
                    }

                    async function loadCart() {
                        const res = await fetch('/cart');
                        const data = await res.json();
                        const cartDiv = document.getElementById('cart');
                        cartDiv.innerHTML = '';
                        if (!data.items || data.items.length === 0) {
                            cartDiv.innerHTML = '<i>Your cart is empty.</i>';
                            return;
                        }
                        let total = 0;
                        data.items.forEach(item => {
                            const itemTotal = item.price * item.quantity;
                            total += itemTotal;
                            cartDiv.innerHTML += `
                                <div class="cart-item">
                                    ${item.name} - Quantity: ${item.quantity} - $${itemTotal.toFixed(2)}
                                    <button onclick="removeFromCart(${item.id})">Remove</button>
                                </div>`;
                        });
                        cartDiv.innerHTML += `<h3>Total: $${total.toFixed(2)}</h3>`;
                    }

                    async function addToCart(id) {
                        const qtyInput = document.getElementById('qty-' + id);
                        const quantity = parseInt(qtyInput.value);
                        if (quantity < 1) {
                            alert('Quantity must be at least 1');
                            return;
                        }
                        await fetch('/cart', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ id, quantity })
                        });
                        loadCart();
                    }

                    async function removeFromCart(id) {
                        await fetch('/cart/' + id, { method: 'DELETE' });
                        loadCart();
                    }

                    async function clearCart() {
                        await fetch('/cart', { method: 'DELETE' });
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

    @DeleteMapping("/cart/{id}")
    public String removeFromCart(@PathVariable Integer id) {
        cart.remove(id);
        return "Removed from cart";
    }

    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
