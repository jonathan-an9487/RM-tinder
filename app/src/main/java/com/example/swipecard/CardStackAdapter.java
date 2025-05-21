package com.example.swipecard;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.yuyakaido.android.cardstackview.CardStackView;

import java.util.ArrayList;
import java.util.List;

public class CardStackAdapter extends CardStackView.Adapter<CardStackAdapter.ViewHolder> {

    private List<User> users;

    public CardStackAdapter(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = users.get(position);

        // 设置名字和简介
        holder.name.setText(user.getName() != null ? user.getName() : "未知用户");
        holder.bio.setText(user.getBio() != null ? user.getBio() : "暂无简介");

        // 调试日志
        Log.d("UserData", "显示用户: " + user.getName() +
                " | 简介: " + user.getBio() +
                " | 图片: " + user.getProfileImageUrl());

        // 图片加载代码保持不变...
        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.tindericon)
                .error(R.drawable.error)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<User> newUsers) {
        this.users.clear();
        this.users.addAll(newUsers);
        notifyDataSetChanged();
    }

    public void addUser(User user) {
        this.users.add(user);
        notifyItemInserted(users.size() - 1);
    }

    public void removeUser(int position) {
        if (position >= 0 && position < users.size()) {
            users.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class ViewHolder extends CardStackView.ViewHolder {
        TextView name, bio;
        ImageView image;

        public ViewHolder(View view) {
            super(view);
            name = view.findViewById(R.id.item_name);
            bio = view.findViewById(R.id.item_bio);
            image = view.findViewById(R.id.item_image);
        }
    }
}