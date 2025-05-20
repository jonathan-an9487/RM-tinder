package com.example.swipecard;

public class User {
    private String userId;  // 未來用Firebase ID
    private String name;
    private String bio;
    private String imageUrl;
    private int swipeStatus; // 0:未滑, 1:喜歡, -1:不喜歡

    // 空構造函數 (Firebase 需要)
    public User() {}

    public void setSwipeStatus(int status) {
        this.swipeStatus = status;
    }

    // 構造函數 (暫時手動生成假ID)
    public User(String name, String bio, String imageUrl) {
        this.userId = "temp_" + System.currentTimeMillis(); // 臨時ID
        this.name = name;
        this.bio = bio;
        this.imageUrl = imageUrl;
        this.swipeStatus = 0;
    }


    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getImageUrl() { return imageUrl; } // 新增 Getter
    public String getUserId(){return userId;}

}