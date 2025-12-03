package com.messaging.server;

import com.messaging.models.Message;
import com.messaging.models.User;

import java.util.*;
        import java.util.concurrent.ConcurrentHashMap;

/**
 * MessageBroker implements Singleton and Observer patterns
 * Singleton: Ensures single instance for managing all client connections
 * Observer: Notifies registered clients of new messages
 */
public class MessageBroker {
    private static MessageBroker instance;
    private final Map<String, ClientHandler> connectedClients;
    private final Map<String, User> users;

    private MessageBroker() {
        // ConcurrentHashMap for thread-safe operations
        this.connectedClients = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
    }

    /**
     * Singleton pattern: Thread-safe lazy initialization
     */
    public static synchronized MessageBroker getInstance() {
        if (instance == null) {
            instance = new MessageBroker();
        }
        return instance;
    }

    public void registerClient(String userId, ClientHandler handler) {
        connectedClients.put(userId, handler);
        System.out.println("Client registered: " + userId);
    }

    public void unregisterClient(String userId) {
        connectedClients.remove(userId);
        User user = users.get(userId);
        if (user != null) {
            user.setOnline(false);
        }
        System.out.println("Client unregistered: " + userId);
    }

    public void registerUser(User user) {
        users.put(user.getId(), user);
        user.setOnline(true);
    }

    /**
     * Routes messages to appropriate recipients
     * Implements the Observer pattern by notifying registered observers (clients)
     */
    public void routeMessage(Message message) {
        String receiverId = message.getReceiverId();

        // Broadcast to all if receiver is null
        if (receiverId == null || receiverId.equals("ALL")) {
            broadcastMessage(message);
        } else {
            // Send to specific client
            ClientHandler receiver = connectedClients.get(receiverId);
            if (receiver != null) {
                receiver.sendMessage(message);
            } else {
                System.out.println("Receiver not found: " + receiverId);
            }
        }
    }

    private void broadcastMessage(Message message) {
        connectedClients.values().forEach(client -> {
            if (!client.getUserId().equals(message.getSenderId())) {
                client.sendMessage(message);
            }
        });
    }

    public List<User> getOnlineUsers() {
        List<User> onlineUsers = new ArrayList<>();
        users.values().stream()
                .filter(User::isOnline)
                .forEach(onlineUsers::add);
        return onlineUsers;
    }
}