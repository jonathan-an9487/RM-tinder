package com.example.swipecard.Loding;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.swipecard.R;
import com.example.swipecard.newloginregist.newloginActivity;

public class LodingActivity extends AppCompatActivity {
    Animation wordanimation,imageanimation;
    TextView logoword;
    ImageView icon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_loding);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        wordanimation = AnimationUtils.loadAnimation(LodingActivity.this,R.anim.animation);
        imageanimation = AnimationUtils.loadAnimation(LodingActivity.this,R.anim.animation);

        icon = findViewById(R.id.imageView);
        logoword = findViewById(R.id.textView);

        icon.setAnimation(imageanimation);
        logoword.setAnimation(wordanimation);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(LodingActivity.this, newloginActivity.class);
                startActivity(intent);
                finish();
            }
        },2000);
    }
}