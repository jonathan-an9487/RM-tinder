package com.example.swipecard.Chat;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ChatRoom {
    private String chatId;
    private List<String> participants;
    private String lastMessage;
    private Timestamp lastMessageTime;
    private String lastSenderId;

    // 額外欄位用於顯示
    private String otherUserName;
    private String otherUserImageUrl;

    public ChatRoom() {
        // Required empty constructor for Firestore
    }

    public ChatRoom(String chatId, List<String> participants, String lastMessage,
                    Timestamp lastMessageTime, String lastSenderId) {
        this.chatId = chatId;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.lastSenderId = lastSenderId;
    }


    // Getters and Setters
    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public List<String> getParticipants() {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public String getLastMessage() {
        return lastMessage != null ? lastMessage : "開始聊天吧！";
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTime() {
        if (lastMessageTime == null) {
            return Timestamp.now();
        }
        return lastMessageTime;
    }

    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastSenderId() {
        return lastSenderId;
    }

    public void setLastSenderId(String lastSenderId) {
        this.lastSenderId = lastSenderId;
    }

    public String getOtherUserName() {
        return otherUserName != null ? otherUserName : "Unknown User";
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserImageUrl() {
        return otherUserImageUrl;
    }

    public void setOtherUserImageUrl(String otherUserImageUrl) {
        this.otherUserImageUrl = otherUserImageUrl;
    }
}