package com.quietinbox.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.*;
import com.quietinbox.database.AppDatabase;
import com.quietinbox.database.UserEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages Google Play Billing for in-app purchases
 * Handles Pro subscription purchases
 */
public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private static BillingManager instance;

    private final Context context;
    private final AppDatabase database;
    private BillingClient billingClient;
    private boolean billingReady = false;
    private PurchaseCallback purchaseCallback;

    private BillingManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);

        setupBillingClient();
    }

    public static synchronized BillingManager getInstance(Context context) {
        if (instance == null) {
            instance = new BillingManager(context);
        }
        return instance;
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    billingReady = true;
                    Log.d(TAG, "Billing client ready");

                    // Check for existing purchases
                    queryPurchases();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                billingReady = false;
                Log.w(TAG, "Billing service disconnected");
            }
        });
    }

    /**
     * Query available Pro subscription products
     */
    public void queryProProducts(Activity activity, ProductCallback callback) {
        if (!billingReady) {
            Log.e(TAG, "Billing client not ready");
            if (callback != null) callback.onError("Billing not ready");
            return;
        }

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("quietinbox_pro_monthly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("quietinbox_pro_yearly")
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("quietinbox_pro_lifetime")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        );

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                if (callback != null) callback.onSuccess(productDetailsList);
            } else {
                if (callback != null) callback.onError(billingResult.getDebugMessage());
            }
        });
    }

    /**
     * Purchase Pro subscription
     */
    public void purchasePro(Activity activity, ProductDetails productDetails, PurchaseCallback callback) {
        if (!billingReady) {
            Log.e(TAG, "Billing client not ready");
            if (callback != null) callback.onError("Billing not ready");
            return;
        }

        this.purchaseCallback = callback;

        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();

        if (productDetails.getProductType().equals(BillingClient.ProductType.SUBS)) {
            // Subscription
            ProductDetails.SubscriptionOfferDetails offerDetails =
                productDetails.getSubscriptionOfferDetails().get(0);

            productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerDetails.getOfferToken())
                    .build()
            );
        } else {
            // One-time purchase
            productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            );
        }

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build();

        billingClient.launchBillingFlow(activity, billingFlowParams);
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase canceled by user");
            if (purchaseCallback != null) purchaseCallback.onError("Purchase canceled");
        } else {
            Log.e(TAG, "Purchase error: " + billingResult.getDebugMessage());
            if (purchaseCallback != null) purchaseCallback.onError(billingResult.getDebugMessage());
        }
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            }

            // Grant Pro access
            grantProAccess();

            Log.d(TAG, "Purchase successful");
            if (purchaseCallback != null) purchaseCallback.onSuccess();
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();

        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged");
            }
        });
    }

    private void grantProAccess() {
        new Thread(() -> {
            try {
                UserEntity user = database.userDao().getUser();
                if (user != null) {
                    database.userDao().updateProStatus(true);
                    Log.d(TAG, "Pro access granted locally");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error granting Pro access", e);
            }
        }).start();
    }

    /**
     * Check for existing purchases
     */
    private void queryPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (purchases != null && !purchases.isEmpty()) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                grantProAccess();
                            }
                        }
                    }
                }
            }
        );

        // Also check one-time purchases
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            (billingResult, purchases) -> {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (purchases != null && !purchases.isEmpty()) {
                        for (Purchase purchase : purchases) {
                            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                grantProAccess();
                            }
                        }
                    }
                }
            }
        );
    }

    /**
     * Callback interfaces
     */
    public interface ProductCallback {
        void onSuccess(List<ProductDetails> products);
        void onError(String error);
    }

    public interface PurchaseCallback {
        void onSuccess();
        void onError(String error);
    }
}
