package com.messaging.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Message class following the Builder pattern for flexible object creation
 * Implements Serializable for network transmission
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String senderId;
    private String receiverId;
    private MessageType type;
    private String content;
    private byte[] data;
    private long fileSize;
    private String fileName;
    private LocalDateTime timestamp;

    private Message(Builder builder) {
        this.id = builder.id;
        this.senderId = builder.senderId;
        this.receiverId = builder.receiverId;
        this.type = builder.type;
        this.content = builder.content;
        this.data = builder.data;
        this.fileSize = builder.fileSize;
        this.fileName = builder.fileName;
        this.timestamp = builder.timestamp;
    }

    // Getters
    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public MessageType getType() { return type; }
    public String getContent() { return content; }
    public byte[] getData() { return data; }
    public long getFileSize() { return fileSize; }
    public String getFileName() { return fileName; }
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * Builder pattern implementation for flexible Message creation
     * This pattern helps maintain immutability and makes code more readable
     */
    public static class Builder {
        private String id = UUID.randomUUID().toString();
        private String senderId;
        private String receiverId;
        private MessageType type;
        private String content;
        private byte[] data;
        private long fileSize;
        private String fileName;
        private LocalDateTime timestamp = LocalDateTime.now();

        public Builder senderId(String senderId) {
            this.senderId = senderId;
            return this;
        }

        public Builder receiverId(String receiverId) {
            this.receiverId = receiverId;
            return this;
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder data(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}