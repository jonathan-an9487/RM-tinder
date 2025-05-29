package com.example.swipecard.Front;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.swipecard.profile.ProfileFragment;
import com.example.swipecard.R;
import com.example.swipecard.roomatelist.roomatelist;
import com.example.swipecard.swipeCard.SwipeCardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class FrontActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationMenuView;
    ImageButton imageButton;

    FrontFragment chatFragment;
    ProfileFragment profileFragment;
    SwipeCardFragment swipecardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_front);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 初始化Fragment
        chatFragment = new FrontFragment();
        profileFragment = new ProfileFragment();
        swipecardFragment = new SwipeCardFragment();

        bottomNavigationMenuView = findViewById(R.id.bottom_navigation);
        bottomNavigationMenuView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.menu_chats) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_layout, chatFragment)
                            .commit();
                    // 刷新聊天室列表
                    if (chatFragment != null) {
                        chatFragment.refreshChatRooms();
                    }
                }

                if (item.getItemId() == R.id.menu_profile) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_layout, profileFragment)
                            .commit();
                }

                if (item.getItemId() == R.id.menu_card) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_frame_layout, swipecardFragment)
                            .commit();
                }

                return true;
            }
        });

        // 默认显示卡片页面
        bottomNavigationMenuView.setSelectedItemId(R.id.menu_card);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果当前显示的是聊天页面，刷新聊天室列表
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.main_frame_layout);
        if (currentFragment instanceof FrontFragment) {
            ((FrontFragment) currentFragment).refreshChatRooms();
        }
    }
}