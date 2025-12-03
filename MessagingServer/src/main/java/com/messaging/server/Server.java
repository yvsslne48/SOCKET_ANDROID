package com.messaging.server;



import java.io.*;
        import java.net.*;
        import java.util.*;
        import java.util.concurrent.*;

/**
 * Main server class implementing the Observer pattern through MessageBroker
 * Uses thread pool for handling multiple clients efficiently
 */
public class Server {
    private static final int PORT = 8888;
    private static final int MAX_CLIENTS = 100;

    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final MessageBroker messageBroker;
    private volatile boolean running;

    public Server() {
        // Using thread pool follows best practices for scalability
        this.threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
        this.messageBroker = MessageBroker.getInstance();
        this.running = false;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Server started on port " + PORT);

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " +
                            clientSocket.getInetAddress().getHostAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, messageBroker);
                    threadPool.execute(handler);

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (Exception e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();

        // Add shutdown hook for graceful termination
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}