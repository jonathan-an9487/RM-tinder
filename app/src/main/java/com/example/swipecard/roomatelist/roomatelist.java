package com.example.swipecard.roomatelist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swipecard.Chat.ChatRoom;
import com.example.swipecard.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class roomatelist extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private roomateAdapter adapter;
    private List<ChatRoom> chatRooms;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration chatRoomsListener;
    private TextView debugView;

    public roomatelist() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roomatelistfragment, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        recyclerView = view.findViewById(R.id.recycler_view_roommates);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyView = view.findViewById(R.id.empty_view);
        debugView = view.findViewById(R.id.debug_view);

        if (recyclerView == null) throw new IllegalStateException("RecyclerView not found");
        if (progressBar == null) throw new IllegalStateException("ProgressBar not found");
        if (emptyView == null) throw new IllegalStateException("EmptyView not found");
        chatRooms = new ArrayList<>();
        adapter = new roomateAdapter(chatRooms, this::onChatRoomClick);

        // 設置 RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            Log.d("RoommateList", "目前登入用戶: " + currentUserId);
            loadChatRooms(); // 立即加載聊天室
        } else {
            Log.e("RoommateList", "用戶未登入");
            Toast.makeText(getContext(), "請先登入", Toast.LENGTH_SHORT).show();
        }

        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            Log.d("RoommateList", "目前登入用戶: " + currentUserId);
        } else {
            Log.e("RoommateList", "用戶未登入");
            Toast.makeText(getContext(), "請先登入", Toast.LENGTH_SHORT).show();
            return view;
        }


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("RoommateList", "Fragment onResume - 重新載入聊天室");
        loadChatRooms();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chatRoomsListener != null) {
            chatRoomsListener.remove();
            chatRoomsListener = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (chatRoomsListener != null) {
            chatRoomsListener.remove();
            chatRoomsListener = null;
        }
    }


//    private void initializeUI(View view) {
//        recyclerView = view.findViewById(R.id.recycler_view_roommates);
//        progressBar = view.findViewById(R.id.progress_bar);
//        emptyView = view.findViewById(R.id.empty_view);
//        chatRooms = new ArrayList<>();
//        adapter = new roomateAdapter(chatRooms, this::onChatRoomClick);
//
//        Log.d("RoommateList", "Setting RecyclerView adapter");
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        recyclerView.setAdapter(adapter);
//        Log.d("RoommateList", "Adapter set: " + (recyclerView.getAdapter() != null));
//    }

    private void loadChatRooms() {
        // 顯示進度條，隱藏空狀態
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        Log.d("RoommateList", "開始載入聊天室，用戶ID: " + currentUserId);

        // 添加 Firestore 查詢日誌
        Log.d("FirestoreQuery", "查詢路徑: chats where participants contains " + currentUserId);

        chatRoomsListener = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("RoommateList", "載入失敗: " + error.getMessage(), error);
                        // 顯示錯誤詳情
                        updateDebugInfo("錯誤: " + error.getMessage());
                        return;
                    }

                    if (querySnapshot == null) {
                        Log.w("RoommateList", "查詢快照為 null");
                        updateDebugInfo("查詢快照為 null");
                        return;
                    }

                    Log.d("RoommateList", "找到 " + querySnapshot.size() + "個聊天室");
                            updateDebugInfo("找到 " + querySnapshot.size() + " 個聊天室");

                    // 臨時列表用於批量處理
                    List<ChatRoom> tempList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        try {
                            ChatRoom chatRoom = document.toObject(ChatRoom.class);
                            chatRoom.setChatId(document.getId());
                            tempList.add(chatRoom);
                        } catch (Exception e) {
                            Log.e("RoommateList", "解析聊天室數據失敗", e);
                        }
                    }

                    // 批量加載其他用戶資料
                    loadAllOtherUserData(tempList);
                });
    }

    private void loadAllOtherUserData(List<ChatRoom> tempList) {
        updateDebugInfo("加載用戶資料: " + tempList.size() + " 個聊天室");
        if (tempList.isEmpty()) {
            Log.d("RoommateList", "暫存聊天室列表為空");
            updateDebugInfo("暫存列表為空");
            return;
        }

        // 使用計數器追蹤加載進度
        final int[] counter = {0};
        final int total = tempList.size();

        if (total == 0) {
            updateEmptyView();
            return;
        }


        for (ChatRoom chatRoom : tempList) {
            List<String> participants = chatRoom.getParticipants();
            if (participants == null || participants.size() < 2) {
                Log.w("RoommateList", "聊天室參與者數據異常: " + chatRoom.getChatId());
                counter[0]++;
                continue;
            }

            String otherUserId = participants.get(0).equals(currentUserId) ?
                    participants.get(1) : participants.get(0);

            db.collection("users").document(otherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            chatRoom.setOtherUserName(name != null ? name : "未知用戶");
                            chatRoom.setOtherUserImageUrl(profileImageUrl);
                        } else {
                            chatRoom.setOtherUserName("未知用戶");
                        }

                        // 添加到最終列表
                        chatRooms.add(chatRoom);

                        // 檢查是否所有聊天室都已處理
                        if (++counter[0] >= total) {
                            updateAdapterAndView();
                        }
                    })
                    .addOnFailureListener(e -> {
                        chatRoom.setOtherUserName("未知用戶");
                        chatRooms.add(chatRoom);

                        // 檢查是否所有聊天室都已處理
                        if (++counter[0] >= total) {
                            updateAdapterAndView();
                        }
                    });
        }
    }
    private void updateAdapterAndView() {
        updateDebugInfo("聊天室加載完成: " + chatRooms.size() + " 個聊天室");

        // 按最後訊息時間排序
        Collections.sort(chatRooms, (r1, r2) ->
                r2.getLastMessageTime().compareTo(r1.getLastMessageTime()));

        // 更新適配器
        adapter.notifyDataSetChanged();

        // 更新UI狀態
        updateEmptyView();

        // 顯示RecyclerView
        recyclerView.setVisibility(View.VISIBLE);
        Log.d("RoommateList", "聊天室列表更新完成，共 " + chatRooms.size() + " 個聊天室");
    }
    private void updateEmptyView() {
        if (chatRooms.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }


    private void onChatRoomClick(ChatRoom chatRoom) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), com.example.swipecard.Chat.ChatActivity.class);
            intent.putExtra("CHAT_ID", chatRoom.getChatId());
            intent.putExtra("OTHER_USER_NAME", chatRoom.getOtherUserName());
            startActivity(intent);
        }
    }

    public void refreshChatRooms() {
        Log.d("RoommateList", "手動刷新聊天室列表");
        if (currentUserId != null) {
            loadChatRooms();
        }
    }
    private void updateDebugInfo(String message) {
        if (debugView != null) {
            debugView.setText(message);
            debugView.setVisibility(View.VISIBLE);
        }
    }
}