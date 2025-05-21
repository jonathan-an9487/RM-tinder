package com.example.swipecard;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.swipecard.newloginregist.newloginActivity;
import com.example.swipecard.profile.ProfileSetupActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CardStackListener {

    private List<User> users;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private FirebaseFirestore db;
    private String currentUserId;
    private CardStackLayoutManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int resultCode = api.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            api.makeGooglePlayServicesAvailable(this)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            loadRealUsersFromFirebase();
                        }
                    });
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 檢查用戶是否登錄
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            redirectToLogin();
            return;
        }

        currentUserId = firebaseUser.getUid();
        db = FirebaseFirestore.getInstance();

        initializeUI();
        checkUserProfile();
    }

    private void redirectToLogin() {
        startActivity(new Intent(this, newloginActivity.class));
        finish();
    }

    private void checkUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        startActivity(new Intent(this, ProfileSetupActivity.class));
                        finish();
                    } else {
                        loadRealUsersFromFirebase();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "檢查用戶資料失敗", Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "檢查用戶資料失敗", e);
                });
    }

    private void initializeUI() {
        cardStackView = findViewById(R.id.card_stack_view);
        users = new ArrayList<>();
        adapter = new CardStackAdapter(users);
        cardStackView.setAdapter(adapter);

        manager = new CardStackLayoutManager(this, this);
        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setDirections(Direction.HORIZONTAL);
        cardStackView.setLayoutManager(manager);
    }

    private void loadRealUsersFromFirebase() {
        db.collection("users")
                .whereNotEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> userList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        Log.d("FirestoreData", "用户数据: " +
                                "名字=" + user.getName() +
                                ", 简介=" + user.getBio());
                        userList.add(user);
                    }
                    adapter.updateUsers(userList);
                });
    }


    @Override
    public void onCardSwiped(Direction direction) {
        int position = manager.getTopPosition() - 1;
        if (position < 0 || position >= users.size()) return;

        User swipedUser = users.get(position);

        // 验证用户数据
        if(swipedUser.getUserId() == null) {
            Log.e("SwipeError", "用户ID为空: " + swipedUser.getName());
            return;
        }

        boolean isLike = direction == Direction.Right;
        saveSwipeToFirestore(swipedUser.getUserId(), isLike);
    }

    private void saveSwipeToFirestore(String targetUserId, boolean isLike) {
        // 1. 验证目标用户ID
        if(targetUserId == null || targetUserId.isEmpty()) {
            Log.e("SwipeError", "目标用户ID无效");
            return;
        }

        // 2. 创建滑动数据
        Map<String, Object> swipeData = new HashMap<>();
        swipeData.put("sourceUserId", currentUserId);
        swipeData.put("targetUserId", targetUserId);
        swipeData.put("isLike", isLike);
        swipeData.put("timestamp", FieldValue.serverTimestamp());

        // 3. 使用事务确保数据一致性
        db.runTransaction(transaction -> {
            DocumentReference swipeRef = db.collection("swipes")
                    .document(currentUserId + "_" + targetUserId);
            transaction.set(swipeRef, swipeData);
            return null;
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if(isLike) checkForMatch(targetUserId);
            } else {
                Log.e("Firestore", "保存滑动失败", task.getException());
                Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
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
                    if (!querySnapshot.isEmpty()) {
                        showMatchDialog(targetUserId);
                    }
                });
    }

    private void showMatchDialog(String matchedUserId) {
        db.collection("users").document(matchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User matchedUser = documentSnapshot.toObject(User.class);
                    if (matchedUser != null) {
                        new AlertDialog.Builder(this)
                                .setTitle("配對成功！")
                                .setMessage("你和 " + matchedUser.getName() + " 互相喜歡！")
                                .setPositiveButton("聊天", (dialog, which) -> {
                                    // 跳轉到聊天界面
                                })
                                .setNegativeButton("關閉", null)
                                .show();
                    }
                });
    }

    // 其他 CardStackListener 方法保持不變
    @Override public void onCardDragging(@NonNull Direction direction, float ratio) {}
    @Override public void onCardRewound() {}
    @Override public void onCardCanceled() {}
    @Override public void onCardAppeared(@NonNull View view, int position) {}
    @Override public void onCardDisappeared(@NonNull View view, int position) {}
}