package com.messaging.desktop.utils;

import com.messaging.desktop.services.FileTransferService;
import com.messaging.models.Message;

import java.io.IOException;

/**
 * Strategy pattern for handling different message types
 * Processes incoming messages based on their type
 */
public class MessageHandler {

    private final FileTransferService fileTransferService;

    public MessageHandler() {
        this.fileTransferService = new FileTransferService();
    }

    /**
     * Handle incoming message based on type
     */
    public void handleMessage(Message message, MessageCallback callback) {
        switch (message.getType()) {
            case TEXT:
            case EMOJI:
                callback.onTextMessage(message);
                break;

            case IMAGE:
                handleImageMessage(message, callback);
                break;

            case FILE:
                handleFileMessage(message, callback);
                break;

            case AUDIO:
                handleAudioMessage(message, callback);
                break;

            case AUDIO_CALL:
            case VIDEO_CALL:
                callback.onCallRequest(message);
                break;

            case DISCONNECT:
                callback.onDisconnect(message);
                break;

            default:
                System.out.println("Unknown message type: " + message.getType());
        }
    }

    private void handleImageMessage(Message message, MessageCallback callback) {
        try {
            // Save image automatically
            fileTransferService.saveFile(message.getData(), message.getFileName());
            callback.onFileReceived(message);
        } catch (IOException e) {
            callback.onError("Failed to save image: " + e.getMessage());
        }
    }

    private void handleFileMessage(Message message, MessageCallback callback) {
        try {
            // Save file automatically
            fileTransferService.saveFile(message.getData(), message.getFileName());
            callback.onFileReceived(message);
        } catch (IOException e) {
            callback.onError("Failed to save file: " + e.getMessage());
        }
    }

    private void handleAudioMessage(Message message, MessageCallback callback) {
        try {
            // Save audio file
            fileTransferService.saveFile(message.getData(), message.getFileName());
            callback.onFileReceived(message);
        } catch (IOException e) {
            callback.onError("Failed to save audio: " + e.getMessage());
        }
    }

    /**
     * Callback interface for message handling results
     */
    public interface MessageCallback {
        void onTextMessage(Message message);
        void onFileReceived(Message message);
        void onCallRequest(Message message);
        void onDisconnect(Message message);
        void onError(String error);
    }
}