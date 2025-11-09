package com.quietinbox.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Entity representing pending sync operations
 */
@Entity(tableName = "sync_queue")
public class SyncQueueEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "operation")
    public String operation; // CREATE, UPDATE, DELETE

    @ColumnInfo(name = "entity_type")
    public String entityType; // profile, vip, notification

    @ColumnInfo(name = "entity_id")
    public long entityId;

    @ColumnInfo(name = "data_json")
    public String dataJson;

    @ColumnInfo(name = "status")
    public String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "retry_count")
    public int retryCount;

    public SyncQueueEntity() {
        this.createdAt = System.currentTimeMillis();
        this.status = "PENDING";
        this.retryCount = 0;
    }
}
