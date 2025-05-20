package com.example.swipecard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.yuyakaido.android.cardstackview.CardStackView;
import java.util.List;

public class CardStackAdapter extends CardStackView.Adapter<CardStackAdapter.ViewHolder> {

    private List<User> users; // 新增：數據列表

    // 新增：構造方法，傳入數據
    public CardStackAdapter(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    // 在 onBindViewHolder 中：
    public void onBindViewHolder(ViewHolder holder, int position) {

        User user = users.get(position);
        holder.name.setText(user.getName());
        holder.bio.setText(user.getBio()); // 新增的自我介紹

        Glide.with(holder.itemView)
                .load(user.getImageUrl())
                .into(holder.image);
    }

    // 新增：必須實現的方法，返回數據數量
    @Override
    public int getItemCount() {
        return users.size();
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