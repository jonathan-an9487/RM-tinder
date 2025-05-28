
// ChatMessage.java
package com.example.swipecard.Chat;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String senderId;
    private String content;
    private Timestamp timestamp;
    private String messageId;

    public ChatMessage() {
        // Required empty constructor for Firestore
    }

    public ChatMessage(String senderId, String content, Timestamp timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

}