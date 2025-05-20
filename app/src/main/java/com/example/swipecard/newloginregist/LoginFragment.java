package com.example.swipecard.newloginregist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.example.swipecard.R;
import com.example.swipecard.chats.chatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class LoginFragment extends Fragment {
    public LoginFragment() {
    }
    EditText meditTextaccount,meditTextpassword;
    Button btnlogin;
    CheckBox rememberme;
    SharedPreferences sharedPreferences;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener authStateListener;
    String PREFS_NAME = "LoginPrefs";
    String KEY_EMAIL = "email";
    String KEY_PASSWORD = "password";
    String KEY_REMEMBER = "remember";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_login, container, false);
        View view = inflater.inflate(R.layout.fragment_login,container,false);

        mAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
            }
        };
        rememberme = view.findViewById(R.id.rememberactpsw);

        meditTextaccount = view.findViewById(R.id.edittextaccount);
        meditTextpassword = view.findViewById(R.id.editTextPassword);

        sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
        loadSavedCredentials();//自定函數

        btnlogin = view.findViewById(R.id.loginbtn);
        btnlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String act = meditTextaccount.getText().toString().trim();
                String psw = meditTextpassword.getText().toString().trim();

                //帳號處裡
                if(act.isEmpty()) {
                    Toast.makeText(getContext(), "請輸入Email！", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!Patterns.EMAIL_ADDRESS.matcher(act).matches()) {
                    Toast.makeText(getContext(), "請輸入正確Email！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //密碼處裡
                if(psw.isEmpty()){
                    Toast.makeText(getContext(), "請輸入密碼！", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(act,psw).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                                Toast.makeText(getContext(), "登入成功！", Toast.LENGTH_LONG).show();
                                saveCredentials(act, psw);
                                startActivity(new Intent(getActivity(), chatActivity.class));
                                requireActivity().finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),"登入失敗，請確認帳號密碼是否有誤！",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        return view;

    }
    void loadSavedCredentials() {
        try {
            if (sharedPreferences == null) {
                sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
            }
            boolean remember = sharedPreferences.getBoolean("remember", false);
            if (remember) {
                String email = sharedPreferences.getString("email", "");
                String password = sharedPreferences.getString("password", "");
                if (!email.isEmpty() && meditTextaccount != null) {
                    meditTextaccount.setText(email);
                }
                if (!password.isEmpty() && meditTextpassword != null) {
                    meditTextpassword.setText(password);
                }
                if (rememberme != null) {
                    rememberme.setChecked(true);
                }
                Log.d("LoadDebug", "已載入資料: " + email);
            }
        } catch (Exception e) {
            Log.e("LoadError", "載入失敗", e);
        }
    }

    void saveCredentials(String email, String password) {
        try {
            // 確保元件已正確綁定
            if (rememberme == null || sharedPreferences == null) {
                Log.e("Login", "元件未初始化");
                return;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (rememberme.isChecked()) {
                editor.putString("email", email);
                editor.putString("password", password);
                editor.putBoolean("remember", true);
            } else {
                // 若取消勾選，清除保存的資料
                editor.remove("email");
                editor.remove("password");
                editor.putBoolean("remember", false);
            }
            editor.apply(); // 或改用 commit() 確保即時保存
            Log.d("SaveDebug", "已保存資料: " + email);
        } catch (Exception e) {
            Log.e("SaveError", "保存失敗", e);
        }
    }


}