package com.quietinbox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SyncQueueDao {

    @Insert
    long insert(SyncQueueEntity syncItem);

    @Update
    void update(SyncQueueEntity syncItem);

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY created_at ASC")
    List<SyncQueueEntity> getPendingItems();

    @Query("UPDATE sync_queue SET status = :status WHERE id = :id")
    void updateStatus(long id, String status);

    @Query("UPDATE sync_queue SET retry_count = retry_count + 1 WHERE id = :id")
    void incrementRetry(long id);

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    void deleteCompleted();

    @Query("DELETE FROM sync_queue WHERE id = :id")
    void delete(long id);
}
