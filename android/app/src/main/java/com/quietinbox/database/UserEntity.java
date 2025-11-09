package com.quietinbox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Entity representing the local user
 */
@Entity(tableName = "user")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "server_id")
    public Long serverId; // ID from server, null if not synced

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "device_id")
    public String deviceId;

    @ColumnInfo(name = "access_token")
    public String accessToken;

    @ColumnInfo(name = "is_pro")
    public boolean isPro;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "last_sync")
    public Long lastSync;

    public UserEntity() {
        this.createdAt = System.currentTimeMillis();
        this.isPro = false;
    }
}
