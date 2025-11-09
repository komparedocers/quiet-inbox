package com.quietinbox.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface ProfileDao {

    @Insert
    long insert(ProfileEntity profile);

    @Update
    void update(ProfileEntity profile);

    @Delete
    void delete(ProfileEntity profile);

    @Query("SELECT * FROM profiles ORDER BY created_at DESC")
    LiveData<List<ProfileEntity>> getAllProfiles();

    @Query("SELECT * FROM profiles WHERE is_active = 1 LIMIT 1")
    ProfileEntity getActiveProfile();

    @Query("SELECT * FROM profiles WHERE is_active = 1 LIMIT 1")
    LiveData<ProfileEntity> getActiveProfileLive();

    @Query("SELECT * FROM profiles WHERE id = :id")
    ProfileEntity getProfileById(long id);

    @Query("UPDATE profiles SET is_active = 0")
    void deactivateAllProfiles();

    @Query("UPDATE profiles SET is_active = 1 WHERE id = :id")
    void activateProfile(long id);

    @Query("SELECT * FROM profiles WHERE synced = 0")
    List<ProfileEntity> getUnsyncedProfiles();

    @Query("UPDATE profiles SET synced = 1 WHERE id = :id")
    void markAsSynced(long id);

    @Query("SELECT COUNT(*) FROM profiles")
    int getProfileCount();
}
