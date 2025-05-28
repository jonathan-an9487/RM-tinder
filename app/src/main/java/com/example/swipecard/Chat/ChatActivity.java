package com.example.swipecard.Chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swipecard.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageView buttonSend;
    private TextView textTitle;

    private FirestoreRecyclerAdapter<ChatMessage, MessageViewHolder> adapter; // 修改适配器类型
    private FirebaseFirestore db;
    private String currentUserId;
    private String chatId;
    private String otherUserName;
    private boolean isAdapterSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> {
            Log.e(TAG, "未捕獲異常", ex);
            runOnUiThread(() -> showErrorDialog(ex));
        });

        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Intent is null");
            showErrorDialog("啟動參數錯誤");
            return;
        }
        chatId = intent.getStringExtra("CHAT_ID");
        otherUserName = intent.getStringExtra("OTHER_USER_NAME");

        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "Invalid chatId: " + chatId);
            showErrorDialog("聊天室ID錯誤");
            return;
        }

        initializeFirebase();
        initializeUI();
        verifyChatRoomExistence();
    }

    private void initializeUI() {
        recyclerView = findViewById(R.id.recycler_view_messages);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        textTitle = findViewById(R.id.text_title);

        if (otherUserName != null) {
            textTitle.setText(otherUserName);
        }

        // 设置布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 从底部开始显示
        recyclerView.setLayoutManager(layoutManager);

        findViewById(R.id.button_back).setOnClickListener(v -> finish());

        // 设置发送按钮点击事件
        buttonSend.setOnClickListener(v -> sendMessage());
    }

    private void initializeFirebase() {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "用戶未登入");
                Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            currentUserId = currentUser.getUid();
            db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase initialized. User: " + currentUserId);
        } catch (Exception e) {
            Log.e(TAG, "Firebase初始化失敗", e);
            showErrorDialog("初始化失敗");
        }
    }

    private void verifyChatRoomExistence() {
        Log.d(TAG, "驗證聊天室存在: " + chatId);

        db.collection("chats").document(chatId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Log.d(TAG, "聊天室存在，設置適配器");
                            setupMessageAdapter();
                        } else {
                            Log.e(TAG, "聊天室不存在: " + chatId);
                            showErrorDialog("聊天室不存在");
                        }
                    } else {
                        Log.e(TAG, "驗證失敗: " + task.getException());
                        showErrorDialog("驗證聊天室失敗");
                    }
                });
    }

    // 自定义 ViewHolder
    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, senderText, timeText;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            senderText = itemView.findViewById(R.id.sender_text);
            timeText = itemView.findViewById(R.id.time_text);
        }
    }

    private void setupMessageAdapter() {
        try {
            Log.d(TAG, "設置訊息適配器，chatId: " + chatId);

            CollectionReference messagesRef = db.collection("chats")
                    .document(chatId)
                    .collection("messages");

            // 升序排序，新消息在底部
            Query query = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING);

            FirestoreRecyclerOptions<ChatMessage> options =
                    new FirestoreRecyclerOptions.Builder<ChatMessage>()
                            .setQuery(query, ChatMessage.class)
                            .build();

            // 创建适配器
            adapter = new FirestoreRecyclerAdapter<ChatMessage, MessageViewHolder>(options) {
                @Override
                protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull ChatMessage model) {
                    // 绑定数据到视图
                    holder.messageText.setText(model.getContent());
                    holder.senderText.setText(model.getSenderId().equals(currentUserId) ? "我" : otherUserName);

                    // 格式化时间戳
                    if (model.getTimestamp() != null) {
                        Date date = model.getTimestamp().toDate();
                        String time = android.text.format.DateFormat.format("HH:mm", date).toString();
                        holder.timeText.setText(time);
                    }

                    // 当有新消息时滚动到底部
                    recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));
                }

                @NonNull
                @Override
                public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                    // 创建新视图
                    View view = getLayoutInflater().inflate(R.layout.item_message, parent, false);
                    return new MessageViewHolder(view);
                }

                @Override
                public void onDataChanged() {
                    super.onDataChanged();
                    // 数据变化时滚动到底部
                    if (getItemCount() > 0) {
                        recyclerView.post(() -> recyclerView.smoothScrollToPosition(getItemCount() - 1));
                    }
                }
            };

            recyclerView.setAdapter(adapter);
            isAdapterSet = true;
            adapter.startListening(); // 立即开始监听

        } catch (Exception e) {
            Log.e(TAG, "設置適配器失敗", e);
            showErrorDialog("初始化聊天室失敗");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening(); // 启动适配器监听
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening(); // 停止适配器监听
        }
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("錯誤")
                .setMessage(message)
                .setPositiveButton("確定", (d, w) -> finish())
                .show();
    }

    private void showErrorDialog(Throwable ex) {
        showErrorDialog("發生錯誤: " + ex.getMessage());
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;

        if (!isAdapterSet) {
            Toast.makeText(this, "聊天室初始化中，請稍後...", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonSend.setEnabled(false);

        // 创建消息对象
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("content", messageText);
        message.put("timestamp", FieldValue.serverTimestamp()); // 使用服务器时间戳

        // 发送到Firestore
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnCompleteListener(task -> {
                    buttonSend.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "訊息發送成功");
                        editTextMessage.setText("");
                        updateLastMessage(messageText);
                    } else {
                        Log.e(TAG, "發送失敗", task.getException());
                        Toast.makeText(ChatActivity.this, "發送失敗: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateLastMessage(String messageText) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", messageText);
        updates.put("lastMessageTime", FieldValue.serverTimestamp());
        updates.put("lastSenderId", currentUserId);

        db.collection("chats")
                .document(chatId)
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "聊天室最後訊息更新成功"))
                .addOnFailureListener(e -> Log.e(TAG, "更新聊天室最後訊息失敗", e));
    }
}