package com.quietinbox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Entity representing a user profile (Work/Personal/Travel)
 */
@Entity(tableName = "profiles")
public class ProfileEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "server_id")
    public Long serverId; // ID from server, null if not synced

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "quiet_hours_start")
    public String quietHoursStart; // HH:MM format

    @ColumnInfo(name = "quiet_hours_end")
    public String quietHoursEnd; // HH:MM format

    @ColumnInfo(name = "rules_json")
    public String rulesJson;

    @ColumnInfo(name = "is_active")
    public boolean isActive;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "synced")
    public boolean synced;

    public ProfileEntity() {
        this.createdAt = System.currentTimeMillis();
        this.synced = false;
        this.rulesJson = "{}";
    }
}
