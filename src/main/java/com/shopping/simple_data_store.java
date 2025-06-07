package com.shopping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SimpleDataStore {
    
    private static final String DATA_FILE = "shopping_data.json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // In-memory storage with thread safety
    private final Map<String, Object> dataStore = new ConcurrentHashMap<>();
    
    // Data structure keys
    private static final String USERS_KEY = "users";
    private static final String SESSIONS_KEY = "sessions";
    private static final String CARTS_KEY = "carts";
    private static final String ORDERS_KEY = "orders";
    private static final String ORDER_COUNTER_KEY = "orderCounter";
    
    @PostConstruct
    public void loadData() {
        try {
            File file = new File(DATA_FILE);
            if (file.exists()) {
                Map<String, Object> loadedData = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
                dataStore.putAll(loadedData);
                System.out.println("Data loaded from " + DATA_FILE);
            } else {
                // Initialize with empty data structures
                initializeEmptyData();
                System.out.println("Initialized with empty data structures");
            }
        } catch (Exception e) {
            System.err.println("Error loading data: " + e.getMessage());
            initializeEmptyData();
        }
    }
    
    @PreDestroy
    public void saveData() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(DATA_FILE), dataStore);
            System.out.println("Data saved to " + DATA_FILE);
        } catch (Exception e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }
    
    private void initializeEmptyData() {
        dataStore.put(USERS_KEY, new ConcurrentHashMap<String, Map<String, Object>>());
        dataStore.put(SESSIONS_KEY, new ConcurrentHashMap<String, String>());
        dataStore.put(CARTS_KEY, new ConcurrentHashMap<String, Map<Integer, Integer>>());
        dataStore.put(ORDERS_KEY, new ArrayList<Map<String, Object>>());
        dataStore.put(ORDER_COUNTER_KEY, 1);
    }
    
    // User management
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> getUsers() {
        return (Map<String, Map<String, Object>>) dataStore.computeIfAbsent(USERS_KEY, k -> new ConcurrentHashMap<>());
    }
    
    public void saveUser(String username, String password, Map<String, Object> userInfo) {
        Map<String, Map<String, Object>> users = getUsers();
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("password", password);
        user.put("createdAt", new Date().toString());
        if (userInfo != null) {
            user.putAll(userInfo);
        }
        users.put(username, user);
        saveDataAsync();
    }
    
    public Map<String, Object> getUser(String username) {
        return getUsers().get(username);
    }
    
    public boolean validateUser(String username, String password) {
        Map<String, Object> user = getUser(username);
        if (user == null) {
            // For demo purposes, create user on first login
            saveUser(username, password, null);
            return true;
        }
        return password.equals(user.get("password"));
    }
    
    // Session management
    @SuppressWarnings("unchecked")
    public Map<String, String> getSessions() {
        return (Map<String, String>) dataStore.computeIfAbsent(SESSIONS_KEY, k -> new ConcurrentHashMap<>());
    }
    
    public void saveSession(String sessionId, String username) {
        getSessions().put(sessionId, username);
        saveDataAsync();
    }
    
    public String getUserFromSession(String sessionId) {
        return getSessions().get(sessionId);
    }
    
    public void removeSession(String sessionId) {
        getSessions().remove(sessionId);
        saveDataAsync();
    }
    
    // Cart management
    @SuppressWarnings("unchecked")
    public Map<String, Map<Integer, Integer>> getAllCarts() {
        return (Map<String, Map<Integer, Integer>>) dataStore.computeIfAbsent(CARTS_KEY, k -> new ConcurrentHashMap<>());
    }
    
    public Map<Integer, Integer> getUserCart(String username) {
        return getAllCarts().computeIfAbsent(username, k -> new HashMap<>());
    }
    
    public void saveUserCart(String username, Map<Integer, Integer> cart) {
        getAllCarts().put(username, new HashMap<>(cart));
        saveDataAsync();
    }
    
    public void clearUserCart(String username) {
        getAllCarts().put(username, new HashMap<>());
        saveDataAsync();
    }
    
    // Order management
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAllOrders() {
        return (List<Map<String, Object>>) dataStore.computeIfAbsent(ORDERS_KEY, k -> new ArrayList<>());
    }
    
    public void saveOrder(Map<String, Object> order) {
        List<Map<String, Object>> orders = getAllOrders();
        orders.add(new HashMap<>(order));
        saveDataAsync();
    }
    
    public List<Map<String, Object>> getUserOrders(String username) {
        return getAllOrders().stream()
                .filter(order -> username.equals(order.get("username")))
                .collect(ArrayList::new, (list, order) -> list.add(new HashMap<>(order)), ArrayList::addAll);
    }
    
    public Map<String, Object> getOrder(int orderId, String username) {
        return getAllOrders().stream()
                .filter(order -> order.get("id").equals(orderId) && username.equals(order.get("username")))
                .findFirst()
                .map(HashMap::new)
                .orElse(null);
    }
    
    // Order counter
    public int getNextOrderId() {
        Integer counter = (Integer) dataStore.get(ORDER_COUNTER_KEY);
        if (counter == null) {
            counter = 1;
        }
        dataStore.put(ORDER_COUNTER_KEY, counter + 1);
        saveDataAsync();
        return counter;
    }
    
    // Statistics and utility methods
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", getUsers().size());
        stats.put("activeSessions", getSessions().size());
        stats.put("totalOrders", getAllOrders().size());
        stats.put("totalCarts", getAllCarts().size());
        
        // Calculate total revenue
        double totalRevenue = getAllOrders().stream()
                .mapToDouble(order -> {
                    Object total = order.get("totalPrice");
                    return total instanceof Number ? ((Number) total).doubleValue() : 0.0;
                })
                .sum();
        stats.put("totalRevenue", totalRevenue);
        
        return stats;
    }
    
    public void cleanupExpiredSessions() {
        // This is a simple cleanup - in a real app, you'd track session timestamps
        // For now, we'll keep all sessions
        System.out.println("Session cleanup called - keeping all sessions for demo");
    }
    
    // Backup and restore
    public void createBackup(String backupFileName) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(backupFileName), dataStore);
            System.out.println("Backup created: " + backupFileName);
        } catch (Exception e) {
            System.err.println("Error creating backup: " + e.getMessage());
        }
    }
    
    public void restoreFromBackup(String backupFileName) {
        try {
            File file = new File(backupFileName);
            if (file.exists()) {
                Map<String, Object> backupData = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
                dataStore.clear();
                dataStore.putAll(backupData);
                System.out.println("Data restored from backup: " + backupFileName);
            } else {
                System.err.println("Backup file not found: " + backupFileName);
            }
        } catch (Exception e) {
            System.err.println("Error restoring from backup: " + e.getMessage());
        }
    }
    
    // Async save to avoid blocking operations
    private void saveDataAsync() {
        // In a real application, you might use @Async or a separate thread pool
        // For simplicity, we'll save synchronously but you can enhance this
        try {
            objectMapper.writeValue(new File(DATA_FILE), dataStore);
        } catch (Exception e) {
            System.err.println("Error in async save: " + e.getMessage());
        }
    }
    
    // Export data in different formats
    public String exportDataAsJson() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dataStore);
        } catch (Exception e) {
            return "Error exporting data: " + e.getMessage();
        }
    }
    
    // Clear all data (useful for testing)
    public void clearAllData() {
        dataStore.clear();
        initializeEmptyData();
        saveDataAsync();
        System.out.println("All data cleared and reinitialized");
    }
    
    // Get raw data store (for admin purposes)
    public Map<String, Object> getRawDataStore() {
        return new HashMap<>(dataStore);
    }
}