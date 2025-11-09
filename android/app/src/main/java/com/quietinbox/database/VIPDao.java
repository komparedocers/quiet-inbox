package com.quietinbox.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface VIPDao {

    @Insert
    long insert(VIPEntity vip);

    @Update
    void update(VIPEntity vip);

    @Delete
    void delete(VIPEntity vip);

    @Query("SELECT * FROM vips ORDER BY priority DESC, created_at DESC")
    LiveData<List<VIPEntity>> getAllVIPs();

    @Query("SELECT * FROM vips WHERE id = :id")
    VIPEntity getVIPById(long id);

    @Query("SELECT * FROM vips WHERE app_package = :appPackage AND identifier = :identifier LIMIT 1")
    VIPEntity findVIP(String appPackage, String identifier);

    @Query("SELECT * FROM vips WHERE synced = 0")
    List<VIPEntity> getUnsyncedVIPs();

    @Query("UPDATE vips SET synced = 1 WHERE id = :id")
    void markAsSynced(long id);

    @Query("SELECT COUNT(*) FROM vips")
    int getVIPCount();
}
