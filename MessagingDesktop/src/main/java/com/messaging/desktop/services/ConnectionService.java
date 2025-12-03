package com.messaging.desktop.services;

import com.messaging.models.Message;
import com.messaging.models.MessageType;

import java.io.*;
        import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Singleton pattern for managing server connection
 * Separates connection logic from UI (Single Responsibility Principle)
 */
public class ConnectionService {
    private static ConnectionService instance;

    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String userId;
    private volatile boolean connected;

    private final BlockingQueue<Message> messageQueue;
    private MessageListener messageListener;

    private ConnectionService() {
        this.messageQueue = new LinkedBlockingQueue<>();
        this.connected = false;
    }

    public static synchronized ConnectionService getInstance() {
        if (instance == null) {
            instance = new ConnectionService();
        }
        return instance;
    }

    public boolean connect(String serverAddress, int port, String userId) {
        try {
            this.userId = userId;
            socket = new Socket(serverAddress, port);

            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());

            connected = true;

            // Send initial connection message
            Message initMessage = new Message.Builder()
                    .senderId(userId)
                    .type(MessageType.TEXT)
                    .content("CONNECT")
                    .build();
            sendMessage(initMessage);

            // Start message receiver thread
            startMessageReceiver();

            return true;

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    private void startMessageReceiver() {
        Thread receiverThread = new Thread(() -> {
            while (connected) {
                try {
                    Message message = (Message) input.readObject();

                    if (messageListener != null) {
                        messageListener.onMessageReceived(message);
                    }

                    messageQueue.offer(message);

                } catch (EOFException e) {
                    connected = false;
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    if (connected) {
                        System.err.println("Error receiving message: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public synchronized void sendMessage(Message message) {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            connected = false;
        }
    }

    public void disconnect() {
        if (connected) {
            try {
                Message disconnectMsg = new Message.Builder()
                        .senderId(userId)
                        .type(MessageType.DISCONNECT)
                        .build();
                sendMessage(disconnectMsg);
            } catch (Exception e) {
                // Ignore
            }

            connected = false;

            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error disconnecting: " + e.getMessage());
            }
        }
    }

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * Observer pattern interface for message notifications
     */
    public interface MessageListener {
        void onMessageReceived(Message message);
    }
}