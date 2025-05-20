package com.example.swipecard;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.swipecard.profile.ProfileSetupActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.yuyakaido.android.cardstackview.CardStackLayoutManager;
import com.yuyakaido.android.cardstackview.CardStackListener;
import com.yuyakaido.android.cardstackview.CardStackView;
import com.yuyakaido.android.cardstackview.Direction;
import com.yuyakaido.android.cardstackview.StackFrom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements CardStackListener {

    private List<User> users;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private FirebaseFirestore db;

    private String currentUserId; // 從Firebase Auth獲取真實用戶ID


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 Firebase 相關對象
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 檢查用戶資料
        checkUserProfile();
    }

    private void checkUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        // 如果用戶資料不存在，跳轉到設置頁面
                        startActivity(new Intent(this, ProfileSetupActivity.class));
                        finish();
                    } else {
                        // 原有初始化代碼
                        initializeUI();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "檢查用戶資料失敗: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void initializeUI() {
        // 這裡放原有的UI初始化代碼
        cardStackView = findViewById(R.id.card_stack_view);

        // 初始化數據
        users = new ArrayList<>();
        users.add(new User("張三", "喜歡爬山和攝影", "https://kmweb.moa.gov.tw/files/IMITA_Gallery/13/b1a898ccbb_m.jpg"));
        users.add(new User("李四", "工程師，愛寫程式", "https://c.files.bbci.co.uk/03F9/production/_93871010_96d3c9bd-2068-4643-bc4f-81c1ad795343.jpg"));
        users.add(new User("淺草寺", "東京", "https://en.pimg.jp/115/846/989/1/115846989.jpg"));
        users.add(new User("曹哲維", "大猛男", "android.resource://" + getPackageName() + "/" + R.drawable.usertesthead));

        adapter = new CardStackAdapter(users);
        cardStackView.setAdapter(adapter);

        // 設定 CardStackView 的 LayoutManager
        CardStackLayoutManager manager = new CardStackLayoutManager(this,this);
        cardStackView.setLayoutManager(manager);

        // 設定卡片堆疊行為
        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setDirections(Direction.HORIZONTAL);
    }

    private void loadRealUsersFromFirebase() {
        db.collection("users")
                .whereNotEqualTo("userId", currentUserId) // 排除自己
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    users = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User user = doc.toObject(User.class);
                        users.add(user);
                    }
                    adapter = new CardStackAdapter(users);
                    cardStackView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "加載用戶失敗", Toast.LENGTH_SHORT).show();
                });
    }

    // 以下是 CardStackListener 的實現方法
    @Override
    public void onCardDragging(@NonNull Direction direction, float ratio) {
        Log.d("CardStack", "正在拖拽: " + direction + ", 比例: " + ratio);
    }

    @Override
    public void onCardSwiped(Direction direction) {
        int position = ((CardStackLayoutManager)cardStackView.getLayoutManager()).getTopPosition() - 1;
        if (position < 0 || position >= users.size()) return;

        User swipedUser = users.get(position);

        if (direction == Direction.Right) {
            // ▼▼▼ 替換原本的本地存儲 ▼▼▼
            saveSwipeToFirestore(swipedUser.getUserId(), true); // true表示喜歡
        } else {
            saveSwipeToFirestore(swipedUser.getUserId(), false); // false表示不喜歡
        }
    }

    private void saveSwipeToFirestore(String targetUserId, boolean isLike) {
        Map<String, Object> swipeData = new HashMap<>();
        swipeData.put("sourceUserId", currentUserId);
        swipeData.put("targetUserId", targetUserId);
        swipeData.put("isLike", isLike);
        swipeData.put("timestamp", FieldValue.serverTimestamp());

        // 寫入到Firestore的swipes集合
        db.collection("swipes")
                .document(currentUserId + "_" + targetUserId) // 用組合ID作為文檔ID
                .set(swipeData)
                .addOnSuccessListener(aVoid -> {
                    if (isLike) checkForMatch(targetUserId); // 只有喜歡才檢查配對
                });
    }
    private void checkForMatch(String targetUserId) {
        // 檢查對方是否也喜歡自己
        db.collection("swipes")
                .whereEqualTo("sourceUserId", targetUserId)
                .whereEqualTo("targetUserId", currentUserId)
                .whereEqualTo("isLike", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        createMatch(targetUserId); // 創建配對記錄
                    }
                });
    }

    private void createMatch(String matchedUserId) {
        // 1. 獲取對方用戶資料
        db.collection("users").document(matchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User matchedUser = documentSnapshot.toObject(User.class);

                    // 2. 顯示配對成功UI
                    showMatchDialog(matchedUser);

                    // 3. 寫入配對記錄 (可選)
                    Map<String, Object> matchData = new HashMap<>();
                    matchData.put("users", Arrays.asList(currentUserId, matchedUserId));
                    matchData.put("timestamp", FieldValue.serverTimestamp());

                    db.collection("matches")
                            .document(currentUserId + "_" + matchedUserId)
                            .set(matchData);
                });
    }
    private void showMatchDialog(User matchedUser) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("配對成功！")
                    .setMessage("你和 " + matchedUser.getName() + " 互相喜歡！")
                    .setPositiveButton("聊天", (dialog, which) -> {
                        // 跳轉到聊天界面
                    })
                    .setNegativeButton("關閉", null)
                    .show();
        });
    }
    @Override
    public void onCardRewound() {
        Log.d("CardStack", "卡片回退");
    }

    @Override
    public void onCardCanceled() {
        Log.d("CardStack", "取消滑動");
    }

    @Override
    public void onCardAppeared(@NonNull View view, int position) {
        User user = users.get(position);
        Log.d("CardStack", "顯示卡片: " + user.getName());
    }

    @Override
    public void onCardDisappeared(@NonNull View view, int position) {
        User user = users.get(position);
        Log.d("CardStack", "消失卡片: " + user.getName());
    }


}