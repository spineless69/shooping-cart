import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

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
        products.add(Map.of("id", 4, "name", "Monitor", "price", 150));
        products.add(Map.of("id", 5, "name", "USB Cable", "price", 10));
        products.add(Map.of("id", 6, "name", "Webcam", "price", 80));
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MainApplication.class);
        app.setDefaultProperties(Map.of("server.port", "8070"));
        app.run(args);
    }

    // Redirect root "/" to frontend.html (assuming frontend.html is served statically)
    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/frontend.html");
    }

    // Get all products
    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    // Get current cart items with total price calculation
    @GetMapping("/cart")
    public Map<String, Object> getCart() {
        List<Map<String, Object>> items = new ArrayList<>();
        double total = 0;

        for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
            int productId = entry.getKey();
            int quantity = entry.getValue();
            if (quantity <= 0) continue;

            Optional<Map<String, Object>> productOpt = products.stream()
                    .filter(p -> p.get("id").equals(productId))
                    .findFirst();

            if (productOpt.isPresent()) {
                Map<String, Object> product = productOpt.get();
                Map<String, Object> item = new HashMap<>(product);
                int price = (int) product.get("price");
                item.put("quantity", quantity);
                item.put("total", price * quantity);
                items.add(item);
                total += price * quantity;
            }
        }

        return Map.of("items", items, "total", total);
    }

    // Add to cart or update quantity (replace or add)
    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> data) {
        Integer id = (Integer) data.get("id");
        Integer quantity = (Integer) data.get("quantity");
        Boolean replace = (Boolean) data.get("replace");

        if (id == null || quantity == null || quantity < 0) {
            return "Invalid product ID or quantity";
        }

        boolean productExists = products.stream().anyMatch(p -> p.get("id").equals(id));
        if (!productExists) {
            return "Product not found";
        }

        if (Boolean.TRUE.equals(replace)) {
            if (quantity == 0) {
                cart.remove(id);
                return "Item removed from cart";
            } else {
                cart.put(id, quantity);
            }
        } else {
            if (quantity <= 0) {
                return "Invalid quantity";
            }
            cart.put(id, cart.getOrDefault(id, 0) + quantity);
        }

        return "Cart updated";
    }

    // Remove item from cart by id
    @DeleteMapping("/cart/{id}")
    public String removeFromCart(@PathVariable Integer id) {
        if (cart.containsKey(id)) {
            cart.remove(id);
            return "Item removed from cart";
        } else {
            return "Item not in cart";
        }
    }

    // Clear entire cart
    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
