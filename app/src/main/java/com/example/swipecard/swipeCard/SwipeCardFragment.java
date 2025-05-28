package com.example.swipecard.swipeCard;

import static androidx.browser.customtabs.CustomTabsClient.getPackageName;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.swipecard.CardStackAdapter;
import com.example.swipecard.Chat.ChatActivity;
import com.example.swipecard.R;
import com.example.swipecard.User;
import com.example.swipecard.newloginregist.newloginActivity;
import com.example.swipecard.profile.ProfileSetupActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class SwipeCardFragment extends Fragment implements CardStackListener{
    private List<User> users;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private FirebaseFirestore db;
    private String currentUserId;
    private CardStackLayoutManager manager;
    private Set<String> swipedUserIds = new HashSet<>();

    public SwipeCardFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_swipe_card, container, false);

        // Initialize Firebase and UI components
        initializeFirebase();
        initializeUI(view);

        // Check Google Play Services availability
        checkGooglePlayServices();

        return view;
    }

    private void initializeFirebase() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            Log.e("SwipeCardFragment", "用戶未登入");
            redirectToLogin();
            return;
        }
        currentUserId = firebaseUser.getUid();
        Log.d("SwipeCardFragment", "當前用戶ID: " + currentUserId);
        db = FirebaseFirestore.getInstance();
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int resultCode = api.isGooglePlayServicesAvailable(requireActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            api.makeGooglePlayServicesAvailable(requireActivity())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && isAdded()) {
                            checkUserProfile();
                        }
                    });
        } else {
            checkUserProfile();
        }
    }

    private void initializeUI(View view) {
        cardStackView = view.findViewById(R.id.card_stack_view); // 確保這個ID與佈局文件一致
        if (cardStackView == null) {
            throw new IllegalStateException("CardStackView not found in layout");
        }

        users = new ArrayList<>();
        adapter = new CardStackAdapter(users);

        manager = new CardStackLayoutManager(requireContext(), this);
        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setDirections(Direction.HORIZONTAL);

        cardStackView.setLayoutManager(manager);
        cardStackView.setAdapter(adapter);
    }

    private void redirectToLogin() {
        startActivity(new Intent(requireActivity(), newloginActivity.class));
        requireActivity().finish();
    }

    private void checkUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        startActivity(new Intent(requireActivity(), ProfileSetupActivity.class));
                        requireActivity().finish();
                    } else {
                        loadRealUsersFromFirebase();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "檢查用戶資料失敗", Toast.LENGTH_SHORT).show();
                        Log.e("MainFragment", "檢查用戶資料失敗", e);
                    }
                });
    }

    private void loadRealUsersFromFirebase() {
        db.collection("swipes")
                .whereEqualTo("sourceUserId", currentUserId)
                .get()
                .addOnSuccessListener(swipeSnapshots -> {
                    Set<String> swipedIds = new HashSet<>();
                    for (DocumentSnapshot doc : swipeSnapshots) {
                        swipedIds.add((String) doc.get("targetUserId"));
                    }
                    loadCandidatesExcluding(swipedIds);
                });
    }
    private void loadCandidatesExcluding(Set<String> excludeIds) {
        db.collection("users")
                .whereNotEqualTo("userId", currentUserId)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    users.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String userId = document.getId();
                        if (excludeIds.contains(userId)) continue;
                        try {
                            User user = document.toObject(User.class);
                            user.setUserId(userId);
                            users.add(user);
                        } catch (Exception e) {
                            Log.e("USER_LOAD", "轉換失敗", e);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
    @Override
    public void onCardSwiped(Direction direction) {

        Log.d("SWIPE_DEBUG", "卡片已滑動，方向: " + direction);
        int position = manager.getTopPosition() - 1;
        if (position < 0 || position >= users.size()) {
            Log.e("SWIPE_DEBUG", "無效的位置: " + position);
            return;
        }

        User swipedUser = users.get(position);
        if (swipedUser.getUserId() == null) {
            Log.e("SWIPE_DEBUG", "用戶ID為空: " + swipedUser.getName());
            return;
        }
        swipedUserIds.add(swipedUser.getUserId());

        Log.d("SWIPE_DEBUG", "正在儲存滑動資料，目標用戶ID: " + swipedUser.getUserId());
        saveSwipeToFirestore(swipedUser.getUserId(), direction == Direction.Right);
    }

    private void saveSwipeToFirestore(String targetUserId, boolean isLike) {
        // 1. 添加 swipeData 的定义
        Map<String, Object> swipeData = new HashMap<>();
        swipeData.put("sourceUserId", currentUserId);
        swipeData.put("targetUserId", targetUserId);
        swipeData.put("isLike", isLike);
        swipeData.put("timestamp", FieldValue.serverTimestamp());

        // 2. 确保变量名正确（注意大小写）
        db.collection("swipes")
                .document(currentUserId + "_" + targetUserId)
                .set(swipeData) // 使用已定义的变量
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("SWIPE_DEBUG", "✅ 伺服器確認寫入成功");
                        updateLikedUsers(targetUserId, isLike);
                    } else {
                        Log.e("SWIPE_DEBUG", "❌ 伺服器拒絕寫入", task.getException());
                        if (isAdded()) {
                            Toast.makeText(getContext(), "寫入失敗: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
    private void updateLikedUsers(String targetUserId, boolean isLike) {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("likedUsers." + targetUserId, isLike);

        db.collection("users").document(currentUserId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    // 檢查是否互相喜歡
                    checkMutualLike(targetUserId);
                });
    }
    private void checkMutualLike(String targetUserId) {
        db.collection("users").document(targetUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Boolean> theirLikedUsers = (Map<String, Boolean>) documentSnapshot.get("likedUsers");
                        if (theirLikedUsers != null && theirLikedUsers.containsKey(currentUserId) && theirLikedUsers.get(currentUserId)) {
                            // 互相喜歡，創建配對
                            createMatch(currentUserId, targetUserId);
                        }
                    }
                }).addOnFailureListener(e -> { // 新增错误处理
                    Log.e("Match", "检查互赞失败", e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "网络错误，请检查连接", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void createMatch(String user1Id, String user2Id) {
        final String chatId = db.collection("chats").document().getId();
        Log.d("SwipeCardFragment", "創建配對，user1Id: " + user1Id + ", user2Id: " + user2Id + ", currentUserId: " + currentUserId);

        if (currentUserId == null) {
            Log.e("SwipeCardFragment", "currentUserId 為空，無法創建配對");
            return;
        }

        if (!user1Id.equals(currentUserId) && !user2Id.equals(currentUserId)) {
            Log.e("SwipeCardFragment", "當前用戶不在配對中，currentUserId: " + currentUserId);
            return;
        }

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("users", Arrays.asList(user1Id, user2Id));
        matchData.put("chatId", chatId);
        matchData.put("timestamp", FieldValue.serverTimestamp());
        Log.d("SwipeCardFragment", "matchData: " + matchData.toString());

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("participants", Arrays.asList(user1Id, user2Id));
        chatData.put("lastMessageTime", FieldValue.serverTimestamp());
        chatData.put("lastMessage", "");
        chatData.put("lastSenderId", user1Id);
        Log.d("SwipeCardFragment", "chatData: " + chatData.toString());

        WriteBatch batch = db.batch();
        DocumentReference matchRef = db.collection("matches").document();
        DocumentReference chatRef = db.collection("chats").document(chatId);

        batch.set(matchRef, matchData);
        batch.set(chatRef, chatData);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("SwipeCardFragment", "配對成功，chatId: " + chatId);
                    showMatchDialog(user2Id, chatId);
                })
                .addOnFailureListener(e -> {
                    Log.e("SwipeCardFragment", "配對失敗: " + e.getMessage(), e);
                    if (isAdded()) {
                        Toast.makeText(getContext(), "配對失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void checkForMatch(String targetUserId) {
        db.collection("swipes")
                .whereEqualTo("sourceUserId", targetUserId)
                .whereEqualTo("targetUserId", currentUserId)
                .whereEqualTo("isLike", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (isAdded() && !querySnapshot.isEmpty()) {
                        createMatch(currentUserId, targetUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SwipeCardFragment", "Failed to check for match", e);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "檢查配對失敗", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void createChatRoom(String matchedUserId) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        List<String> participants = Arrays.asList(currentUserId, matchedUserId);

        // Check if a chat room already exists to avoid duplicates
        db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .whereArrayContains("participants", matchedUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Create a new chat room
                        Map<String, Object> chatRoomData = new HashMap<>();
                        chatRoomData.put("participants", participants);
                        chatRoomData.put("lastMessageTime", FieldValue.serverTimestamp());
                        chatRoomData.put("lastMessage", "");

                        db.collection("chats")
                                .add(chatRoomData)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d("SwipeCardFragment", "Chat room created with ID: " + documentReference.getId());
                                    // Show dialog with the new chatId
                                    showMatchDialog(matchedUserId, documentReference.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SwipeCardFragment", "Failed to create chat room", e);
                                    if (isAdded()) {
                                        Toast.makeText(requireContext(), "無法創建聊天室", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Use existing chat room
                        String chatId = querySnapshot.getDocuments().get(0).getId();
                        showMatchDialog(matchedUserId, chatId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SwipeCardFragment", "Failed to check for existing chat room", e);
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "檢查聊天室失敗", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void showMatchDialog(String matchedUserId, String chatId) {
        db.collection("users").document(matchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User matchedUser = documentSnapshot.toObject(User.class);
                    if (matchedUser != null && isAdded()) {
                        new AlertDialog.Builder(requireContext())
                                .setTitle("配對成功！")
                                .setMessage("你和 " + matchedUser.getName() + " 已配對")
                                .setPositiveButton("開始聊天", (dialog, which) -> {
                                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                                    intent.putExtra("CHAT_ID", chatId);
                                    startActivity(intent);
                                })
                                .setNegativeButton("關閉", null)
                                .show();
                    }
                });
    }

    // Other CardStackListener methods
    @Override public void onCardDragging(@NonNull Direction direction, float ratio) {}
    @Override public void onCardRewound() {}
    @Override public void onCardCanceled() {}
    @Override public void onCardAppeared(@NonNull View view, int position) {}
    @Override public void onCardDisappeared(@NonNull View view, int position) {}

}