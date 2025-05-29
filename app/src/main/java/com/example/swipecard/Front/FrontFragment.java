package com.example.swipecard.Front;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.swipecard.Chat.ChatActivity;
import com.example.swipecard.Chat.ChatRoom;
import com.example.swipecard.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class FrontFragment extends Fragment {

    private static final String TAG = "FrontFragment";

    private RecyclerView recyclerViewChatRooms;
    private ChatRoomAdapter chatRoomAdapter;
    private List<ChatRoom> chatRooms;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private String currentUserId;

    public FrontFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roomatelistfragment, container, false);

        initializeFirebase();
        initializeUI(view);
        loadChatRooms();

        return view;
    }

    private void initializeFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "用户未登录");
            return;
        }
        currentUserId = currentUser.getUid();
        db = FirebaseFirestore.getInstance();
    }

    private void initializeUI(View view) {
        recyclerViewChatRooms = view.findViewById(R.id.recycler_view_roommates);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        emptyView = view.findViewById(R.id.empty_view);
        progressBar = view.findViewById(R.id.progress_bar);

        // 初始化聊天室列表
        chatRooms = new ArrayList<>();
        chatRoomAdapter = new ChatRoomAdapter(chatRooms, this::onChatRoomClick);

        recyclerViewChatRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewChatRooms.setAdapter(chatRoomAdapter);

        // 设置下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::refreshChatRooms);
    }

    private void loadChatRooms() {
        if (currentUserId == null) return;

        showLoading(true);

        // 查询当前用户参与的所有聊天室（暫時移除排序避免索引問題）
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    chatRooms.clear();

                    if (querySnapshot.isEmpty()) {
                        showEmptyView(true);
                        showLoading(false);
                        return;
                    }

                    List<ChatRoom> tempChatRooms = new ArrayList<>();

                    // 处理每个聊天室
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        ChatRoom chatRoom = document.toObject(ChatRoom.class);
                        if (chatRoom != null) {
                            chatRoom.setChatId(document.getId());
                            tempChatRooms.add(chatRoom);
                        }
                    }

                    // 在客戶端按 lastMessageTime 排序
                    tempChatRooms.sort((a, b) -> {
                        if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                        if (a.getLastMessageTime() == null) return 1;
                        if (b.getLastMessageTime() == null) return -1;
                        return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                    });

                    chatRooms.addAll(tempChatRooms);

                    // 載入每個聊天室的對方用戶信息
                    for (ChatRoom chatRoom : chatRooms) {
                        String otherUserId = getOtherUserId(chatRoom.getParticipants());
                        if (otherUserId != null) {
                            loadOtherUserInfo(chatRoom, otherUserId);
                        }
                    }

                    showLoading(false);
                    showEmptyView(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "加载聊天室失败", e);
                    showLoading(false);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "加载聊天室失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getOtherUserId(List<String> participants) {
        if (participants == null || participants.size() != 2) return null;

        for (String userId : participants) {
            if (!userId.equals(currentUserId)) {
                return userId;
            }
        }
        return null;
    }

    private void loadOtherUserInfo(ChatRoom chatRoom, String otherUserId) {
        db.collection("users").document(otherUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        String name = documentSnapshot.getString("name");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        chatRoom.setOtherUserName(name != null ? name : "Unknown User");
                        chatRoom.setOtherUserImageUrl(profileImageUrl);

                        chatRoomAdapter.notifyDataSetChanged();
                        Log.d(TAG, "加载用户信息成功: " + name);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "加载用户信息失败", e);
                    chatRoom.setOtherUserName("Unknown User");
                    chatRoomAdapter.notifyDataSetChanged();
                });
    }

    private void onChatRoomClick(ChatRoom chatRoom) {
        Log.d(TAG, "点击聊天室: " + chatRoom.getChatId() + ", 对方: " + chatRoom.getOtherUserName());

        Intent intent = new Intent(getActivity(), ChatActivity.class);
        intent.putExtra("CHAT_ID", chatRoom.getChatId());
        intent.putExtra("OTHER_USER_NAME", chatRoom.getOtherUserName());
        startActivity(intent);
    }

    // 合併後的刷新方法 - 既可以被下拉刷新調用，也可以被外部調用
    public void refreshChatRooms() {
        if (isAdded()) {
            loadChatRooms();
            // 如果是通過下拉刷新觸發的，停止刷新動畫
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyView(boolean show) {
        if (emptyView != null && recyclerViewChatRooms != null) {
            emptyView.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerViewChatRooms.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次页面显示时刷新聊天室列表
        refreshChatRooms();
    }
}