package com.example.swipecard.swipeCard;

import static androidx.browser.customtabs.CustomTabsClient.getPackageName;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.swipecard.CardStackAdapter;
import com.example.swipecard.R;
import com.example.swipecard.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
import java.util.Objects;


public class SwipeCardFragment extends Fragment implements CardStackListener{
    public SwipeCardFragment() {
        // Required empty public constructor
    }
    List<User> users;
    CardStackAdapter adapter;
    CardStackView cardStackView;
    FirebaseFirestore db;
    String currentUserId; // 從Firebase Auth獲取真實用戶ID

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipe_card, container, false);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        cardStackView = view.findViewById(R.id.card_stack_view1);

        initializeUI();
        return view;
    }

    private void initializeUI() {
        // 初始化數據
        users = new ArrayList<>();
        users.add(new User("張三", "喜歡爬山和攝影", "https://kmweb.moa.gov.tw/files/IMITA_Gallery/13/b1a898ccbb_m.jpg"));
        users.add(new User("李四", "工程師，愛寫程式", "https://c.files.bbci.co.uk/03F9/production/_93871010_96d3c9bd-2068-4643-bc4f-81c1ad795343.jpg"));
        users.add(new User("淺草寺", "東京", "https://en.pimg.jp/115/846/989/1/115846989.jpg"));
        users.add(new User("曹哲維", "大猛男", "android.resource://" + requireActivity().getPackageName() + "/" + R.drawable.usertesthead));

        adapter = new CardStackAdapter(users);
        cardStackView.setAdapter(adapter);

        // 設定 CardStackView 的 LayoutManager
        CardStackLayoutManager manager = new CardStackLayoutManager(requireContext(), (CardStackListener) this);
        cardStackView.setLayoutManager(manager);

        // 設定卡片堆疊行為
        manager.setStackFrom(StackFrom.Top);
        manager.setVisibleCount(3);
        manager.setDirections(Direction.HORIZONTAL);
    }
    public void onCardDragging(@NonNull Direction direction, float ratio) {
        Log.d("CardStack", "正在拖拽: " + direction + ", 比例: " + ratio);
    }
    public void onCardSwiped(Direction direction) {
        int position = ((CardStackLayoutManager) Objects.requireNonNull(cardStackView.getLayoutManager())).getTopPosition() - 1;
        if (position < 0 || position >= users.size()) return;

        User swipedUser = users.get(position);

        if (direction == Direction.Right) {
            // ▼▼▼ 替換原本的本地存儲 ▼▼▼
            saveSwipeToFirestore(swipedUser.getUserId(), true); // true表示喜歡
        } else {
            saveSwipeToFirestore(swipedUser.getUserId(), false); // false表示不喜歡
        }
    }
    void saveSwipeToFirestore(String targetUserId, boolean isLike) {
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
    void checkForMatch(String targetUserId) {
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
    void createMatch(String matchedUserId) {
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
    void showMatchDialog(User matchedUser) {
        if(!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("配對成功！")
                    .setMessage("你和 " + matchedUser.getName() + " 互相喜歡！")
                    .setPositiveButton("聊天", (dialog, which) -> {
                        // 跳轉到聊天界面
                    })
                    .setNegativeButton("關閉", null)
                    .show();
        });
    }

    public void onCardRewound() {
        Log.d("CardStack", "卡片回退");
    }

    public void onCardCanceled() {
        Log.d("CardStack", "取消滑動");
    }

    public void onCardAppeared(@NonNull View view, int position) {
        User user = users.get(position);
        Log.d("CardStack", "顯示卡片: " + user.getName());
    }

    public void onCardDisappeared(@NonNull View view, int position) {
        User user = users.get(position);
        Log.d("CardStack", "消失卡片: " + user.getName());
    }


}