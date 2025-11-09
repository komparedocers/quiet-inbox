package com.quietinbox.services;

import android.app.Notification;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.quietinbox.database.AppDatabase;
import com.quietinbox.database.NotificationEntity;

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
        Log.d(TAG, "NotificationListenerService created");

        database = AppDatabase.getInstance(this);
        classifier = new NotificationClassifier(this);
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            // Ignore our own notifications
            if (sbn.getPackageName().equals(getPackageName())) {
                return;
            }

            // Ignore ongoing notifications (media playback, etc.)
            if ((sbn.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != 0) {
                return;
            }

            Log.d(TAG, "Notification received from: " + sbn.getPackageName());

            // Process in background
            executorService.execute(() -> processNotification(sbn));

        } catch (Exception e) {
            Log.e(TAG, "Error in onNotificationPosted", e);
        }
    }

    private void processNotification(StatusBarNotification sbn) {
        try {
            // Classify the notification
            NotificationClassifier.ClassificationResult result = classifier.classify(sbn);

            // Get app name
            String appName = getAppName(sbn.getPackageName());

            // Extract notification details
            String title = getTitle(sbn);
            String text = getText(sbn);

            // Create entity
            NotificationEntity entity = new NotificationEntity();
            entity.appPackage = sbn.getPackageName();
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
            long id = database.notificationDao().insert(entity);

            Log.d(TAG, "Notification classified as " + result.action +
                    " (confidence: " + result.confidence + ") - ID: " + id);

            // Cancel notification based on action
            if (result.action.equals(NotificationClassifier.ACTION_NEVER)) {
                // Cancel spam/unwanted notifications
                cancelNotification(sbn.getKey());
            } else if (result.action.equals(NotificationClassifier.ACTION_LATER)) {
                // Cancel and save for later
                cancelNotification(sbn.getKey());
                // Schedule for later delivery (could use WorkManager here)
            }
            // If ACTION_NOW, leave the notification as-is

            // Broadcast update to UI
            broadcastNotificationUpdate();

        } catch (Exception e) {
            Log.e(TAG, "Error processing notification", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Handle notification removal if needed
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }

    private String getAppName(String packageName) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
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
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        Log.d(TAG, "NotificationListenerService destroyed");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.d(TAG, "NotificationListenerService connected");
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.d(TAG, "NotificationListenerService disconnected");
    }
}
