package com.quietinbox.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface NotificationDao {

    @Insert
    long insert(NotificationEntity notification);

    @Update
    void update(NotificationEntity notification);

    @Delete
    void delete(NotificationEntity notification);

    @Query("SELECT * FROM notifications WHERE action = :action AND dismissed = 0 ORDER BY received_at DESC")
    LiveData<List<NotificationEntity>> getNotificationsByAction(String action);

    @Query("SELECT * FROM notifications WHERE dismissed = 0 ORDER BY received_at DESC LIMIT :limit")
    LiveData<List<NotificationEntity>> getRecentNotifications(int limit);

    @Query("SELECT * FROM notifications WHERE synced = 0")
    List<NotificationEntity> getUnsyncedNotifications();

    @Query("UPDATE notifications SET synced = 1 WHERE id = :id")
    void markAsSynced(long id);

    @Query("UPDATE notifications SET dismissed = 1 WHERE id = :id")
    void markAsDismissed(long id);

    @Query("DELETE FROM notifications WHERE received_at < :timestamp")
    void deleteOlderThan(long timestamp);

    @Query("SELECT COUNT(*) FROM notifications WHERE action = :action AND dismissed = 0")
    LiveData<Integer> getCountByAction(String action);
}
