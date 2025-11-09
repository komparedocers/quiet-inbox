package com.quietinbox.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room Database for QuietInbox
 */
@Database(
    entities = {
        NotificationEntity.class,
        ProfileEntity.class,
        VIPEntity.class,
        SyncQueueEntity.class,
        UserEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "quietinbox.db";
    private static volatile AppDatabase instance;

    public abstract NotificationDao notificationDao();
    public abstract ProfileDao profileDao();
    public abstract VIPDao vipDao();
    public abstract SyncQueueDao syncQueueDao();
    public abstract UserDao userDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}
