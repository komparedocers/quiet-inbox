package com.quietinbox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Entity representing a VIP contact
 */
@Entity(tableName = "vips")
public class VIPEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "server_id")
    public Long serverId; // ID from server, null if not synced

    @ColumnInfo(name = "app_package")
    public String appPackage;

    @ColumnInfo(name = "identifier")
    public String identifier; // phone number, email, sender name, etc.

    @ColumnInfo(name = "display_name")
    public String displayName;

    @ColumnInfo(name = "priority")
    public int priority; // 1-5

    @ColumnInfo(name = "bypass_quiet_hours")
    public boolean bypassQuietHours;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "synced")
    public boolean synced;

    public VIPEntity() {
        this.createdAt = System.currentTimeMillis();
        this.synced = false;
        this.priority = 1;
        this.bypassQuietHours = true;
    }
}
