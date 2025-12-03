package com.messaging.server;

import com.messaging.models.Message;
import com.messaging.models.MessageType;
import com.messaging.models.User;

import java.io.*;
import java.net.Socket;


/**
 * Handles individual client connections
 * Each instance runs in its own thread from the server's thread pool
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final MessageBroker broker;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String userId;
    private volatile boolean running;

    public ClientHandler(Socket socket, MessageBroker broker) {
        this.socket = socket;
        this.broker = broker;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            // ObjectOutputStream must be created before ObjectInputStream
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            // First message should contain user info
            Object obj = input.readObject();
            if (obj instanceof Message) {
                Message initMessage = (Message) obj;
                this.userId = initMessage.getSenderId();

                User user = new User(userId, userId);
                user.setIpAddress(socket.getInetAddress().getHostAddress());
                user.setPort(socket.getPort());

                broker.registerUser(user);
                broker.registerClient(userId, this);

                System.out.println("User connected: " + userId);

                // Send list of online users
                sendOnlineUsers();

                // Message processing loop
                while (running) {
                    try {
                        Object messageObj = input.readObject();

                        if (messageObj instanceof Message) {
                            Message message = (Message) messageObj;

                            if (message.getType() == MessageType.DISCONNECT) {
                                System.out.println("User disconnecting: " + userId);
                                break;
                            }

                            System.out.println("Message received from " + userId +
                                    ": Type=" + message.getType());

                            broker.routeMessage(message);
                        }

                    } catch (EOFException e) {
                        System.out.println("Client disconnected (EOF): " + userId);
                        break;
                    } catch (ClassNotFoundException e) {
                        System.err.println("Unknown message class: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.err.println("First message is not a Message object");
            }

        } catch (EOFException e) {
            System.out.println("Client connection closed unexpectedly: " +
                    (userId != null ? userId : "unknown"));
        } catch (IOException e) {
            System.err.println("Client handler IO error: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found during initialization: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public synchronized void sendMessage(Message message) {
        try {
            if (output != null) {
                output.writeObject(message);
                output.flush();
                output.reset(); // Important: prevents memory leaks with object caching
            }
        } catch (IOException e) {
            System.err.println("Error sending message to " + userId + ": " + e.getMessage());
            running = false;
        }
    }

    private void sendOnlineUsers() {
        try {
            Message userListMessage = new Message.Builder()
                    .type(MessageType.TEXT)
                    .content("ONLINE_USERS")
                    .senderId("SERVER")
                    .receiverId(userId)
                    .build();
            sendMessage(userListMessage);
        } catch (Exception e) {
            System.err.println("Error sending online users: " + e.getMessage());
        }
    }

    public String getUserId() {
        return userId;
    }

    private void cleanup() {
        running = false;

        if (userId != null) {
            broker.unregisterClient(userId);
            System.out.println("Client cleaned up: " + userId);
        }

        try {
            if (input != null) input.close();
        } catch (IOException e) {
            System.err.println("Error closing input stream: " + e.getMessage());
        }

        try {
            if (output != null) output.close();
        } catch (IOException e) {
            System.err.println("Error closing output stream: " + e.getMessage());
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
}