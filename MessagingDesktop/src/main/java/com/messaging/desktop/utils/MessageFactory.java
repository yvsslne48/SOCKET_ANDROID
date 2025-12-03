package com.messaging.desktop.utils;

import com.messaging.models.Message;
import com.messaging.models.MessageType;

/**
 * Factory pattern for creating Message objects
 * Simplifies message creation with common patterns
 */
public class MessageFactory {

    /**
     * Create a text message
     */
    public static Message createTextMessage(String senderId, String receiverId, String content) {
        return new Message.Builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(MessageType.TEXT)
                .content(content)
                .build();
    }

    /**
     * Create an emoji message
     */
    public static Message createEmojiMessage(String senderId, String receiverId, String emoji) {
        return new Message.Builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(MessageType.EMOJI)
                .content(emoji)
                .build();
    }

    /**
     * Create an image message
     */
    public static Message createImageMessage(String senderId, String receiverId,
                                             byte[] imageData, String fileName) {
        return new Message.Builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(MessageType.IMAGE)
                .data(imageData)
                .fileName(fileName)
                .fileSize(imageData.length)
                .build();
    }

    /**
     * Create a file message
     */
    public static Message createFileMessage(String senderId, String receiverId,
                                            byte[] fileData, String fileName) {
        return new Message.Builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(MessageType.FILE)
                .data(fileData)
                .fileName(fileName)
                .fileSize(fileData.length)
                .build();
    }

    /**
     * Create an audio message
     */
    public static Message createAudioMessage(String senderId, String receiverId,
                                             byte[] audioData, String fileName) {
        return new Message.Builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(MessageType.AUDIO)
                .data(audioData)
                .fileName(fileName)
                .fileSize(audioData.length)
                .build();
    }

    /**
     * Create a call initiation message
     */
    public static Message createCallMessage(String senderId, String receiverId,
                                            MessageType callType) {
        return new Message.Builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .type(callType)
                .content("CALL_INIT")
                .build();
    }

    /**
     * Create a disconnect message
     */
    public static Message createDisconnectMessage(String senderId) {
        return new Message.Builder()
                .senderId(senderId)
                .type(MessageType.DISCONNECT)
                .build();
    }
}