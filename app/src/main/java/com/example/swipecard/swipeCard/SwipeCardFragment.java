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
import com.example.swipecard.R;
import com.example.swipecard.User;
import com.example.swipecard.newloginregist.newloginActivity;
import com.example.swipecard.profile.ProfileSetupActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.Objects;


public class SwipeCardFragment extends Fragment implements CardStackListener{
    private List<User> users;
    private CardStackAdapter adapter;
    private CardStackView cardStackView;
    private FirebaseFirestore db;
    private String currentUserId;
    private CardStackLayoutManager manager;

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
            redirectToLogin();
            return;
        }
        currentUserId = firebaseUser.getUid();
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
        Log.d("USER_LOAD", "开始加载用户数据，当前用户ID: " + currentUserId);

        db.collection("users")
                .whereNotEqualTo("userId", currentUserId)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("USER_LOAD", "查询成功，文档数量: " + querySnapshot.size());

                    users.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Log.d("USER_LOAD", "文档ID: " + document.getId());
                        Log.d("USER_LOAD", "文档数据: " + document.getData());

                        try {
                            User user = document.toObject(User.class);
                            user.setUserId(document.getId());
                            users.add(user);
                            Log.d("USER_LOAD", "成功添加用户: " + user.getName());
                        } catch (Exception e) {
                            Log.e("USER_LOAD", "转换用户对象失败", e);
                        }
                    }

                    if (users.isEmpty()) {
                        Log.d("USER_LOAD", "用户列表为空");
                    } else {
                        Log.d("USER_LOAD", "加载了 " + users.size() + " 个用户");
                    }

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.e("USER_LOAD", "适配器为null");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("USER_LOAD", "加载用户失败", e);
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

        Log.d("SWIPE_DEBUG", "正在儲存滑動資料，目標用戶ID: " + swipedUser.getUserId());
        saveSwipeToFirestore(swipedUser.getUserId(), direction == Direction.Right);
    }

    private void saveSwipeToFirestore(String targetUserId, boolean isLike) {
        Log.d("SWIPE_DEBUG", "currentUserId: " + currentUserId + ", targetUserId: " + targetUserId);

        if (targetUserId == null || targetUserId.isEmpty() || currentUserId == null) {
            Log.e("SWIPE_DEBUG", "用戶ID無效: currentUserId=" + currentUserId + ", targetUserId=" + targetUserId);
            return;
        }

        Map<String, Object> swipeData = new HashMap<>();
        swipeData.put("sourceUserId", currentUserId);
        swipeData.put("targetUserId", targetUserId);
        swipeData.put("isLike", isLike);
        swipeData.put("timestamp", FieldValue.serverTimestamp());

        Log.d("SWIPE_DEBUG", "準備寫入 Firestore，資料: " + swipeData);

        db.collection("swipes")
                .document(currentUserId + "_" + targetUserId)
                .set(swipeData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("SWIPE_DEBUG", "✅ 伺服器確認寫入成功");
                    } else {
                        Log.e("SWIPE_DEBUG", "❌ 伺服器拒絕寫入", task.getException());
                        Toast.makeText(getContext(), "寫入失敗: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
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
                        showMatchDialog(targetUserId);
                    }
                });
    }

    private void showMatchDialog(String matchedUserId) {
        db.collection("users").document(matchedUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (isAdded()) {
                        User matchedUser = documentSnapshot.toObject(User.class);
                        if (matchedUser != null) {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("配對成功！")
                                    .setMessage("你和 " + matchedUser.getName() + " 互相喜歡！")
                                    .setPositiveButton("聊天", (dialog, which) -> {
                                        // TODO: Implement chat navigation
                                    })
                                    .setNegativeButton("關閉", null)
                                    .show();
                        }
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