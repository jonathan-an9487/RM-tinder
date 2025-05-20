package com.example.swipecard.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.swipecard.R;
import com.example.swipecard.User;


public class ProfileFragment extends Fragment {
    public ProfileFragment() {
    }
    ImageView userpicture;
    TextView Username,bio;
    Button mprofilesetup;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userpicture = view.findViewById(R.id.userpicture);

        Username = view.findViewById(R.id.Username);
        bio = view.findViewById(R.id.bio);



        mprofilesetup = view.findViewById(R.id.toprofilesetup);
        mprofilesetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(),ProfileSetupActivity.class));
                requireActivity().finish();
            }
        });



        return view;
    }
}