package com.example.swipecard.Front;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.swipecard.Chat.ChatRoom;
import com.example.swipecard.R;

import java.util.Date;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    private List<ChatRoom> chatRooms;
    private OnChatRoomClickListener clickListener;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public ChatRoomAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener clickListener) {
        this.chatRooms = chatRooms;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_roommate, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom, clickListener);
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageAvatar;
        private TextView textUserName;
        private TextView textLastMessage;
        private TextView textTime;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.image_avatar);
            textUserName = itemView.findViewById(R.id.text_user_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
            textTime = itemView.findViewById(R.id.text_time);
        }

        public void bind(ChatRoom chatRoom, OnChatRoomClickListener clickListener) {
            // 设置用户名
            textUserName.setText(chatRoom.getOtherUserName());

            // 设置最后一条消息
            String lastMessage = chatRoom.getLastMessage();
            if (lastMessage == null || lastMessage.trim().isEmpty()) {
                textLastMessage.setText("开始聊天吧！");
            } else {
                textLastMessage.setText(lastMessage);
            }

            // 设置时间
            if (chatRoom.getLastMessageTime() != null) {
                Date date = chatRoom.getLastMessageTime().toDate();
                String timeText = DateFormat.format("MM/dd HH:mm", date).toString();
                textTime.setText(timeText);
            } else {
                textTime.setText("");
            }

            // 设置头像
            String imageUrl = chatRoom.getOtherUserImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.tindericon) // 默认头像
                        .error(R.drawable.tindericon) // 加载失败时的头像
                        .circleCrop() // 圆形头像
                        .into(imageAvatar);
            } else {
                imageAvatar.setImageResource(R.drawable.tindericon);
            }

            // 设置点击事件
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onChatRoomClick(chatRoom);
                }
            });
        }
    }
}