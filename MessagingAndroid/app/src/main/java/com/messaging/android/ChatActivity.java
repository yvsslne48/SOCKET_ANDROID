package com.messaging.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.messaging.android.adapters.ChatAdapter;
import com.messaging.android.services.ConnectionService;
import com.messaging.android.services.MessageService;
import com.messaging.android.services.FileTransferService;
import com.messaging.models.Message;
import com.messaging.models.MessageType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Chat Activity implementing Observer pattern through MessageListener
 * Updates UI when new messages arrive
 */
public class ChatActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_FILE_REQUEST = 2;

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    private EditText receiverIdInput;
    private EditText messageInput;
    private Button sendButton;
    private Button sendImageButton;
    private Button sendFileButton;
    private Button audioCallButton;
    private Button videoCallButton;

    private ConnectionService connectionService;
    private MessageService messageService;
    private FileTransferService fileTransferService;
    private Handler mainHandler;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = getIntent().getStringExtra("userId");

        connectionService = ConnectionService.getInstance();
        messageService = new MessageService();
        fileTransferService = new FileTransferService(this);
        mainHandler = new Handler(Looper.getMainLooper());

        initializeViews();
        setupRecyclerView();
        setupListeners();
        setupMessageListener();

        // Set action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat - " + currentUserId);
        }
    }

    private void initializeViews() {
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        receiverIdInput = findViewById(R.id.receiverIdInput);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        sendImageButton = findViewById(R.id.sendImageButton);
        sendFileButton = findViewById(R.id.sendFileButton);
        audioCallButton = findViewById(R.id.audioCallButton);
        videoCallButton = findViewById(R.id.videoCallButton);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserId, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);

        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        sendButton.setOnClickListener(v -> handleSendMessage());

        sendImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        sendFileButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, PICK_FILE_REQUEST);
        });

        audioCallButton.setOnClickListener(v -> {
            String receiverId = receiverIdInput.getText().toString().trim();
            if (!receiverId.isEmpty()) {
                messageService.initiateAudioCall(receiverId);
                addSystemMessage("📞 Initiating audio call with " + receiverId);
            } else {
                Toast.makeText(this, "Please enter receiver ID", Toast.LENGTH_SHORT).show();
            }
        });

        videoCallButton.setOnClickListener(v -> {
            String receiverId = receiverIdInput.getText().toString().trim();
            if (!receiverId.isEmpty()) {
                messageService.initiateVideoCall(receiverId);
                addSystemMessage("📹 Initiating video call with " + receiverId);
            } else {
                Toast.makeText(this, "Please enter receiver ID", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupMessageListener() {
        connectionService.setMessageListener(new ConnectionService.MessageListener() {
            @Override
            public void onMessageReceived(Message message) {
                mainHandler.post(() -> {
                    handleIncomingMessage(message);
                });
            }

            @Override
            public void onDisconnected() {
                mainHandler.post(() -> {
                    Toast.makeText(ChatActivity.this,
                            "Disconnected from server", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private void handleSendMessage() {
        String receiverId = receiverIdInput.getText().toString().trim();
        String content = messageInput.getText().toString().trim();

        if (receiverId.isEmpty()) {
            Toast.makeText(this, "Please enter receiver ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            return;
        }

        messageService.sendTextMessage(receiverId, content);

        // Add to local list
        Message sentMessage = new Message.Builder()
                .senderId(currentUserId)
                .receiverId(receiverId)
                .type(MessageType.TEXT)
                .content(content)
                .build();

        addMessageToList(sentMessage);
        messageInput.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            String receiverId = receiverIdInput.getText().toString().trim();
            if (receiverId.isEmpty()) {
                Toast.makeText(this, "Please enter receiver ID", Toast.LENGTH_SHORT).show();
                return;
            }

            Uri fileUri = data.getData();

            if (fileUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(fileUri);
                    byte[] fileData = fileTransferService.readFile(inputStream);
                    String fileName = getFileNameFromUri(fileUri);

                    // Ensure file has proper extension
                    fileName = ensureFileExtension(fileUri, fileName);

                    if (requestCode == PICK_IMAGE_REQUEST) {
                        messageService.sendImage(receiverId, fileData, fileName);
                        addSystemMessage("📷 Image sent: " + fileName);
                    } else if (requestCode == PICK_FILE_REQUEST) {
                        messageService.sendFile(receiverId, fileData, fileName);
                        addSystemMessage("📎 File sent: " + fileName + " (" +
                                FileTransferService.formatFileSize(fileData.length) + ")");
                    }

                } catch (IOException e) {
                    Toast.makeText(this, "Failed to read file: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    private String ensureFileExtension(Uri uri, String fileName) {
        // Check if filename already has an extension
        if (fileName.contains(".")) {
            return fileName;
        }

        // Get MIME type and add appropriate extension
        String mimeType = getContentResolver().getType(uri);
        String extension = "";

        if (mimeType != null) {
            switch (mimeType) {
                case "image/jpeg":
                    extension = ".jpg";
                    break;
                case "image/png":
                    extension = ".png";
                    break;
                case "image/gif":
                    extension = ".gif";
                    break;
                case "image/bmp":
                    extension = ".bmp";
                    break;
                case "application/pdf":
                    extension = ".pdf";
                    break;
                case "application/msword":
                    extension = ".doc";
                    break;
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    extension = ".docx";
                    break;
                case "text/plain":
                    extension = ".txt";
                    break;
                case "audio/mpeg":
                    extension = ".mp3";
                    break;
                case "video/mp4":
                    extension = ".mp4";
                    break;
                default:
                    // Try to get extension from URI path
                    String path = uri.getPath();
                    if (path != null && path.contains(".")) {
                        extension = path.substring(path.lastIndexOf("."));
                    }
                    break;
            }
        }

        return fileName + extension;
    }

    private String getFileNameFromUri(Uri uri) {
        String fileName = "file_" + System.currentTimeMillis();

        // Try to get real filename from content resolver
        android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        // Fallback: try to get from path
        if (fileName.equals("file_" + System.currentTimeMillis())) {
            String path = uri.getPath();
            if (path != null) {
                int lastSlash = path.lastIndexOf("/");
                if (lastSlash != -1) {
                    fileName = path.substring(lastSlash + 1);
                }
            }
        }

        return fileName;
    }

    private void handleIncomingMessage(Message message) {
        switch (message.getType()) {
            case TEXT:
            case EMOJI:
                addMessageToList(message);
                break;

            case IMAGE:
                handleImageMessage(message);
                addMessageToList(message);
                break;

            case FILE:
                handleFileMessage(message);
                addMessageToList(message);
                break;

            case AUDIO:
                handleAudioMessage(message);
                addMessageToList(message);
                break;

            case AUDIO_CALL:
                addMessageToList(message);
                handleCallRequest(message, "Audio");
                break;

            case VIDEO_CALL:
                addMessageToList(message);
                handleCallRequest(message, "Video");
                break;

            case CALL_SIGNAL:
                handleCallSignal(message);
                break;

            default:
                addMessageToList(message);
                break;
        }
    }

    private void handleImageMessage(Message message) {
        try {
            fileTransferService.saveFile(message.getData(), message.getFileName());

            String sizeStr = FileTransferService.formatFileSize(message.getFileSize());
            Toast.makeText(this, "Image saved: " + message.getFileName() + " (" + sizeStr + ")",
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to save image: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleFileMessage(Message message) {
        try {
            fileTransferService.saveFile(message.getData(), message.getFileName());

            String sizeStr = FileTransferService.formatFileSize(message.getFileSize());
            Toast.makeText(this, "File saved: " + message.getFileName() + " (" + sizeStr + ")",
                    Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to save file: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleAudioMessage(Message message) {
        try {
            fileTransferService.saveFile(message.getData(), message.getFileName());

            String sizeStr = FileTransferService.formatFileSize(message.getFileSize());
            Toast.makeText(this, "Audio saved: " + message.getFileName() + " (" + sizeStr + ")",
                    Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Toast.makeText(this, "Failed to save audio: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void handleCallRequest(Message message, String callType) {
        // Show alert dialog for incoming call
        new AlertDialog.Builder(this)
                .setTitle("Incoming " + callType + " Call")
                .setMessage(message.getSenderId() + " is calling you...")
                .setIcon(callType.equals("Audio") ?
                        android.R.drawable.ic_btn_speak_now :
                        android.R.drawable.ic_menu_camera)
                .setPositiveButton("Accept ✓", (dialog, which) -> {
                    addSystemMessage("✓ Call accepted with " + message.getSenderId());
                    sendCallResponse(message.getSenderId(), "ACCEPT", callType);
                    showInCallScreen(message.getSenderId(), callType);
                })
                .setNegativeButton("Decline ✗", (dialog, which) -> {
                    addSystemMessage("✗ Call declined from " + message.getSenderId());
                    sendCallResponse(message.getSenderId(), "DECLINE", callType);
                })
                .setCancelable(false)
                .show();
    }

    private void sendCallResponse(String receiverId, String response, String callType) {
        Message callResponse = new Message.Builder()
                .senderId(currentUserId)
                .receiverId(receiverId)
                .type(MessageType.CALL_SIGNAL)
                .content(response + ":" + callType)
                .build();
        connectionService.sendMessage(callResponse);
    }

    private void showInCallScreen(String otherUser, String callType) {
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate custom layout
        android.view.View callView = getLayoutInflater().inflate(R.layout.dialog_in_call, null);

        TextView callerNameText = callView.findViewById(R.id.callerNameText);
        TextView callStatusText = callView.findViewById(R.id.callStatusText);
        TextView callDurationText = callView.findViewById(R.id.callDurationText);
        TextView callTypeIcon = callView.findViewById(R.id.callTypeIcon);
        Button endCallButton = callView.findViewById(R.id.endCallButton);

        callerNameText.setText(otherUser);
        callStatusText.setText("Call in progress...");
        callTypeIcon.setText(callType.equals("Audio") ? "📞" : "📹");

        // Simulate call duration
        final int[] seconds = {0};
        Handler durationHandler = new Handler(Looper.getMainLooper());
        Runnable durationRunnable = new Runnable() {
            @Override
            public void run() {
                seconds[0]++;
                int mins = seconds[0] / 60;
                int secs = seconds[0] % 60;
                callDurationText.setText(String.format("%02d:%02d", mins, secs));
                durationHandler.postDelayed(this, 1000);
            }
        };
        durationHandler.post(durationRunnable);

        builder.setView(callView);
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();

        endCallButton.setOnClickListener(v -> {
            durationHandler.removeCallbacks(durationRunnable);
            addSystemMessage("Call ended with " + otherUser + " (Duration: " + callDurationText.getText() + ")");
            sendCallResponse(otherUser, "END", callType);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void handleCallSignal(Message message) {
        String content = message.getContent();
        String sender = message.getSenderId();

        if (content.startsWith("ACCEPT")) {
            addSystemMessage("✓ " + sender + " accepted your call");
            String callType = content.contains(":") ? content.split(":")[1] : "Call";
            showInCallScreen(sender, callType);
        } else if (content.startsWith("DECLINE")) {
            addSystemMessage("✗ " + sender + " declined your call");
            Toast.makeText(this, sender + " declined your call", Toast.LENGTH_LONG).show();
        } else if (content.startsWith("END")) {
            addSystemMessage(sender + " ended the call");
        }
    }

    private void addMessageToList(Message message) {
        messageList.add(message);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.smoothScrollToPosition(messageList.size() - 1);
    }

    private void addSystemMessage(String content) {
        Message systemMessage = new Message.Builder()
                .senderId("System")
                .type(MessageType.TEXT)
                .content(content)
                .build();

        addMessageToList(systemMessage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect here - let MainActivity handle it
    }
}