package com.messaging.desktop.utils;

import com.messaging.models.Message;

public class ChatMessageItem {
    private final Message message;
    private final boolean isSent; // true if sent by current user

    public ChatMessageItem(Message message, boolean isSent) {
        this.message = message;
        this.isSent = isSent;
    }

    public Message getMessage() {
        return message;
    }

    public boolean isSent() {
        return isSent;
    }
}