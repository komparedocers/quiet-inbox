package com.quietinbox.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Background service for periodic sync
 */
public class SyncService extends Service {
    private static final String TAG = "SyncService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Sync service started");

        // Perform sync
        SyncManager syncManager = SyncManager.getInstance(this);
        syncManager.syncAll(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Background sync completed");
                stopSelf(startId);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Background sync failed: " + error);
                stopSelf(startId);
            }
        });

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
