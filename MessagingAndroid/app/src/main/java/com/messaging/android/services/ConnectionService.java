package com.messaging.android.services;


import com.messaging.models.Message;
import com.messaging.models.MessageType;

import java.io.*;
        import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Android version of ConnectionService with same Singleton pattern
 * Maintains consistency with desktop client architecture
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

    /**
     * Connects to server in a background thread to avoid NetworkOnMainThreadException
     */
    public void connectAsync(String serverAddress, int port, String userId,
                             ConnectionCallback callback) {
        new Thread(() -> {
            boolean success = connect(serverAddress, port, userId);
            if (callback != null) {
                callback.onConnectionResult(success);
            }
        }).start();
    }

    private boolean connect(String serverAddress, int port, String userId) {
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
            e.printStackTrace();
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
                        e.printStackTrace();
                    }
                    break;
                }
            }

            if (messageListener != null) {
                messageListener.onDisconnected();
            }
        });
        receiverThread.start();
    }

    public synchronized void sendMessage(Message message) {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        new Thread(() -> {
            try {
                output.writeObject(message);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
                connected = false;
            }
        }).start();
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
                e.printStackTrace();
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

    public interface MessageListener {
        void onMessageReceived(Message message);
        void onDisconnected();
    }

    public interface ConnectionCallback {
        void onConnectionResult(boolean success);
    }
}