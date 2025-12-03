package com.messaging.desktop.controllers;

import com.messaging.desktop.services.ConnectionService;
import com.messaging.desktop.services.MessageService;
import com.messaging.desktop.services.FileTransferService;
import com.messaging.models.Message;
import com.messaging.models.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Main controller with rich message display
 */
public class MainController {
    @FXML private TextField serverAddressField;
    @FXML private TextField portField;
    @FXML private TextField userIdField;
    @FXML private Button connectButton;

    @FXML private TextField receiverIdField;
    @FXML private TextField messageField;
    @FXML private Button sendButton;
    @FXML private Button sendImageButton;
    @FXML private Button sendFileButton;
    @FXML private Button audioCallButton;
    @FXML private Button videoCallButton;

    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatBox;

    private ConnectionService connectionService;
    private MessageService messageService;
    private FileTransferService fileTransferService;

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        connectionService = ConnectionService.getInstance();
        messageService = new MessageService();
        fileTransferService = new FileTransferService();

        // Set default values
        serverAddressField.setText("localhost");
        portField.setText("8888");

        // Disable chat controls initially
        setChatControlsDisabled(true);

        // Setup message listener
        connectionService.setMessageListener(this::handleIncomingMessage);

        // Allow Enter key to send message
        messageField.setOnAction(event -> handleSendMessage());

        // Auto-scroll to bottom
        chatBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
    }

    @FXML
    private void handleConnect() {
        String serverAddress = serverAddressField.getText().trim();
        String portStr = portField.getText().trim();
        String userId = userIdField.getText().trim();

        if (userId.isEmpty()) {
            showAlert("Error", "Please enter a user ID", Alert.AlertType.ERROR);
            return;
        }

        if (serverAddress.isEmpty()) {
            showAlert("Error", "Please enter server address", Alert.AlertType.ERROR);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            boolean connected = connectionService.connect(serverAddress, port, userId);

            if (connected) {
                setChatControlsDisabled(false);
                connectButton.setDisable(true);
                serverAddressField.setDisable(true);
                portField.setDisable(true);
                userIdField.setDisable(true);

                addSystemMessage("✓ Connected to server");
                showAlert("Success", "Connected to server successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to connect to server. Check address and port.", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "Port must be a number", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSendMessage() {
        String receiverId = receiverIdField.getText().trim();
        String content = messageField.getText().trim();

        if (receiverId.isEmpty()) {
            showAlert("Warning", "Please enter receiver ID", Alert.AlertType.WARNING);
            return;
        }

        if (content.isEmpty()) {
            return;
        }

        messageService.sendTextMessage(receiverId, content);
        addTextMessage(content, true);
        messageField.clear();
    }

    @FXML
    private void handleSendImage() {
        String receiverId = receiverIdField.getText().trim();
        if (receiverId.isEmpty()) {
            showAlert("Warning", "Please enter receiver ID first", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File file = fileChooser.showOpenDialog(sendImageButton.getScene().getWindow());
        if (file != null) {
            try {
                byte[] imageData = fileTransferService.readFile(file);
                messageService.sendImage(receiverId, imageData, file.getName());

                // Show image preview in chat
                addImageMessage(file, true);

            } catch (IOException e) {
                showAlert("Error", "Failed to read image file: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleSendFile() {
        String receiverId = receiverIdField.getText().trim();
        if (receiverId.isEmpty()) {
            showAlert("Warning", "Please enter receiver ID first", Alert.AlertType.WARNING);
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File");

        File file = fileChooser.showOpenDialog(sendFileButton.getScene().getWindow());
        if (file != null) {
            try {
                byte[] fileData = fileTransferService.readFile(file);
                messageService.sendFile(receiverId, fileData, file.getName());

                addFileMessage(file.getName(), file.length(), true);

            } catch (IOException e) {
                showAlert("Error", "Failed to read file: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleAudioCall() {
        String receiverId = receiverIdField.getText().trim();
        if (!receiverId.isEmpty()) {
            messageService.initiateAudioCall(receiverId);
            addSystemMessage("📞 Initiating audio call with " + receiverId);
        } else {
            showAlert("Warning", "Please enter receiver ID", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleVideoCall() {
        String receiverId = receiverIdField.getText().trim();
        if (!receiverId.isEmpty()) {
            messageService.initiateVideoCall(receiverId);
            addSystemMessage("📹 Initiating video call with " + receiverId);
        } else {
            showAlert("Warning", "Please enter receiver ID", Alert.AlertType.WARNING);
        }
    }

    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            switch (message.getType()) {
                case TEXT:
                case EMOJI:
                    addTextMessage(message.getContent(), false, message.getSenderId());
                    break;

                case IMAGE:
                    handleImageReceived(message);
                    break;

                case FILE:
                    handleFileReceived(message);
                    break;

                case AUDIO:
                    handleAudioReceived(message);
                    break;

                case AUDIO_CALL:
                    addSystemMessage("📞 " + message.getSenderId() + " is calling (audio)...");
                    showCallAlert(message.getSenderId(), "Audio Call");
                    break;

                case VIDEO_CALL:
                    addSystemMessage("📹 " + message.getSenderId() + " is calling (video)...");
                    showCallAlert(message.getSenderId(), "Video Call");
                    break;

                case CALL_SIGNAL:
                    handleCallSignal(message);
                    break;

                default:
                    addTextMessage(message.getContent() != null ? message.getContent() : "", false, message.getSenderId());
                    break;
            }
        });
    }

    private void handleImageReceived(Message message) {
        try {
            // Save image
            fileTransferService.saveFile(message.getData(), message.getFileName());
            String savePath = fileTransferService.getDownloadDirectory() + message.getFileName();

            // Display image in chat with download button
            addReceivedImageMessage(savePath, message.getFileName(), message.getSenderId());

        } catch (Exception e) {
            showAlert("Error", "Failed to save image: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleFileReceived(Message message) {
        try {
            // Save file
            fileTransferService.saveFile(message.getData(), message.getFileName());
            String savePath = fileTransferService.getDownloadDirectory() + message.getFileName();

            // Display file in chat with download button
            addReceivedFileMessage(message.getFileName(), message.getFileSize(), savePath, message.getSenderId());

        } catch (Exception e) {
            showAlert("Error", "Failed to save file: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleAudioReceived(Message message) {
        try {
            fileTransferService.saveFile(message.getData(), message.getFileName());
            String savePath = fileTransferService.getDownloadDirectory() + message.getFileName();

            addReceivedFileMessage(message.getFileName(), message.getFileSize(), savePath, message.getSenderId());
            addSystemMessage("🎵 Audio saved to: " + savePath);

        } catch (Exception e) {
            showAlert("Error", "Failed to save audio: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ========== UI Message Display Methods ==========

    private void addTextMessage(String content, boolean isSent) {
        addTextMessage(content, isSent, "You");
    }

    private void addTextMessage(String content, boolean isSent, String sender) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setSpacing(10);

        VBox bubble = createMessageBubble(content, isSent);

        if (isSent) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getChildren().add(bubble);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            Label senderLabel = new Label(sender + ":");
            senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");
            VBox leftBox = new VBox(3, senderLabel, bubble);
            messageBox.getChildren().add(leftBox);
        }

        chatBox.getChildren().add(messageBox);
    }

    private void addImageMessage(File imageFile, boolean isSent) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setSpacing(10);

        VBox content = new VBox(5);
        content.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                "; -fx-background-radius: 10; -fx-padding: 10;");
        content.setMaxWidth(300);

        try {
            ImageView imageView = new ImageView(new Image(new FileInputStream(imageFile)));
            imageView.setFitWidth(280);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Label fileNameLabel = new Label("📷 " + imageFile.getName());
            fileNameLabel.setStyle("-fx-font-size: 12px;");

            content.getChildren().addAll(imageView, fileNameLabel);

        } catch (Exception e) {
            Label errorLabel = new Label("❌ Failed to load image");
            content.getChildren().add(errorLabel);
        }

        if (isSent) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }

        messageBox.getChildren().add(content);
        chatBox.getChildren().add(messageBox);
    }

    private void addReceivedImageMessage(String imagePath, String fileName, String sender) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setSpacing(10);
        messageBox.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(3);
        Label senderLabel = new Label(sender + ":");
        senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");

        VBox content = new VBox(5);
        content.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-padding: 10;");
        content.setMaxWidth(300);

        try {
            File imageFile = new File(imagePath);
            ImageView imageView = new ImageView(new Image(new FileInputStream(imageFile)));
            imageView.setFitWidth(280);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);

            Label fileNameLabel = new Label("📷 " + fileName);
            fileNameLabel.setStyle("-fx-font-size: 12px;");

            Button downloadButton = new Button("💾 Open File Location");
            downloadButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11px;");
            downloadButton.setOnAction(e -> openFileLocation(imagePath));

            content.getChildren().addAll(imageView, fileNameLabel, downloadButton);

        } catch (Exception e) {
            Label errorLabel = new Label("❌ Failed to load image");
            content.getChildren().add(errorLabel);
        }

        leftBox.getChildren().addAll(senderLabel, content);
        messageBox.getChildren().add(leftBox);
        chatBox.getChildren().add(messageBox);
    }

    private void addFileMessage(String fileName, long fileSize, boolean isSent) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));

        VBox bubble = new VBox(5);
        bubble.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                "; -fx-background-radius: 10; -fx-padding: 10;");
        bubble.setMaxWidth(300);

        Label fileIcon = new Label("📎");
        fileIcon.setStyle("-fx-font-size: 24px;");

        Label fileNameLabel = new Label(fileName);
        fileNameLabel.setStyle("-fx-font-weight: bold;");
        fileNameLabel.setWrapText(true);

        Label fileSizeLabel = new Label(formatFileSize(fileSize));
        fileSizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        bubble.getChildren().addAll(fileIcon, fileNameLabel, fileSizeLabel);

        if (isSent) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }

        messageBox.getChildren().add(bubble);
        chatBox.getChildren().add(messageBox);
    }

    private void addReceivedFileMessage(String fileName, long fileSize, String filePath, String sender) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setAlignment(Pos.CENTER_LEFT);

        VBox leftBox = new VBox(3);
        Label senderLabel = new Label(sender + ":");
        senderLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");

        VBox bubble = new VBox(5);
        bubble.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10; -fx-padding: 10;");
        bubble.setMaxWidth(300);

        Label fileIcon = new Label("📎");
        fileIcon.setStyle("-fx-font-size: 24px;");

        Label fileNameLabel = new Label(fileName);
        fileNameLabel.setStyle("-fx-font-weight: bold;");
        fileNameLabel.setWrapText(true);

        Label fileSizeLabel = new Label(formatFileSize(fileSize));
        fileSizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

        Button downloadButton = new Button("💾 Open File Location");
        downloadButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px;");
        downloadButton.setOnAction(e -> openFileLocation(filePath));

        bubble.getChildren().addAll(fileIcon, fileNameLabel, fileSizeLabel, downloadButton);
        leftBox.getChildren().addAll(senderLabel, bubble);
        messageBox.getChildren().add(leftBox);
        chatBox.getChildren().add(messageBox);
    }

    private void addSystemMessage(String content) {
        HBox messageBox = new HBox();
        messageBox.setPadding(new Insets(5));
        messageBox.setAlignment(Pos.CENTER);

        Label systemLabel = new Label(content);
        systemLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px; -fx-font-style: italic;");

        messageBox.getChildren().add(systemLabel);
        chatBox.getChildren().add(messageBox);
    }

    private VBox createMessageBubble(String content, boolean isSent) {
        VBox bubble = new VBox();
        bubble.setStyle("-fx-background-color: " + (isSent ? "#DCF8C6" : "#FFFFFF") +
                "; -fx-background-radius: 10; -fx-padding: 10;");
        bubble.setMaxWidth(400);

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px;");

        bubble.getChildren().add(contentLabel);
        return bubble;
    }

    private void openFileLocation(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                Desktop.getDesktop().open(file.getParentFile());
            } else {
                showAlert("Error", "File not found: " + filePath, Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Error", "Failed to open file location: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ========== Call Handling Methods ==========

    private void showCallAlert(String caller, String callType) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Incoming " + callType);
        alert.setHeaderText(null);
        alert.setContentText(caller + " is calling you...");

        ButtonType acceptButton = new ButtonType("Accept ✓");
        ButtonType declineButton = new ButtonType("Decline ✗");
        alert.getButtonTypes().setAll(acceptButton, declineButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == acceptButton) {
                addSystemMessage("✓ Call accepted with " + caller);
                sendCallResponse(caller, "ACCEPT", callType);
                showInCallWindow(caller, callType);
            } else {
                addSystemMessage("✗ Call declined from " + caller);
                sendCallResponse(caller, "DECLINE", callType);
            }
        });
    }

    private void sendCallResponse(String receiverId, String response, String callType) {
        try {
            Message callResponse = new Message.Builder()
                    .senderId(connectionService.getUserId())
                    .receiverId(receiverId)
                    .type(MessageType.CALL_SIGNAL)
                    .content(response + ":" + callType)
                    .build();
            connectionService.sendMessage(callResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showInCallWindow(String caller, String callType) {
        // Create custom dialog
        Dialog<ButtonType> callDialog = new Dialog<>();
        callDialog.setTitle(callType + " Call");

        // Set content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: #2C2C2C;");

        // Call icon
        Label iconLabel = new Label(callType.contains("Video") ? "📹" : "📞");
        iconLabel.setStyle("-fx-font-size: 48px;");

        // Caller name
        Label callerLabel = new Label(caller);
        callerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Call status
        Label statusLabel = new Label("Call in progress...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #B0B0B0;");

        // Call duration (simulated)
        Label durationLabel = new Label("00:00");
        durationLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");

        // Start duration timer
        final int[] seconds = {0};
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                    seconds[0]++;
                    int mins = seconds[0] / 60;
                    int secs = seconds[0] % 60;
                    durationLabel.setText(String.format("%02d:%02d", mins, secs));
                })
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();

        content.getChildren().addAll(iconLabel, callerLabel, statusLabel, durationLabel);

        callDialog.getDialogPane().setContent(content);

        // End call button
        ButtonType endCallButton = new ButtonType("End Call", ButtonBar.ButtonData.OK_DONE);
        callDialog.getDialogPane().getButtonTypes().add(endCallButton);

        // Style the button
        Button endButton = (Button) callDialog.getDialogPane().lookupButton(endCallButton);
        endButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20;");

        callDialog.setOnCloseRequest(e -> timeline.stop());

        callDialog.showAndWait().ifPresent(response -> {
            timeline.stop();
            addSystemMessage("Call ended with " + caller + " (Duration: " + durationLabel.getText() + ")");
            sendCallResponse(caller, "END", callType);
        });
    }
    private void handleCallSignal(Message message) {
        String content = message.getContent();
        String sender = message.getSenderId();

        if (content.startsWith("ACCEPT")) {
            addSystemMessage("✓ " + sender + " accepted your call");
            String callType = content.contains(":") ? content.split(":")[1] : "Call";
            showInCallWindow(sender, callType);
        } else if (content.startsWith("DECLINE")) {
            addSystemMessage("✗ " + sender + " declined your call");
            showAlert("Call Declined", sender + " declined your call", Alert.AlertType.INFORMATION);
        } else if (content.startsWith("END")) {
            addSystemMessage(sender + " ended the call");
        }
    }

    // ========== Utility Methods ==========

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private void setChatControlsDisabled(boolean disabled) {
        receiverIdField.setDisable(disabled);
        messageField.setDisable(disabled);
        sendButton.setDisable(disabled);
        sendImageButton.setDisable(disabled);
        sendFileButton.setDisable(disabled);
        audioCallButton.setDisable(disabled);
        videoCallButton.setDisable(disabled);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}