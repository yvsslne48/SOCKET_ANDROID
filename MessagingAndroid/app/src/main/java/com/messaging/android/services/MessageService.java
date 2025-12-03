package com.messaging.android.services;



import com.messaging.models.Message;
import com.messaging.models.MessageType;

/**
 * Same Strategy pattern implementation as desktop version
 * Demonstrates code reusability and consistency across platforms
 */
public class MessageService {
    private final ConnectionService connectionService;

    public MessageService() {
        this.connectionService = ConnectionService.getInstance();
    }

    public void sendTextMessage(String receiverId, String content) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.TEXT)
                .content(content)
                .build();

        connectionService.sendMessage(message);
    }

    public void sendEmoji(String receiverId, String emoji) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.EMOJI)
                .content(emoji)
                .build();

        connectionService.sendMessage(message);
    }

    public void sendImage(String receiverId, byte[] imageData, String fileName) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.IMAGE)
                .data(imageData)
                .fileName(fileName)
                .fileSize(imageData.length)
                .build();

        connectionService.sendMessage(message);
    }

    public void sendFile(String receiverId, byte[] fileData, String fileName) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.FILE)
                .data(fileData)
                .fileName(fileName)
                .fileSize(fileData.length)
                .build();

        connectionService.sendMessage(message);
    }

    public void sendAudio(String receiverId, byte[] audioData, String fileName) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.AUDIO)
                .data(audioData)
                .fileName(fileName)
                .fileSize(audioData.length)
                .build();

        connectionService.sendMessage(message);
    }

    public void initiateVideoCall(String receiverId) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.VIDEO_CALL)
                .content("CALL_INIT")
                .build();

        connectionService.sendMessage(message);
    }

    public void initiateAudioCall(String receiverId) {
        Message message = new Message.Builder()
                .senderId(connectionService.getUserId())
                .receiverId(receiverId)
                .type(MessageType.AUDIO_CALL)
                .content("CALL_INIT")
                .build();

        connectionService.sendMessage(message);
    }
}