package com.example.swipecard.Chat;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swipecard.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatMessageAdapter extends FirestoreRecyclerAdapter<ChatMessage, RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static Activity itemView;
    private final String currentUserId;
    private boolean isScrolledToBottom = false;
    private RecyclerView recyclerView;

    public ChatMessageAdapter(
            @NonNull FirestoreRecyclerOptions<ChatMessage> options,
            String currentUserId
    ) {
        super(options);
        this.currentUserId = currentUserId;

        // 添加數據變化監聽器，自動滾動到底部
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (shouldScrollToBottom()) {
                    scrollToBottom();
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = getItem(position);
        if (message.getSenderId() != null) {
            return message.getSenderId().equals(currentUserId) ? TYPE_SENT : TYPE_RECEIVED;
        }
        return TYPE_SENT; // 默認發送類型
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull ChatMessage message) {
        try {
            // 添加日誌查看綁定數據
            Log.d("ChatAdapter", "綁定訊息: " + message.getContent() +
                    ", 發送者: " + message.getSenderId());

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
        TextView textMessage, textTime;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textTime = itemView.findViewById(R.id.text_time);
        }

        void bind(ChatMessage message) {
            textMessage.setText(message.getContent());
            setTimeText(message);
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
            setTimeText(message);
        }
    }

    // 共用時間設置方法
    private static void setTimeText(ChatMessage message) {
        if (message.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            ((TextView)itemView.findViewById(R.id.text_time))
                    .setText(sdf.format(message.getTimestamp().toDate()));
        }
    }

    // 自動滾動到底部
    private void scrollToBottom() {
        if (getItemCount() > 0) {
            RecyclerView recyclerView = getRecyclerView();
            if (recyclerView != null) {
                recyclerView.post(() -> recyclerView.smoothScrollToPosition(getItemCount() - 1));
            }
        }
    }

    // 判斷是否需要自動滾動
    private boolean shouldScrollToBottom() {
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView == null) return true;

        // 檢查是否接近底部
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return true;

        int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
        int totalItemCount = layoutManager.getItemCount();

        // 如果最後一項可見或接近可見，則滾動到底部
        return (lastVisiblePosition >= totalItemCount - 3);
    }

    // 獲取綁定的RecyclerView
    private RecyclerView getRecyclerView() {
        if (getRecyclerView() != null && getRecyclerView().getAdapter() == this) {
            return getRecyclerView();
        }
        return null;
    }

    // 在ChatActivity中設置RecyclerView
    public void attachToRecyclerView(RecyclerView recyclerView) {
        recyclerView.setAdapter(this);
        this.recyclerView = recyclerView;
    }
}