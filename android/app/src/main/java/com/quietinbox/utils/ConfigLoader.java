package com.quietinbox.utils;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration from config.properties file
 */
public class ConfigLoader {
    private static final String TAG = "ConfigLoader";
    private static final String CONFIG_FILE = "config.properties";
    private static ConfigLoader instance;
    private Properties properties;

    private ConfigLoader(Context context) {
        properties = new Properties();
        loadConfig(context);
    }

    public static synchronized ConfigLoader getInstance(Context context) {
        if (instance == null) {
            instance = new ConfigLoader(context.getApplicationContext());
        }
        return instance;
    }

    private void loadConfig(Context context) {
        try {
            InputStream inputStream = context.getAssets().open(CONFIG_FILE);
            properties.load(inputStream);
            inputStream.close();
            Log.d(TAG, "Configuration loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Error loading configuration", e);
            loadDefaults();
        }
    }

    private void loadDefaults() {
        // Load default values if config file is not found
        properties.setProperty("admob.app.id", "ca-app-pub-3940256099942544~3347511713");
        properties.setProperty("admob.banner.id", "ca-app-pub-3940256099942544/6300978111");
        properties.setProperty("admob.interstitial.id", "ca-app-pub-3940256099942544/1033173712");
        properties.setProperty("admob.banner.enabled", "true");
        properties.setProperty("admob.interstitial.enabled", "true");
        properties.setProperty("admob.interstitial.frequency", "5");
        properties.setProperty("admob.interstitial.min_interval_seconds", "180");
        properties.setProperty("backend.url", "https://api.quietinbox.com");
        properties.setProperty("backend.timeout.connect", "10");
        properties.setProperty("backend.timeout.read", "30");
        properties.setProperty("app.offline.mode.enabled", "true");
    }

    public String getString(String key) {
        return properties.getProperty(key, "");
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            String value = properties.getProperty(key);
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    // AdMob getters
    public String getAdMobAppId() {
        return getString("admob.app.id");
    }

    public String getAdMobBannerId() {
        return getString("admob.banner.id");
    }

    public String getAdMobInterstitialId() {
        return getString("admob.interstitial.id");
    }

    public boolean isBannerAdEnabled() {
        return getBoolean("admob.banner.enabled", true);
    }

    public boolean isInterstitialAdEnabled() {
        return getBoolean("admob.interstitial.enabled", true);
    }

    public int getInterstitialFrequency() {
        return getInt("admob.interstitial.frequency", 5);
    }

    public int getInterstitialMinInterval() {
        return getInt("admob.interstitial.min_interval_seconds", 180);
    }

    // Backend getters
    public String getBackendUrl() {
        return getString("backend.url", "https://api.quietinbox.com");
    }

    public int getConnectTimeout() {
        return getInt("backend.timeout.connect", 10);
    }

    public int getReadTimeout() {
        return getInt("backend.timeout.read", 30);
    }

    public boolean isOfflineModeEnabled() {
        return getBoolean("app.offline.mode.enabled", true);
    }
}
