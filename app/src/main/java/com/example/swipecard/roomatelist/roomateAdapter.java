package com.example.swipecard.roomatelist;

import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class roomateAdapter extends RecyclerView.Adapter<roomateAdapter.ViewHolder> {

    private List<ChatRoom> chatRooms;
    private OnChatRoomClickListener listener;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom);
    }

    public roomateAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener listener) {
        this.chatRooms = chatRooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roommate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        Log.d("RoomateAdapter", "Binding chat room: " + chatRoom.getChatId() +
                ", other user: " + chatRoom.getOtherUserName());

        // 設置用戶名稱
        holder.textUserName.setText(chatRoom.getOtherUserName());

        // 設置最後訊息 - 添加發送者標示
        String lastMessage = chatRoom.getLastMessage();
        if (chatRoom.getLastSenderId() != null &&
                chatRoom.getLastSenderId().equals(chatRoom.getParticipants().get(0))) {
            lastMessage = "你: " + lastMessage;
        }
        holder.textLastMessage.setText(lastMessage);

        // 設置時間
        if (chatRoom.getLastMessageTime() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            String timeString = sdf.format(chatRoom.getLastMessageTime().toDate());
            holder.textTime.setText(timeString);
        } else {
            holder.textTime.setText("");
        }

        // 設置頭像
        if (chatRoom.getOtherUserImageUrl() != null &&
                !chatRoom.getOtherUserImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(chatRoom.getOtherUserImageUrl())
                    .circleCrop()
                    .placeholder(R.drawable.userhead)
                    .error(R.drawable.userhead)
                    .into(holder.imageAvatar);
        } else {
            holder.imageAvatar.setImageResource(R.drawable.userhead);
        }

        // 點擊事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatRoomClick(chatRoom);
            }
        });
    }


    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageAvatar;
        TextView textUserName;
        TextView textLastMessage;
        TextView textTime;

        ViewHolder(View itemView) {
            super(itemView);
            imageAvatar = itemView.findViewById(R.id.image_avatar);
            textUserName = itemView.findViewById(R.id.text_user_name);
            textLastMessage = itemView.findViewById(R.id.text_last_message);
            textTime = itemView.findViewById(R.id.text_time);
        }
    }
}