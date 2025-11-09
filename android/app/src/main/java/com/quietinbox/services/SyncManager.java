package com.quietinbox.services;

import android.content.Context;
import android.util.Log;

import com.quietinbox.api.ApiClient;
import com.quietinbox.api.ApiService;
import com.quietinbox.database.*;
import com.quietinbox.models.*;
import com.quietinbox.utils.ConfigLoader;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Manages offline-first synchronization with backend server
 * Handles network failures gracefully and queues operations
 */
public class SyncManager {
    private static final String TAG = "SyncManager";
    private static SyncManager instance;

    private final Context context;
    private final AppDatabase database;
    private final ApiService apiService;
    private final ExecutorService executorService;
    private final ConfigLoader config;

    private SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.apiService = ApiClient.getInstance(context).getService();
        this.executorService = Executors.newSingleThreadExecutor();
        this.config = ConfigLoader.getInstance(context);
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            instance = new SyncManager(context);
        }
        return instance;
    }

    /**
     * Sync all pending changes to server
     */
    public void syncAll(SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (!isNetworkAvailable()) {
                    Log.w(TAG, "Network not available, skipping sync");
                    if (callback != null) callback.onError("Network not available");
                    return;
                }

                UserEntity user = database.userDao().getUser();
                if (user == null || user.accessToken == null) {
                    Log.w(TAG, "User not logged in, skipping sync");
                    if (callback != null) callback.onError("Not logged in");
                    return;
                }

                String token = "Bearer " + user.accessToken;

                // Sync profiles
                syncProfiles(token);

                // Sync VIPs
                syncVIPs(token);

                // Sync notifications
                syncNotifications(token);

                // Update last sync time
                database.userDao().updateLastSync(System.currentTimeMillis());

                Log.i(TAG, "Sync completed successfully");
                if (callback != null) callback.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "Sync error", e);
                if (callback != null) callback.onError(e.getMessage());
            }
        });
    }

    private void syncProfiles(String token) {
        try {
            List<ProfileEntity> unsyncedProfiles = database.profileDao().getUnsyncedProfiles();

            for (ProfileEntity profile : unsyncedProfiles) {
                try {
                    Profile apiProfile = new Profile();
                    apiProfile.name = profile.name;
                    apiProfile.quiet_hours_start = profile.quietHoursStart;
                    apiProfile.quiet_hours_end = profile.quietHoursEnd;
                    apiProfile.rules_json = profile.rulesJson;
                    apiProfile.is_active = profile.isActive;

                    Response<Profile> response;
                    if (profile.serverId == null) {
                        // Create new profile
                        response = apiService.createProfile(token, apiProfile).execute();
                    } else {
                        // Update existing profile
                        response = apiService.updateProfile(token, profile.serverId, apiProfile).execute();
                    }

                    if (response.isSuccessful() && response.body() != null) {
                        profile.serverId = response.body().id;
                        profile.synced = true;
                        database.profileDao().update(profile);
                        Log.d(TAG, "Profile synced: " + profile.name);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error syncing profile: " + profile.name, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing profiles", e);
        }
    }

    private void syncVIPs(String token) {
        try {
            List<VIPEntity> unsyncedVIPs = database.vipDao().getUnsyncedVIPs();

            for (VIPEntity vip : unsyncedVIPs) {
                try {
                    VIP apiVip = new VIP();
                    apiVip.app_package = vip.appPackage;
                    apiVip.identifier = vip.identifier;
                    apiVip.priority = vip.priority;
                    apiVip.bypass_quiet_hours = vip.bypassQuietHours;

                    Response<VIP> response = apiService.createVIP(token, apiVip).execute();

                    if (response.isSuccessful() && response.body() != null) {
                        vip.serverId = response.body().id;
                        vip.synced = true;
                        database.vipDao().update(vip);
                        Log.d(TAG, "VIP synced: " + vip.displayName);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error syncing VIP: " + vip.displayName, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing VIPs", e);
        }
    }

    private void syncNotifications(String token) {
        try {
            List<NotificationEntity> unsyncedNotifications = database.notificationDao().getUnsyncedNotifications();

            if (unsyncedNotifications.isEmpty()) {
                return;
            }

            // Build sync request
            List<Map<String, Object>> items = new ArrayList<>();
            for (NotificationEntity notif : unsyncedNotifications) {
                Map<String, Object> item = new HashMap<>();
                item.put("local_id", String.valueOf(notif.id));
                item.put("type", "notification");

                Map<String, Object> data = new HashMap<>();
                data.put("app_package", notif.appPackage);
                data.put("title", notif.title);
                data.put("text", notif.text);
                data.put("action", notif.action);
                data.put("confidence", notif.confidence);
                data.put("received_at", notif.receivedAt);

                item.put("data", data);
                items.add(item);
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            String timestamp = sdf.format(new Date());

            SyncPushRequest request = new SyncPushRequest(items, timestamp);
            Response<SyncResponse> response = apiService.pushSync(token, request).execute();

            if (response.isSuccessful() && response.body() != null) {
                // Mark notifications as synced
                for (NotificationEntity notif : unsyncedNotifications) {
                    database.notificationDao().markAsSynced(notif.id);
                }
                Log.d(TAG, "Synced " + response.body().synced_count + " notifications");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing notifications", e);
        }
    }

    /**
     * Check backend server health
     */
    public void checkServerHealth(HealthCheckCallback callback) {
        executorService.execute(() -> {
            try {
                Response<HealthResponse> response = apiService.healthCheck().execute();
                if (response.isSuccessful() && response.body() != null) {
                    boolean isHealthy = "healthy".equals(response.body().status) ||
                                      "ok".equals(response.body().status);
                    if (callback != null) callback.onResult(isHealthy);
                } else {
                    if (callback != null) callback.onResult(false);
                }
            } catch (Exception e) {
                Log.e(TAG, "Health check failed", e);
                if (callback != null) callback.onResult(false);
            }
        });
    }

    private boolean isNetworkAvailable() {
        // In a real app, check actual network connectivity
        // For now, always return true and let API calls handle failures
        return config.isOfflineModeEnabled();
    }

    /**
     * Sync callback interface
     */
    public interface SyncCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Health check callback interface
     */
    public interface HealthCheckCallback {
        void onResult(boolean isHealthy);
    }
}
