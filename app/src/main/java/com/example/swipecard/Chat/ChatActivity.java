
package com.example.swipecard.Chat;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swipecard.R;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageView buttonSend;
    private TextView textTitle;

    private ChatMessageAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private String chatId;
    private String otherUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // 設置未捕獲異常處理器
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
                    Log.e("ChatActivity_CRASH", "未捕獲異常", ex);
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(ChatActivity.this)
                                .setTitle("應用程式錯誤")
                                .setMessage("很抱歉，發生錯誤: " + ex.getMessage())
                                .setPositiveButton("確定", (dialog, which) -> finish())
                                .show();
                    });
                });

        // 從 Intent 獲取數據
        chatId = getIntent().getStringExtra("CHAT_ID");
        otherUserName = getIntent().getStringExtra("OTHER_USER_NAME");
        if (chatId == null) {
            Toast.makeText(this, "聊天室ID錯誤", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化 Firebase
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        buttonSend = findViewById(R.id.button_send);
        buttonSend.setOnClickListener(v -> sendMessage());


        initializeUI();
        setupMessageAdapter(); // 使用新的適配器設置方法
    }

    private void initializeUI() {
        recyclerView = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);

        textTitle = findViewById(R.id.text_title);

        // 設置標題
        if (otherUserName != null) {
            textTitle.setText(otherUserName);
        }

        // 設置 RecyclerView 佈局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 從底部開始顯示
        recyclerView.setLayoutManager(layoutManager);

        // 返回按鈕
        findViewById(R.id.button_back).setOnClickListener(v -> finish());
    }

    private void setupMessageAdapter() {
        // 創建 Firestore 查詢
        Query query = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);

        // 配置 FirestoreRecyclerOptions
        FirestoreRecyclerOptions<ChatMessage> options = new FirestoreRecyclerOptions.Builder<ChatMessage>()
                .setQuery(query, ChatMessage.class)
                .build();

        // 初始化適配器
        adapter = new ChatMessageAdapter(options, currentUserId);
        recyclerView.setAdapter(adapter);

        // 添加自動滾動到底部的邏輯
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                recyclerView.smoothScrollToPosition(adapter.getItemCount());
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening(); // 啟動適配器監聽
            Log.d("ChatActivity", "開始監聽訊息, chatId: " + chatId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening(); // 停止適配器監聽
        }
    }

    private boolean isMessageInAdapter(ChatMessage newMessage) {
        for (ChatMessage message : adapter.getSnapshots()) {
            if (message.getTimestamp().equals(newMessage.getTimestamp())
                    && message.getContent().equals(newMessage.getContent())) {
                return true;
            }
        }
        return false;
    }
    private void addMessageToAdapter(ChatMessage message) {
        // 手動添加到適配器
        adapter.getSnapshots().add(message);
        adapter.notifyItemInserted(adapter.getItemCount() - 1);
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        try {
            // 創建基本訊息數據
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("senderId", currentUserId);
            messageData.put("content", messageText);
            messageData.put("timestamp", FieldValue.serverTimestamp());

            // 直接發送到主要集合
            db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(messageData)
                    .addOnCompleteListener(task -> {
                        runOnUiThread(() -> {
                            if (task.isSuccessful()) {
                                Log.d("ChatActivity", "訊息發送成功");
                                editTextMessage.setText("");
                                updateLastMessage(messageText);
                            } else {
                                Log.e("ChatActivity", "發送失敗", task.getException());
                                Toast.makeText(this, "發送失敗: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        } catch (Exception e) {
            Log.e("ChatActivity", "發送時發生異常", e);
            Toast.makeText(this, "發生錯誤: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLastMessage(String messageText) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", messageText);
        updates.put("lastMessageTime", FieldValue.serverTimestamp());
        updates.put("lastSenderId", currentUserId);

        db.collection("chats")
                .document(chatId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d("ChatActivity", "聊天室最後訊息更新成功"))
                .addOnFailureListener(e -> Log.e("ChatActivity", "更新聊天室最後訊息失敗", e));
    }
}