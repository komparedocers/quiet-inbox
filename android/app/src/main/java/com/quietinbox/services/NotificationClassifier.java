package com.quietinbox.services;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.quietinbox.database.AppDatabase;
import com.quietinbox.database.ProfileEntity;
import com.quietinbox.database.VIPEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Classifies notifications into NOW, LATER, or NEVER categories
 * Uses rule-based logic and VIP status
 */
public class NotificationClassifier {
    private static final String TAG = "NotificationClassifier";

    public static final String ACTION_NOW = "NOW";
    public static final String ACTION_LATER = "LATER";
    public static final String ACTION_NEVER = "NEVER";

    private final Context context;
    private final AppDatabase database;

    public NotificationClassifier(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
    }

    /**
     * Classify a notification
     */
    public ClassificationResult classify(StatusBarNotification sbn) {
        try {
            String packageName = sbn.getPackageName();
            String title = getTitle(sbn);
            String text = getText(sbn);

            // Check VIP status
            boolean isVip = checkVIPStatus(packageName, title, text);

            // Get active profile
            ProfileEntity profile = database.profileDao().getActiveProfile();

            // Check quiet hours
            boolean inQuietHours = profile != null && isInQuietHours(profile);

            // Apply classification rules
            String action;
            float confidence;

            if (isVip) {
                // VIP notifications always go to NOW (unless it's quiet hours and VIP doesn't bypass)
                if (inQuietHours) {
                    VIPEntity vip = findVIP(packageName, title, text);
                    if (vip != null && vip.bypassQuietHours) {
                        action = ACTION_NOW;
                        confidence = 0.95f;
                    } else {
                        action = ACTION_LATER;
                        confidence = 0.85f;
                    }
                } else {
                    action = ACTION_NOW;
                    confidence = 0.95f;
                }
            } else if (isSystemNotification(packageName)) {
                // System notifications
                action = ACTION_NEVER;
                confidence = 0.90f;
            } else if (inQuietHours) {
                // During quiet hours, defer non-VIP notifications
                action = ACTION_LATER;
                confidence = 0.80f;
            } else {
                // Default rule-based classification
                action = classifyByContent(title, text);
                confidence = 0.70f;
            }

            return new ClassificationResult(action, confidence, isVip);

        } catch (Exception e) {
            Log.e(TAG, "Classification error", e);
            // Default to LATER on error to avoid missing important notifications
            return new ClassificationResult(ACTION_LATER, 0.50f, false);
        }
    }

    private boolean checkVIPStatus(String packageName, String title, String text) {
        try {
            // Check if there's a VIP entry for this app/sender
            VIPEntity vip = findVIP(packageName, title, text);
            return vip != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking VIP status", e);
            return false;
        }
    }

    private VIPEntity findVIP(String packageName, String title, String text) {
        // Try to find VIP by package and identifier
        // Identifier could be in title or text
        String identifier = extractIdentifier(title, text);
        if (identifier != null) {
            return database.vipDao().findVIP(packageName, identifier);
        }
        return null;
    }

    private String extractIdentifier(String title, String text) {
        // Simple identifier extraction (phone numbers, emails, names)
        // In a real app, this would be more sophisticated
        if (title != null && title.length() > 0) {
            return title;
        }
        if (text != null && text.length() > 0) {
            return text.substring(0, Math.min(50, text.length()));
        }
        return null;
    }

    private boolean isInQuietHours(ProfileEntity profile) {
        try {
            if (profile.quietHoursStart == null || profile.quietHoursEnd == null) {
                return false;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Calendar now = Calendar.getInstance();
            String currentTime = sdf.format(now.getTime());

            String start = profile.quietHoursStart;
            String end = profile.quietHoursEnd;

            // Handle overnight quiet hours (e.g., 22:00 to 07:00)
            if (start.compareTo(end) > 0) {
                return currentTime.compareTo(start) >= 0 || currentTime.compareTo(end) < 0;
            } else {
                return currentTime.compareTo(start) >= 0 && currentTime.compareTo(end) < 0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking quiet hours", e);
            return false;
        }
    }

    private boolean isSystemNotification(String packageName) {
        return packageName.equals("android") ||
               packageName.equals("com.android.systemui") ||
               packageName.startsWith("com.google.android.gms");
    }

    private String classifyByContent(String title, String text) {
        if (title == null && text == null) {
            return ACTION_NEVER;
        }

        String content = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase();

        // High priority keywords
        if (content.contains("urgent") || content.contains("important") ||
            content.contains("critical") || content.contains("alert")) {
            return ACTION_NOW;
        }

        // Low priority keywords
        if (content.contains("newsletter") || content.contains("promotion") ||
            content.contains("advertisement") || content.contains("sale")) {
            return ACTION_LATER;
        }

        // Spam/noise keywords
        if (content.contains("spam") || content.contains("unsubscribe")) {
            return ACTION_NEVER;
        }

        // Default to NOW for unclassified notifications to avoid missing important ones
        return ACTION_NOW;
    }

    private String getTitle(StatusBarNotification sbn) {
        try {
            return sbn.getNotification().extras.getString("android.title", "");
        } catch (Exception e) {
            return "";
        }
    }

    private String getText(StatusBarNotification sbn) {
        try {
            CharSequence text = sbn.getNotification().extras.getCharSequence("android.text");
            return text != null ? text.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Classification result
     */
    public static class ClassificationResult {
        public String action;
        public float confidence;
        public boolean isVip;

        public ClassificationResult(String action, float confidence, boolean isVip) {
            this.action = action;
            this.confidence = confidence;
            this.isVip = isVip;
        }
    }
}
