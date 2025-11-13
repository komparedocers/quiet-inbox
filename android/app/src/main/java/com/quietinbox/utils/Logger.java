package com.quietinbox.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Comprehensive logging utility for QuietInbox
 * Provides structured logging with levels, timestamps, and error tracking
 */
public class Logger {
    private static final String APP_TAG = "QuietInbox";
    private static boolean loggingEnabled = true;

    // Log levels
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;

    /**
     * Enable or disable logging globally
     */
    public static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    /**
     * Log verbose message
     */
    public static void v(String tag, String message) {
        if (loggingEnabled) {
            Log.v(APP_TAG, formatMessage(tag, message));
        }
    }

    /**
     * Log debug message
     */
    public static void d(String tag, String message) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, message));
        }
    }

    /**
     * Log info message
     */
    public static void i(String tag, String message) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, message));
        }
    }

    /**
     * Log warning message
     */
    public static void w(String tag, String message) {
        if (loggingEnabled) {
            Log.w(APP_TAG, formatMessage(tag, message));
        }
    }

    /**
     * Log warning with throwable
     */
    public static void w(String tag, String message, Throwable throwable) {
        if (loggingEnabled) {
            Log.w(APP_TAG, formatMessage(tag, message), throwable);
            logStackTrace(tag, throwable);
        }
    }

    /**
     * Log error message
     */
    public static void e(String tag, String message) {
        if (loggingEnabled) {
            Log.e(APP_TAG, formatMessage(tag, message));
        }
    }

    /**
     * Log error with throwable
     */
    public static void e(String tag, String message, Throwable throwable) {
        if (loggingEnabled) {
            Log.e(APP_TAG, formatMessage(tag, message), throwable);
            logStackTrace(tag, throwable);
        }
    }

    /**
     * Log method entry
     */
    public static void enter(String tag, String methodName) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "→ ENTER: " + methodName));
        }
    }

    /**
     * Log method exit
     */
    public static void exit(String tag, String methodName) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "← EXIT: " + methodName));
        }
    }

    /**
     * Log method exit with result
     */
    public static void exit(String tag, String methodName, Object result) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "← EXIT: " + methodName + " | Result: " + result));
        }
    }

    /**
     * Log API call
     */
    public static void api(String tag, String endpoint, String method) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "API: " + method + " " + endpoint));
        }
    }

    /**
     * Log API response
     */
    public static void apiResponse(String tag, String endpoint, int statusCode, String response) {
        if (loggingEnabled) {
            String truncatedResponse = response != null && response.length() > 200
                ? response.substring(0, 200) + "..."
                : response;
            Log.i(APP_TAG, formatMessage(tag, "API Response: " + endpoint +
                " | Status: " + statusCode + " | Body: " + truncatedResponse));
        }
    }

    /**
     * Log database operation
     */
    public static void db(String tag, String operation, String details) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "DB: " + operation + " | " + details));
        }
    }

    /**
     * Log sync operation
     */
    public static void sync(String tag, String operation, int count) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "SYNC: " + operation + " | Count: " + count));
        }
    }

    /**
     * Log notification event
     */
    public static void notification(String tag, String app, String action) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "NOTIFICATION: " + app + " | Action: " + action));
        }
    }

    /**
     * Log user action
     */
    public static void userAction(String tag, String action, String details) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "USER ACTION: " + action + " | " + details));
        }
    }

    /**
     * Log ad event
     */
    public static void ad(String tag, String adType, String event) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "AD: " + adType + " | " + event));
        }
    }

    /**
     * Log billing event
     */
    public static void billing(String tag, String event, String details) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "BILLING: " + event + " | " + details));
        }
    }

    /**
     * Log performance metric
     */
    public static void performance(String tag, String operation, long durationMs) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "PERFORMANCE: " + operation +
                " | Duration: " + durationMs + "ms"));
        }
    }

    /**
     * Log network status
     */
    public static void network(String tag, boolean isAvailable) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "NETWORK: " +
                (isAvailable ? "Available" : "Unavailable")));
        }
    }

    /**
     * Log JSON object (useful for debugging)
     */
    public static void json(String tag, String label, JSONObject json) {
        if (loggingEnabled && json != null) {
            try {
                String formatted = json.toString(2);
                Log.d(APP_TAG, formatMessage(tag, label + ":\n" + formatted));
            } catch (JSONException e) {
                Log.e(APP_TAG, formatMessage(tag, "Error formatting JSON: " + e.getMessage()));
            }
        }
    }

    /**
     * Format message with timestamp and tag
     */
    private static String formatMessage(String tag, String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        String timestamp = sdf.format(new Date());
        return "[" + timestamp + "][" + tag + "] " + message;
    }

    /**
     * Log full stack trace
     */
    private static void logStackTrace(String tag, Throwable throwable) {
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            Log.e(APP_TAG, formatMessage(tag, "Stack Trace:\n" + sw.toString()));
        }
    }

    /**
     * Create a section separator in logs for better readability
     */
    public static void separator(String tag, String title) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "========== " + title + " =========="));
        }
    }

    /**
     * Log application lifecycle event
     */
    public static void lifecycle(String tag, String event) {
        if (loggingEnabled) {
            Log.i(APP_TAG, formatMessage(tag, "LIFECYCLE: " + event));
        }
    }

    /**
     * Log configuration change
     */
    public static void config(String tag, String key, String value) {
        if (loggingEnabled) {
            Log.d(APP_TAG, formatMessage(tag, "CONFIG: " + key + " = " + value));
        }
    }

    /**
     * Helper method to log method execution time
     */
    public static class Timer {
        private final String tag;
        private final String operation;
        private final long startTime;

        public Timer(String tag, String operation) {
            this.tag = tag;
            this.operation = operation;
            this.startTime = System.currentTimeMillis();
            Logger.d(tag, "⏱ START: " + operation);
        }

        public void stop() {
            long duration = System.currentTimeMillis() - startTime;
            Logger.performance(tag, operation, duration);
        }
    }
}
