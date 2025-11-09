package com.quietinbox.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.billingclient.api.ProductDetails;
import com.quietinbox.R;
import com.quietinbox.utils.BillingManager;

import java.util.List;

/**
 * Activity for upgrading to Pro subscription
 */
public class ProUpgradeActivity extends AppCompatActivity {
    private static final String TAG = "ProUpgradeActivity";

    private BillingManager billingManager;
    private ProgressBar progressBar;
    private Button monthlyButton;
    private Button yearlyButton;
    private Button lifetimeButton;
    private TextView priceMonthlyText;
    private TextView priceYearlyText;
    private TextView priceLifetimeText;

    private ProductDetails monthlyProduct;
    private ProductDetails yearlyProduct;
    private ProductDetails lifetimeProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pro_upgrade);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initializeViews();
        loadProducts();
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        monthlyButton = findViewById(R.id.monthlyButton);
        yearlyButton = findViewById(R.id.yearlyButton);
        lifetimeButton = findViewById(R.id.lifetimeButton);
        priceMonthlyText = findViewById(R.id.priceMonthlyText);
        priceYearlyText = findViewById(R.id.priceYearlyText);
        priceLifetimeText = findViewById(R.id.priceLifetimeText);

        monthlyButton.setOnClickListener(v -> purchaseProduct(monthlyProduct));
        yearlyButton.setOnClickListener(v -> purchaseProduct(yearlyProduct));
        lifetimeButton.setOnClickListener(v -> purchaseProduct(lifetimeProduct));

        billingManager = BillingManager.getInstance(this);
    }

    private void loadProducts() {
        progressBar.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);

        billingManager.queryProProducts(this, new BillingManager.ProductCallback() {
            @Override
            public void onSuccess(List<ProductDetails> products) {
                runOnUiThread(() -> {
                    for (ProductDetails product : products) {
                        String productId = product.getProductId();

                        if ("quietinbox_pro_monthly".equals(productId)) {
                            monthlyProduct = product;
                            updatePriceText(priceMonthlyText, product);
                        } else if ("quietinbox_pro_yearly".equals(productId)) {
                            yearlyProduct = product;
                            updatePriceText(priceYearlyText, product);
                        } else if ("quietinbox_pro_lifetime".equals(productId)) {
                            lifetimeProduct = product;
                            updatePriceText(priceLifetimeText, product);
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    setButtonsEnabled(true);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProUpgradeActivity.this,
                        "Error loading products: " + error,
                        Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error loading products: " + error);
                });
            }
        });
    }

    private void updatePriceText(TextView textView, ProductDetails product) {
        String price = "Not available";

        if (product.getProductType().equals("subs") &&
            product.getSubscriptionOfferDetails() != null &&
            !product.getSubscriptionOfferDetails().isEmpty()) {

            ProductDetails.SubscriptionOfferDetails offer =
                product.getSubscriptionOfferDetails().get(0);
            if (!offer.getPricingPhases().getPricingPhaseList().isEmpty()) {
                price = offer.getPricingPhases().getPricingPhaseList().get(0)
                    .getFormattedPrice();
            }
        } else if (product.getOneTimePurchaseOfferDetails() != null) {
            price = product.getOneTimePurchaseOfferDetails().getFormattedPrice();
        }

        textView.setText(price);
    }

    private void purchaseProduct(ProductDetails product) {
        if (product == null) {
            Toast.makeText(this, "Product not available", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        setButtonsEnabled(false);

        billingManager.purchasePro(this, product, new BillingManager.PurchaseCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(ProUpgradeActivity.this,
                        "Welcome to Pro!",
                        Toast.LENGTH_LONG).show();
                    finish();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setButtonsEnabled(true);
                    Toast.makeText(ProUpgradeActivity.this,
                        "Purchase failed: " + error,
                        Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        monthlyButton.setEnabled(enabled);
        yearlyButton.setEnabled(enabled);
        lifetimeButton.setEnabled(enabled);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
