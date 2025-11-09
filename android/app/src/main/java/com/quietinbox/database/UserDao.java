package com.quietinbox.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserDao {

    @Insert
    long insert(UserEntity user);

    @Update
    void update(UserEntity user);

    @Query("SELECT * FROM user LIMIT 1")
    UserEntity getUser();

    @Query("UPDATE user SET access_token = :token WHERE id = 1")
    void updateToken(String token);

    @Query("UPDATE user SET is_pro = :isPro WHERE id = 1")
    void updateProStatus(boolean isPro);

    @Query("UPDATE user SET last_sync = :timestamp WHERE id = 1")
    void updateLastSync(long timestamp);
}
