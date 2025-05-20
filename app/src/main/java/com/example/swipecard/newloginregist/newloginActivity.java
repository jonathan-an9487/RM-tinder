package com.example.swipecard.newloginregist;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.swipecard.R;
import com.google.android.material.tabs.TabLayout;

public class newloginActivity extends AppCompatActivity {
    LoginFragment loginFragment;
    RegistFragment registFragment;
    TabLayout tabNavigation;
    FrameLayout viewToSee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_newlogin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        loginFragment = new LoginFragment();
        registFragment = new RegistFragment();

        getSupportFragmentManager().beginTransaction().replace(R.id.viewToSee,loginFragment).commit();

        tabNavigation = findViewById(R.id.tabNavigation);
        tabNavigation.addTab(tabNavigation.newTab().setText("Login"));
        tabNavigation.addTab(tabNavigation.newTab().setText("Register"));

        tabNavigation.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case 0 :
                        getSupportFragmentManager().beginTransaction().replace(R.id.viewToSee,loginFragment).commit();
                        break;
                    case 1 :
                        getSupportFragmentManager().beginTransaction().replace(R.id.viewToSee,registFragment).commit();
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });


    }
}