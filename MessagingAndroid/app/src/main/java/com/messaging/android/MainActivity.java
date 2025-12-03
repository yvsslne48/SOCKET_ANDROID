package com.messaging.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.messaging.android.services.ConnectionService;

/**
 * Main Activity for connection setup
 * Follows Android MVC pattern with Activity as Controller
 */
public class MainActivity extends AppCompatActivity {
    private EditText serverAddressInput;
    private EditText portInput;
    private EditText userIdInput;
    private Button connectButton;

    private ConnectionService connectionService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionService = ConnectionService.getInstance();

        initializeViews();
        setupListeners();

        // Set default values
        serverAddressInput.setText("192.168.1.4"); // Change to your server IP
        portInput.setText("8888");
    }

    private void initializeViews() {
        serverAddressInput = findViewById(R.id.serverAddressInput);
        portInput = findViewById(R.id.portInput);
        userIdInput = findViewById(R.id.userIdInput);
        connectButton = findViewById(R.id.connectButton);
    }

    private void setupListeners() {
        connectButton.setOnClickListener(v -> handleConnect());
    }

    private void handleConnect() {
        String serverAddress = serverAddressInput.getText().toString().trim();
        String portStr = portInput.getText().toString().trim();
        String userId = userIdInput.getText().toString().trim();

        if (userId.isEmpty()) {
            Toast.makeText(this, "Please enter a user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (serverAddress.isEmpty()) {
            Toast.makeText(this, "Please enter server address", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            connectButton.setEnabled(false);
            connectButton.setText("Connecting...");

            connectionService.connectAsync(serverAddress, port, userId, success -> {
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(MainActivity.this,
                                "Connected successfully", Toast.LENGTH_SHORT).show();

                        // Navigate to chat activity
                        Intent intent = new Intent(MainActivity.this, ChatActivity.class);
                        intent.putExtra("userId", userId);
                        startActivity(intent);

                    } else {
                        Toast.makeText(MainActivity.this,
                                "Connection failed", Toast.LENGTH_SHORT).show();
                        connectButton.setEnabled(true);
                        connectButton.setText("Connect");
                    }
                });
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionService.disconnect();
    }
}