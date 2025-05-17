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


        SpringApplication.run(MainApplication.class, args);


        SpringApplication app = new SpringApplication(MainApplication.class);


        app.setDefaultProperties(Map.of("server.port", "8070"));


        app.run(args);

    }




    // Root mapping

    @GetMapping("/")


    public String home() {


        return "Welcome to the Shopping Cart API";


    public RedirectView home() {


        return new RedirectView("/index.html");

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

        List<Map<String, Object>> items = new ArrayList<>();


        


        double total = 0;

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


        


        response.put("items", items);


        return response;


        for (Map<String, Object> item : items) {


            total += (int) item.get("total");


        }


        return Map.of("items", items, "total", total);

    }




    // Add to cart

    @PostMapping("/cart")

    public String addToCart(@RequestBody Map<String, Object> data) {

        Integer id = (Integer) data.get("id");

        Integer quantity = (Integer) data.get("quantity");


        Boolean replace = (Boolean) data.get("replace");


        


        if (id == null || quantity == null) {


        if (id == null || quantity == null || quantity <= 0) {

            return "Invalid product ID or quantity";

        }


        


        boolean productExists = products.stream().anyMatch(p -> p.get("id").equals(id));


        if (!productExists) {


            return "Product not found";


        }


        


        if (Boolean.TRUE.equals(replace)) {


            // Replace the quantity instead of adding to it


            if (quantity <= 0) {


                cart.remove(id);


                return "Item removed from cart";


            } else {


                cart.put(id, quantity);


            }


        } else {


            // Add to existing quantity


            if (quantity <= 0) {


                return "Invalid quantity";


            }


            cart.put(id, cart.getOrDefault(id, 0) + quantity);


        }


        


        return "Cart updated";


        boolean exists = products.stream().anyMatch(p -> p.get("id").equals(id));


        if (!exists) return "Product not found";





        cart.put(id, cart.getOrDefault(id, 0) + quantity);


        return "Added to cart";

    }




    // Remove from cart


    @DeleteMapping("/cart/{id}")


    public String removeFromCart(@PathVariable Integer id) {


        if (cart.containsKey(id)) {


    @PostMapping("/cart/update")


    public String updateCart(@RequestBody Map<String, Object> data) {


        Integer id = (Integer) data.get("id");


        Integer quantity = (Integer) data.get("quantity");


        if (id == null || quantity == null || quantity < 0) {


            return "Invalid product ID or quantity";


        }


        if (quantity == 0) {

            cart.remove(id);


            return "Item removed from cart";


        } else {


            cart.put(id, quantity);

        }


        return "Item not in cart";


        return "Cart updated";

    }




    // Clear cart

    @DeleteMapping("/cart")

    public String clearCart() {

        cart.clear();
