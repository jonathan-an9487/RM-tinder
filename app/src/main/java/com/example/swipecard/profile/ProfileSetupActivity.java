package com.example.swipecard.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.swipecard.R;
import com.example.swipecard.chats.chatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProfileSetupActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int STORAGE_PERMISSION_CODE = 100;

    private ImageView profileImageView;
    private EditText nameEditText;
    private EditText bioEditText;
    private Button saveButton;

    private Uri imageUri;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    Button mbackprofileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        // 初始化 Firebase
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference("profile_images");

        // 初始化視圖
        profileImageView = findViewById(R.id.profile_image_view);
        nameEditText = findViewById(R.id.name_edit_text);
        bioEditText = findViewById(R.id.bio_edit_text);
        saveButton = findViewById(R.id.save_button);

        mbackprofileFragment = findViewById(R.id.gobacktoprofilefragment);
        mbackprofileFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileSetupActivity.this, chatActivity.class);
                startActivity(intent);
            }
        });

        // 設置點擊事件
        profileImageView.setOnClickListener(v -> checkPermissionsAndOpenImageChooser());
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void checkPermissionsAndOpenImageChooser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 使用 READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                openImageChooser();
            } else {
                requestStoragePermission(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // 舊版本使用 READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openImageChooser();
            } else {
                requestStoragePermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void requestStoragePermission(String permission) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            new AlertDialog.Builder(this)
                    .setTitle("需要權限")
                    .setMessage("需要存取您的相冊以選擇個人照片")
                    .setPositiveButton("確定", (dialog, which) ->
                            ActivityCompat.requestPermissions(this,
                                    new String[]{permission},
                                    STORAGE_PERMISSION_CODE))
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImageChooser();
            } else {
                Toast.makeText(this, "需要權限才能選擇照片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                // 檢查 URI 是否可讀
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream != null) {
                    inputStream.close();
                }

                Picasso.get()
                        .load(imageUri)
                        .placeholder(R.drawable.usertesthead)
                        .error(R.drawable.error)
                        .fit()
                        .centerCrop()
                        .into(profileImageView);
            } catch (IOException e) {
                Toast.makeText(this, "無法讀取圖片: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("ImageLoad", "Error loading image", e);
            } catch (Exception e) {
                Toast.makeText(this, "圖片加載失敗", Toast.LENGTH_SHORT).show();
                Log.e("ImageLoad", "Error loading image", e);
            }
        }
    }

    private void saveProfile() {
        String name = nameEditText.getText().toString().trim();
        String bio = bioEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "請輸入姓名", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUri == null) {
            Toast.makeText(this, "請選擇個人照片", Toast.LENGTH_SHORT).show();
            return;
        }

        // 再次確認用戶已登入
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "用戶未登入，請重新登入", Toast.LENGTH_SHORT).show();
            auth.signOut();
            finish();
            return;
        }

        saveButton.setEnabled(false);
        Toast.makeText(this, "正在上傳圖片...", Toast.LENGTH_SHORT).show();

        // 上傳圖片到 Firebase Storage
        String imageName = UUID.randomUUID().toString();
        StorageReference imageRef = storageRef.child(imageName);

        // 添加元數據（可選）
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();
        UploadTask uploadTask = imageRef.putFile(imageUri, metadata);

        uploadTask.addOnProgressListener(taskSnapshot -> {
            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
            Log.d("Upload", "Upload is " + progress + "% done");
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "上傳失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            saveButton.setEnabled(true);
            Log.e("Upload", "Upload failed", e);

            // 檢查是否是權限問題
            if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_NOT_AUTHORIZED) {
                Toast.makeText(this, "權限不足，請檢查Firebase Storage規則", Toast.LENGTH_LONG).show();
            }
        }).addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                saveUserData(name, bio, uri.toString());
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "獲取下載鏈接失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
                saveButton.setEnabled(true);
                Log.e("Upload", "Failed to get download URL", e);
            });
        });
    }
    private void saveUserData(String name, String bio, String imageUrl) {
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> user = new HashMap<>();
        user.put("userId", userId);
        user.put("name", name);
        user.put("bio", bio);
        user.put("profileImageUrl", imageUrl);

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "個人資料已保存", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, chatActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "保存失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                    Log.e("Firestore", "Error saving user data", e);
                });
    }
}