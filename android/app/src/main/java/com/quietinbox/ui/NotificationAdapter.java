package com.quietinbox.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.quietinbox.R;
import com.quietinbox.database.NotificationEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying notifications in RecyclerView
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationEntity> notifications;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(NotificationEntity notification);
    }

    public NotificationAdapter(List<NotificationEntity> notifications, OnItemClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationEntity notification = notifications.get(position);

        holder.appNameText.setText(notification.appName);
        holder.titleText.setText(notification.title);
        holder.messageText.setText(notification.text);
        holder.timeText.setText(formatTime(notification.receivedAt));

        if (notification.isVip) {
            holder.vipIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.vipIndicator.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateData(List<NotificationEntity> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView appNameText;
        TextView titleText;
        TextView messageText;
        TextView timeText;
        View vipIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            appNameText = itemView.findViewById(R.id.appNameText);
            titleText = itemView.findViewById(R.id.titleText);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
            vipIndicator = itemView.findViewById(R.id.vipIndicator);
        }
    }
}
