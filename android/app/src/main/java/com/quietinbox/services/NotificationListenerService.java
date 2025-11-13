package com.quietinbox.services;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import com.quietinbox.database.AppDatabase;
import com.quietinbox.database.NotificationEntity;
import com.quietinbox.utils.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Listens to all notifications and processes them through QuietInbox
 */
public class NotificationListenerService extends android.service.notification.NotificationListenerService {
    private static final String TAG = "NotifListener";

    private AppDatabase database;
    private NotificationClassifier classifier;
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.lifecycle(TAG, "Service created");
        Logger.enter(TAG, "onCreate");

        try {
            database = AppDatabase.getInstance(this);
            Logger.d(TAG, "Database instance initialized");

            classifier = new NotificationClassifier(this);
            Logger.d(TAG, "Classifier initialized");

            executorService = Executors.newSingleThreadExecutor();
            Logger.d(TAG, "ExecutorService initialized");

            Logger.i(TAG, "NotificationListenerService successfully initialized");
        } catch (Exception e) {
            Logger.e(TAG, "Error initializing NotificationListenerService", e);
        }

        Logger.exit(TAG, "onCreate");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Logger.enter(TAG, "onNotificationPosted");

        try {
            String packageName = sbn.getPackageName();
            Logger.notification(TAG, packageName, "Posted");

            // Ignore our own notifications
            if (packageName.equals(getPackageName())) {
                Logger.d(TAG, "Ignoring own notification");
                return;
            }

            // Ignore ongoing notifications (media playback, etc.)
            if ((sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != 0) {
                Logger.d(TAG, "Ignoring ongoing notification from: " + packageName);
                return;
            }

            Logger.i(TAG, "Processing notification from: " + packageName);

            // Process in background
            executorService.execute(() -> processNotification(sbn));

        } catch (Exception e) {
            Logger.e(TAG, "Error in onNotificationPosted", e);
        }

        Logger.exit(TAG, "onNotificationPosted");
    }

    private void processNotification(StatusBarNotification sbn) {
        Logger.enter(TAG, "processNotification");
        Logger.Timer timer = new Logger.Timer(TAG, "processNotification");

        try {
            String packageName = sbn.getPackageName();
            Logger.d(TAG, "Starting processing for package: " + packageName);

            // Classify the notification
            Logger.d(TAG, "Classifying notification...");
            NotificationClassifier.ClassificationResult result = classifier.classify(sbn);
            Logger.i(TAG, "Classification result: " + result.action +
                " (confidence: " + result.confidence + ", isVip: " + result.isVip + ")");

            // Get app name
            String appName = getAppName(packageName);
            Logger.d(TAG, "App name resolved: " + appName);

            // Extract notification details
            String title = getTitle(sbn);
            String text = getText(sbn);
            Logger.d(TAG, "Notification content - Title: " +
                (title != null && title.length() > 30 ? title.substring(0, 30) + "..." : title) +
                ", Text length: " + (text != null ? text.length() : 0));

            // Create entity
            NotificationEntity entity = new NotificationEntity();
            entity.appPackage = packageName;
            entity.appName = appName;
            entity.title = title;
            entity.text = text;
            entity.topic = ""; // Could be extracted from notification category
            entity.receivedAt = sbn.getPostTime();
            entity.action = result.action;
            entity.confidence = result.confidence;
            entity.isVip = result.isVip;
            entity.synced = false;
            entity.dismissed = false;

            // Save to database
            Logger.d(TAG, "Saving notification to database...");
            long id = database.notificationDao().insert(entity);
            Logger.db(TAG, "INSERT notification", "ID: " + id + ", Action: " + result.action);

            // Cancel notification based on action
            if (result.action.equals(NotificationClassifier.ACTION_NEVER)) {
                Logger.i(TAG, "Canceling NEVER notification");
                cancelNotification(sbn.getKey());
                Logger.notification(TAG, packageName, "Cancelled (NEVER)");
            } else if (result.action.equals(NotificationClassifier.ACTION_LATER)) {
                Logger.i(TAG, "Deferring LATER notification");
                cancelNotification(sbn.getKey());
                Logger.notification(TAG, packageName, "Deferred (LATER)");
                // Schedule for later delivery (could use WorkManager here)
            } else {
                Logger.i(TAG, "Keeping NOW notification visible");
                Logger.notification(TAG, packageName, "Showing (NOW)");
            }

            // Broadcast update to UI
            Logger.d(TAG, "Broadcasting UI update");
            broadcastNotificationUpdate();

            Logger.i(TAG, "Successfully processed notification ID: " + id);

        } catch (Exception e) {
            Logger.e(TAG, "Error processing notification", e);
        } finally {
            timer.stop();
            Logger.exit(TAG, "processNotification");
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Logger.notification(TAG, sbn.getPackageName(), "Removed");
        Logger.d(TAG, "Notification removed from system: " + sbn.getPackageName());
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            String appName = pm.getApplicationLabel(appInfo).toString();
            Logger.d(TAG, "Resolved app name: " + packageName + " -> " + appName);
            return appName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.w(TAG, "App name not found for: " + packageName);
            return packageName;
        }
    }

    private String getTitle(StatusBarNotification sbn) {
        try {
            return sbn.getNotification().extras.getString(Notification.EXTRA_TITLE, "");
        } catch (Exception e) {
            return "";
        }
    }

    private String getText(StatusBarNotification sbn) {
        try {
            CharSequence text = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
            return text != null ? text.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void broadcastNotificationUpdate() {
        Intent intent = new Intent("com.quietinbox.NOTIFICATION_UPDATE");
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        Logger.lifecycle(TAG, "Service destroying");
        super.onDestroy();

        if (executorService != null) {
            Logger.d(TAG, "Shutting down executor service");
            executorService.shutdown();
        }

        Logger.i(TAG, "NotificationListenerService destroyed");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Logger.lifecycle(TAG, "Listener connected");
        Logger.i(TAG, "NotificationListenerService is now active and listening");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Logger.lifecycle(TAG, "Listener disconnected");
        Logger.w(TAG, "NotificationListenerService disconnected - notifications will not be captured");
    }
}
