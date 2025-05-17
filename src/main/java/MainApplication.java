import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@SpringBootApplication
@RestController
public class MainApplication {

    private static List<Map<String, Object>> products = new ArrayList<>();
    private static Map<Integer, Integer> cart = new HashMap<>();

    static {
        products.add(Map.of("id", 1, "name", "Laptop", "price", 750));
        products.add(Map.of("id", 2, "name", "Mouse", "price", 20));
        products.add(Map.of("id", 3, "name", "Keyboard", "price", 30));
    }

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @GetMapping("/products")
    public List<Map<String, Object>> getProducts() {
        return products;
    }

    @GetMapping("/cart")
    public Map<String, Object> getCart() {
        Map<String, Object> response = new HashMap<>();
        response.put("items", cart);
        return response;
    }

    @PostMapping("/cart")
    public String addToCart(@RequestBody Map<String, Object> data) {
        int id = (int) data.get("id");
        int quantity = (int) data.get("quantity");
        cart.put(id, cart.getOrDefault(id, 0) + quantity);
        return "Added to cart";
    }

    @DeleteMapping("/cart")
    public String clearCart() {
        cart.clear();
        return "Cart cleared";
    }
}
