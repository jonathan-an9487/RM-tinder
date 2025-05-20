package com.example.swipecard.newloginregist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.swipecard.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegistFragment extends Fragment {
    public RegistFragment() {
    }
    Button btnregist;
    EditText mTextRegistaccount,mTextRegistpassword,mTextcomfirmpassword;
    FirebaseAuth auth;
    FirebaseAuth.AuthStateListener authStateListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_regist, container, false);

        mTextRegistaccount = view.findViewById(R.id.editTextregistaccount);
        mTextRegistpassword = view.findViewById(R.id.editTextregistPassword);
        mTextcomfirmpassword = view.findViewById(R.id.editTextcheckPassword);
        auth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
            }
        };

        btnregist = view.findViewById(R.id.registbtn);
        btnregist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String regact = mTextRegistaccount.getText().toString().trim();//trim() 去除頭尾無用空格
                String regpsw = mTextRegistpassword.getText().toString().trim();
                String confirmPsw = mTextcomfirmpassword.getText().toString().trim();
                //密碼二確
                if (!regpsw.equals(confirmPsw)) {
                    Toast.makeText(getContext(), "密碼不一致！", Toast.LENGTH_SHORT).show();
                    return;
                }
                //帳號處裡
                if(regact.isEmpty()) {
                    Toast.makeText(getContext(), "請輸入Email！", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!Patterns.EMAIL_ADDRESS.matcher(regact).matches()) {
                    Toast.makeText(getContext(), "請輸入正確Email！", Toast.LENGTH_SHORT).show();
                    return;
                }

                //密碼處裡
                if(regpsw.isEmpty()){
                    Toast.makeText(getContext(), "請輸入密碼！", Toast.LENGTH_SHORT).show();
                    return;
                }else if(regpsw.length()<6){
                    Toast.makeText(getContext(), "請輸入至少6位數密碼", Toast.LENGTH_SHORT).show();
                    return;
                }
                auth.createUserWithEmailAndPassword(regact,regpsw).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                            Toast.makeText(getContext(),"恭喜成功創建帳號！",Toast.LENGTH_LONG).show();
                            getParentFragmentManager().beginTransaction().replace(R.id.viewToSee,new LoginFragment()).addToBackStack(null).commit();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(),"註冊失敗",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        return view;
    }
}