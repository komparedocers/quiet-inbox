package com.quietinbox.services;

import android.content.Context;

import com.quietinbox.api.ApiClient;
import com.quietinbox.api.ApiService;
import com.quietinbox.database.*;
import com.quietinbox.models.*;
import com.quietinbox.utils.ConfigLoader;
import com.quietinbox.utils.Logger;

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
        Logger.enter(TAG, "SyncManager constructor");
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        Logger.d(TAG, "Database initialized");
        this.apiService = ApiClient.getInstance(context).getService();
        Logger.d(TAG, "API service initialized");
        this.executorService = Executors.newSingleThreadExecutor();
        Logger.d(TAG, "Executor service initialized");
        this.config = ConfigLoader.getInstance(context);
        Logger.i(TAG, "SyncManager initialized successfully");
        Logger.exit(TAG, "SyncManager constructor");
    }

    public static synchronized SyncManager getInstance(Context context) {
        if (instance == null) {
            Logger.d(TAG, "Creating new SyncManager instance");
            instance = new SyncManager(context);
        }
        return instance;
    }

    /**
     * Sync all pending changes to server
     */
    public void syncAll(SyncCallback callback) {
        Logger.enter(TAG, "syncAll");
        Logger.separator(TAG, "SYNC ALL START");

        executorService.execute(() -> {
            Logger.Timer timer = new Logger.Timer(TAG, "syncAll");

            try {
                // Check network availability
                Logger.d(TAG, "Checking network availability...");
                if (!isNetworkAvailable()) {
                    Logger.w(TAG, "Network not available, aborting sync");
                    Logger.network(TAG, false);
                    if (callback != null) callback.onError("Network not available");
                    return;
                }
                Logger.network(TAG, true);

                // Check user authentication
                Logger.d(TAG, "Checking user authentication...");
                UserEntity user = database.userDao().getUser();
                if (user == null || user.accessToken == null) {
                    Logger.w(TAG, "User not logged in, aborting sync");
                    if (callback != null) callback.onError("Not logged in");
                    return;
                }
                Logger.i(TAG, "User authenticated, token available");

                String token = "Bearer " + user.accessToken;

                // Sync profiles
                Logger.d(TAG, "Starting profile sync...");
                syncProfiles(token);
                Logger.i(TAG, "Profile sync completed");

                // Sync VIPs
                Logger.d(TAG, "Starting VIP sync...");
                syncVIPs(token);
                Logger.i(TAG, "VIP sync completed");

                // Sync notifications
                Logger.d(TAG, "Starting notification sync...");
                syncNotifications(token);
                Logger.i(TAG, "Notification sync completed");

                // Update last sync time
                long syncTime = System.currentTimeMillis();
                Logger.d(TAG, "Updating last sync time: " + syncTime);
                database.userDao().updateLastSync(syncTime);
                Logger.db(TAG, "UPDATE user", "last_sync = " + syncTime);

                Logger.separator(TAG, "SYNC ALL SUCCESS");
                Logger.i(TAG, "All sync operations completed successfully");

                if (callback != null) callback.onSuccess();

            } catch (Exception e) {
                Logger.e(TAG, "Sync error - operation failed", e);
                Logger.separator(TAG, "SYNC ALL FAILED");
                if (callback != null) callback.onError(e.getMessage());
            } finally {
                timer.stop();
                Logger.exit(TAG, "syncAll");
            }
        });
    }

    private void syncProfiles(String token) {
        Logger.enter(TAG, "syncProfiles");

        try {
            List<ProfileEntity> unsyncedProfiles = database.profileDao().getUnsyncedProfiles();
            Logger.sync(TAG, "Profiles to sync", unsyncedProfiles.size());

            for (ProfileEntity profile : unsyncedProfiles) {
                try {
                    Logger.d(TAG, "Syncing profile: " + profile.name + " (ID: " + profile.id + ")");

                    Profile apiProfile = new Profile();
                    apiProfile.name = profile.name;
                    apiProfile.quiet_hours_start = profile.quietHoursStart;
                    apiProfile.quiet_hours_end = profile.quietHoursEnd;
                    apiProfile.rules_json = profile.rulesJson;
                    apiProfile.is_active = profile.isActive;

                    Response<Profile> response;
                    if (profile.serverId == null) {
                        Logger.d(TAG, "Creating new profile on server");
                        Logger.api(TAG, "/v1/profile", "POST");
                        response = apiService.createProfile(token, apiProfile).execute();
                    } else {
                        Logger.d(TAG, "Updating existing profile on server (serverId: " + profile.serverId + ")");
                        Logger.api(TAG, "/v1/profile/" + profile.serverId, "PUT");
                        response = apiService.updateProfile(token, profile.serverId, apiProfile).execute();
                    }

                    Logger.apiResponse(TAG, "/v1/profile", response.code(), response.message());

                    if (response.isSuccessful() && response.body() != null) {
                        profile.serverId = response.body().id;
                        profile.synced = true;
                        database.profileDao().update(profile);
                        Logger.db(TAG, "UPDATE profile", "ID: " + profile.id + ", synced=true");
                        Logger.i(TAG, "Profile synced successfully: " + profile.name);
                    } else {
                        Logger.w(TAG, "Profile sync failed: " + response.code() + " " + response.message());
                    }
                } catch (IOException e) {
                    Logger.e(TAG, "Network error syncing profile: " + profile.name, e);
                }
            }

            Logger.sync(TAG, "Profiles synced", unsyncedProfiles.size());
        } catch (Exception e) {
            Logger.e(TAG, "Error in syncProfiles", e);
        }

        Logger.exit(TAG, "syncProfiles");
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
