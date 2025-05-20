package com.example.swipecard.chats;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.swipecard.MainActivity;
import com.example.swipecard.profile.ProfileFragment;
import com.example.swipecard.R;
import com.example.swipecard.swipeCard.SwipeCardFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class chatActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationMenuView;
    ImageButton imageButton;
    ChatFragment chatFragment;
    ProfileFragment profileFragment;
    SwipeCardFragment swipecardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        chatFragment = new ChatFragment();
        profileFragment = new ProfileFragment();
        swipecardFragment = new SwipeCardFragment();



        bottomNavigationMenuView = findViewById(R.id.bottom_navigation);
        bottomNavigationMenuView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId()==R.id.menu_chats){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout,chatFragment).commit();
                }
                if(item.getItemId()==R.id.menu_profile){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout,profileFragment).commit();
                }
                if(item.getItemId()==R.id.menu_card){
                    getSupportFragmentManager().beginTransaction().replace(R.id.main_frame_layout, swipecardFragment).commit();
                }
                return true;
            }
        });
        bottomNavigationMenuView.setSelectedItemId(R.id.menu_card);
    }
}