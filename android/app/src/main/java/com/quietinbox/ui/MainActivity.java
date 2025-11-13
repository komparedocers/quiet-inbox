package com.quietinbox.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.quietinbox.R;
import com.quietinbox.database.AppDatabase;
import com.quietinbox.database.NotificationEntity;
import com.quietinbox.services.NotificationClassifier;
import com.quietinbox.services.SyncManager;
import com.quietinbox.utils.AdManager;
import com.quietinbox.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main activity showing notification feed with NOW/LATER tabs
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private NotificationAdapter adapter;
    private TabLayout tabLayout;
    private FrameLayout adContainer;

    private AppDatabase database;
    private AdManager adManager;
    private SyncManager syncManager;
    private ExecutorService executorService;

    private String currentFilter = NotificationClassifier.ACTION_NOW;

    private BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(TAG, "Notification update broadcast received");
            loadNotifications();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.lifecycle(TAG, "Activity created");
        Logger.enter(TAG, "onCreate");

        setContentView(R.layout.activity_main);

        try {
            Logger.d(TAG, "Initializing views...");
            initializeViews();

            Logger.d(TAG, "Initializing services...");
            initializeServices();

            Logger.d(TAG, "Checking notification access...");
            checkNotificationAccess();

            Logger.d(TAG, "Loading notifications...");
            loadNotifications();

            Logger.d(TAG, "Loading banner ad...");
            loadBannerAd();

            // Track screen view for ad display
            adManager.trackScreenView();

            Logger.i(TAG, "MainActivity initialized successfully");

        } catch (Exception e) {
            Logger.e(TAG, "Error in onCreate - activity initialization failed", e);
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show();
        }

        Logger.exit(TAG, "onCreate");
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new NotificationAdapter(new ArrayList<>(), this::onNotificationClick);
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::refreshData);

        tabLayout = findViewById(R.id.tabLayout);
        tabLayout.addTab(tabLayout.newTab().setText("Now"));
        tabLayout.addTab(tabLayout.newTab().setText("Later"));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentFilter = tab.getPosition() == 0 ?
                    NotificationClassifier.ACTION_NOW :
                    NotificationClassifier.ACTION_LATER;
                loadNotifications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showUpgradeDialog());

        adContainer = findViewById(R.id.adContainer);
    }

    private void initializeServices() {
        database = AppDatabase.getInstance(this);
        adManager = AdManager.getInstance(this);
        syncManager = SyncManager.getInstance(this);
        executorService = Executors.newSingleThreadExecutor();
    }

    private void checkNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            new AlertDialog.Builder(this)
                .setTitle("Notification Access Required")
                .setMessage("QuietInbox needs notification access to manage your notifications. Please enable it in settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    startActivity(new Intent(NOTIFICATION_LISTENER_SETTINGS));
                })
                .setNegativeButton("Later", null)
                .show();
        }
    }

    private boolean isNotificationServiceEnabled() {
        String packageName = getPackageName();
        String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(packageName);
    }

    private void loadNotifications() {
        Logger.enter(TAG, "loadNotifications");
        Logger.d(TAG, "Loading notifications for filter: " + currentFilter);

        LiveData<List<NotificationEntity>> liveData =
            database.notificationDao().getNotificationsByAction(currentFilter);

        liveData.observe(this, notifications -> {
            if (notifications != null) {
                Logger.i(TAG, "Loaded " + notifications.size() + " notifications");
                adapter.updateData(notifications);
                swipeRefresh.setRefreshing(false);
            }
        });

        Logger.exit(TAG, "loadNotifications");
    }

    private void refreshData() {
        Logger.userAction(TAG, "Refresh", "Manual sync triggered");

        // Sync with server
        syncManager.syncAll(new SyncManager.SyncCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Logger.i(TAG, "Sync completed successfully");
                    Toast.makeText(MainActivity.this, "Sync completed", Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Logger.e(TAG, "Sync failed: " + error);
                    Toast.makeText(MainActivity.this, "Sync failed: " + error, Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }

    private void loadBannerAd() {
        try {
            adManager.loadBannerAd(this, adContainer);
        } catch (Exception e) {
            Log.e(TAG, "Error loading banner ad", e);
        }
    }

    private void onNotificationClick(NotificationEntity notification) {
        // Show notification details
        new AlertDialog.Builder(this)
            .setTitle(notification.appName)
            .setMessage(notification.title + "\n\n" + notification.text)
            .setPositiveButton("Dismiss", (dialog, which) -> dismissNotification(notification))
            .setNegativeButton("Close", null)
            .show();
    }

    private void dismissNotification(NotificationEntity notification) {
        executorService.execute(() -> {
            database.notificationDao().markAsDismissed(notification.id);
            runOnUiThread(() -> Toast.makeText(this, "Dismissed", Toast.LENGTH_SHORT).show());
        });
    }

    private void showUpgradeDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Upgrade to Pro")
            .setMessage("Unlock premium features:\n\n" +
                "• Multiple Profiles\n" +
                "• Calendar Integration\n" +
                "• Cloud Sync & Backup\n" +
                "• Advanced Analytics\n" +
                "• Ad-Free Experience")
            .setPositiveButton("Upgrade", (dialog, which) -> {
                Intent intent = new Intent(this, ProUpgradeActivity.class);
                startActivity(intent);
            })
            .setNegativeButton("Not Now", null)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_profiles) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_vip) {
            startActivity(new Intent(this, VIPActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_sync) {
            swipeRefresh.setRefreshing(true);
            refreshData();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Logger.lifecycle(TAG, "Activity resumed");

        registerReceiver(notificationReceiver, new IntentFilter("com.quietinbox.NOTIFICATION_UPDATE"));
        Logger.d(TAG, "Notification receiver registered");

        // Show interstitial ad with smart frequency
        adManager.showInterstitialAd(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.lifecycle(TAG, "Activity paused");

        try {
            unregisterReceiver(notificationReceiver);
            Logger.d(TAG, "Notification receiver unregistered");
        } catch (Exception e) {
            Logger.w(TAG, "Receiver was not registered", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Logger.lifecycle(TAG, "Activity destroyed");

        if (executorService != null) {
            executorService.shutdown();
            Logger.d(TAG, "Executor service shut down");
        }
    }
}
