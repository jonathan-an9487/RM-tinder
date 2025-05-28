package com.example.swipecard.Chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swipecard.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends FirestoreRecyclerAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private final String currentUserId;
    private FirebaseFirestore db;

    public ChatMessageAdapter(
            @NonNull FirestoreRecyclerOptions<ChatMessage> options,
            String currentUserId
    ) {
        super(options);
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        return message.getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    protected void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position,
            @NonNull ChatMessage message
    ) {
        try {
            if (holder instanceof SentMessageViewHolder) {
                ((SentMessageViewHolder) holder).bind(message);
            } else if (holder instanceof ReceivedMessageViewHolder) {
                ((ReceivedMessageViewHolder) holder).bind(message);
            }
        } catch (Exception e) {
            Log.e("ChatAdapter", "綁定視圖時出錯", e);
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime, textStatus;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                textTime.setText(sdf.format(message.getTimestamp().toDate()));
            }
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage, textTime;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                textTime.setText(sdf.format(message.getTimestamp().toDate()));
            }
        }
    }
}