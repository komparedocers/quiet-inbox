package com.quietinbox;

import android.app.Application;
import android.provider.Settings;
import android.util.Log;

import com.quietinbox.database.AppDatabase;
import com.quietinbox.database.ProfileEntity;
import com.quietinbox.database.UserEntity;
import com.quietinbox.utils.AdManager;
import com.quietinbox.utils.BillingManager;
import com.quietinbox.utils.ConfigLoader;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Application class for QuietInbox
 * Initializes core components and handles first-run setup
 */
public class QuietInboxApplication extends Application {
    private static final String TAG = "QuietInboxApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "QuietInbox Application starting...");

        try {
            // Initialize configuration
            ConfigLoader.getInstance(this);

            // Initialize AdMob
            AdManager adManager = AdManager.getInstance(this);
            adManager.preloadInterstitialAd(this);

            // Initialize Billing
            BillingManager.getInstance(this);

            // Initialize database and create default user if needed
            initializeDatabase();

            Log.d(TAG, "QuietInbox Application initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing application", e);
        }
    }

    private void initializeDatabase() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);

                // Check if user exists
                UserEntity user = db.userDao().getUser();
                if (user == null) {
                    // Create default user
                    user = new UserEntity();
                    user.deviceId = getDeviceId();
                    user.email = null;
                    user.isPro = false;
                    user.createdAt = System.currentTimeMillis();

                    long userId = db.userDao().insert(user);
                    Log.d(TAG, "Created default user with ID: " + userId);

                    // Create default profile
                    ProfileEntity defaultProfile = new ProfileEntity();
                    defaultProfile.name = "Default";
                    defaultProfile.quietHoursStart = "22:00";
                    defaultProfile.quietHoursEnd = "07:00";
                    defaultProfile.rulesJson = "{}";
                    defaultProfile.isActive = true;

                    long profileId = db.profileDao().insert(defaultProfile);
                    Log.d(TAG, "Created default profile with ID: " + profileId);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error initializing database", e);
            }
        });
    }

    private String getDeviceId() {
        try {
            String androidId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
            );
            return androidId != null ? androidId : UUID.randomUUID().toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
