package com.example.swipecard;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String profileImageUrl; // 统一使用这个字段名
    private String name;
    private String bio;
    private String userId;
    private int swipeStatus = 0; // 0:未滑, 1:喜歡, -1:不喜歡

    // Firebase 需要的空構造函數
    public User() {}

    // 修改构造方法参数名
    public User(String userId, String name, String bio, String profileImageUrl) {
        this.userId = userId;
        this.name = name;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    // 重点修改：统一使用 profileImageUrl 命名
    public String getProfileImageUrl() {
        return profileImageUrl != null ? profileImageUrl : ""; // 防止null
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    @Exclude
    public int getSwipeStatus() { return swipeStatus; }
    @Exclude
    public void setSwipeStatus(int swipeStatus) { this.swipeStatus = swipeStatus; }
}