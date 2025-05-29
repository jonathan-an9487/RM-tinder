package com.example.swipecard.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.swipecard.R;
import com.example.swipecard.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private ImageView userpicture;
    private TextView Username, bio;
    private Button mprofilesetup;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    public ProfileFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // 初始化 Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 获取当前用户
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        } else {
            Toast.makeText(getContext(), "用户未登录", Toast.LENGTH_SHORT).show();
            return view;
        }

        // 初始化 UI 组件
        initializeViews(view);

        // 加载用户资料
        loadUserProfile();

        return view;
    }

    private void initializeViews(View view) {
        userpicture = view.findViewById(R.id.userpicture);
        Username = view.findViewById(R.id.Username);
        bio = view.findViewById(R.id.bio);
        mprofilesetup = view.findViewById(R.id.toprofilesetup);

        mprofilesetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ProfileSetupActivity.class));
                requireActivity().finish();
            }
        });
    }

    private void loadUserProfile() {
        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            return;
        }

        Log.d(TAG, "Loading profile for user: " + currentUserId);

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            try {
                                // 将文档转换为 User 对象
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    updateUI(user);
                                } else {
                                    Log.e(TAG, "User object is null");
                                    showDefaultValues();
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document to User object", e);
                                showDefaultValues();
                            }
                        } else {
                            Log.d(TAG, "No such document exists");
                            showDefaultValues();
                        }
                    } else {
                        Log.e(TAG, "Get failed with ", task.getException());
                        Toast.makeText(getContext(), "加载用户资料失败", Toast.LENGTH_SHORT).show();
                        showDefaultValues();
                    }
                });
    }

    private void updateUI(User user) {
        if (getActivity() == null) return; // 防止 Fragment 已经销毁

        // 更新用户名
        if (user.getName() != null && !user.getName().trim().isEmpty()) {
            Username.setText(user.getName());
        } else {
            Username.setText("未设置用户名");
        }

        // 更新个人简介
        if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
            bio.setText(user.getBio());
        } else {
            bio.setText("还没有个人简介");
        }

        // 更新头像
        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.userhead) // 默认头像
                    .error(R.drawable.userhead) // 加载失败时的头像
                    .circleCrop() // 圆形头像
                    .into(userpicture);
        } else {
            // 如果没有头像，使用默认头像
            userpicture.setImageResource(R.drawable.userhead);
        }

        Log.d(TAG, "UI updated successfully for user: " + user.getName());
    }

    private void showDefaultValues() {
        if (getActivity() == null) return;

        Username.setText("未设置用户名");
        bio.setText("还没有个人简介");
        userpicture.setImageResource(R.drawable.userhead);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次 Fragment 恢复时重新加载用户资料
        // 这样可以确保从 ProfileSetupActivity 返回时显示最新的资料
        if (currentUserId != null) {
            loadUserProfile();
        }
    }
}