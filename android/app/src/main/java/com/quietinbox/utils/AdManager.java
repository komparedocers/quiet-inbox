package com.quietinbox.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Manages AdMob banner and interstitial ads
 * Implements non-intrusive ad strategy for revenue generation
 */
public class AdManager {
    private static final String TAG = "AdManager";
    private static AdManager instance;

    private final ConfigLoader config;
    private InterstitialAd interstitialAd;
    private long lastInterstitialTime = 0;
    private int screenViewCount = 0;

    private AdManager(Context context) {
        this.config = ConfigLoader.getInstance(context);

        // Initialize Mobile Ads SDK
        MobileAds.initialize(context, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK initialized");
        });
    }

    public static synchronized AdManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Load and display banner ad in the provided container
     */
    public void loadBannerAd(Activity activity, FrameLayout adContainer) {
        if (!config.isBannerAdEnabled()) {
            Log.d(TAG, "Banner ads disabled");
            adContainer.setVisibility(View.GONE);
            return;
        }

        try {
            AdView adView = new AdView(activity);
            adView.setAdUnitId(config.getAdMobBannerId());
            adView.setAdSize(AdSize.BANNER);

            adContainer.removeAllViews();
            adContainer.addView(adView);
            adContainer.setVisibility(View.VISIBLE);

            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);

            Log.d(TAG, "Banner ad loaded");
        } catch (Exception e) {
            Log.e(TAG, "Error loading banner ad", e);
            adContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Preload interstitial ad for later display
     */
    public void preloadInterstitialAd(Context context) {
        if (!config.isInterstitialAdEnabled()) {
            Log.d(TAG, "Interstitial ads disabled");
            return;
        }

        try {
            AdRequest adRequest = new AdRequest.Builder().build();

            InterstitialAd.load(
                context,
                config.getAdMobInterstitialId(),
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d(TAG, "Interstitial ad loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        interstitialAd = null;
                    }
                }
            );
        } catch (Exception e) {
            Log.e(TAG, "Error preloading interstitial ad", e);
        }
    }

    /**
     * Show interstitial ad if conditions are met
     * Implements smart frequency capping to avoid annoying users
     */
    public void showInterstitialAd(Activity activity) {
        if (!config.isInterstitialAdEnabled()) {
            return;
        }

        try {
            screenViewCount++;

            // Check frequency
            int frequency = config.getInterstitialFrequency();
            if (screenViewCount < frequency) {
                Log.d(TAG, "Interstitial frequency not met: " + screenViewCount + "/" + frequency);
                return;
            }

            // Check time interval
            long currentTime = System.currentTimeMillis();
            long minInterval = config.getInterstitialMinInterval() * 1000L;

            if (currentTime - lastInterstitialTime < minInterval) {
                Log.d(TAG, "Interstitial time interval not met");
                return;
            }

            // Show ad
            if (interstitialAd != null) {
                interstitialAd.show(activity);
                lastInterstitialTime = currentTime;
                screenViewCount = 0;

                // Preload next ad
                preloadInterstitialAd(activity);

                Log.d(TAG, "Interstitial ad shown");
            } else {
                Log.d(TAG, "Interstitial ad not ready");
                // Try to load for next time
                preloadInterstitialAd(activity);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing interstitial ad", e);
        }
    }

    /**
     * Increment screen view count (call this from each activity)
     */
    public void trackScreenView() {
        screenViewCount++;
    }
}
