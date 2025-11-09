package com.quietinbox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Entity representing a notification event
 */
@Entity(tableName = "notifications")
public class NotificationEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "app_package")
    public String appPackage;

    @ColumnInfo(name = "app_name")
    public String appName;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "text")
    public String text;

    @ColumnInfo(name = "topic")
    public String topic;

    @ColumnInfo(name = "received_at")
    public long receivedAt;

    @ColumnInfo(name = "action")
    public String action; // NOW, LATER, NEVER

    @ColumnInfo(name = "confidence")
    public float confidence;

    @ColumnInfo(name = "is_vip")
    public boolean isVip;

    @ColumnInfo(name = "synced")
    public boolean synced;

    @ColumnInfo(name = "dismissed")
    public boolean dismissed;

    public NotificationEntity() {
        this.receivedAt = System.currentTimeMillis();
        this.synced = false;
        this.dismissed = false;
    }
}
