package com.stripe.samplestore;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.samplestore.service.SampleStoreEphemeralKeyProvider;

public class StoreActivity
        extends AppCompatActivity
        implements StoreAdapter.TotalItemsChangedListener{

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://dashboard.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY =
            "put your publishable key here";

    static final int PURCHASE_REQUEST = 37;

    private static final String EXTRA_PRICE_PAID = "EXTRA_PRICE_PAID";

    private FloatingActionButton mGoToCartButton;
    private StoreAdapter mStoreAdapter;
    private SampleStoreEphemeralKeyProvider mEphemeralKeyProvider;

    @NonNull
    public static Intent createPurchaseCompleteIntent(long amount) {
        return new Intent()
                .putExtra(EXTRA_PRICE_PAID, amount);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        PaymentConfiguration.init(PUBLISHABLE_KEY);
        mGoToCartButton = findViewById(R.id.fab_checkout);
        mStoreAdapter = new StoreAdapter(this);

        mGoToCartButton.hide();
        setSupportActionBar(findViewById(R.id.my_toolbar));

        final RecyclerView recyclerView = findViewById(R.id.rv_store_items);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new ItemDivider(this, R.drawable.item_divider));
        recyclerView.setAdapter(mStoreAdapter);

        mGoToCartButton.setOnClickListener(v -> mStoreAdapter.launchPurchaseActivityWithCart());
        setupCustomerSession();
    }

    @Override
    protected void onDestroy() {
        mEphemeralKeyProvider.destroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PURCHASE_REQUEST
                && resultCode == RESULT_OK
                && data.getExtras() != null) {
            long price = data.getExtras().getLong(EXTRA_PRICE_PAID, -1L);
            if (price != -1L) {
                displayPurchase(price);
            }
            mStoreAdapter.clearItemSelections();
        }
    }

    @Override
    public void onTotalItemsChanged(int totalItems) {
        if (totalItems > 0) {
            mGoToCartButton.show();
        } else {
            mGoToCartButton.hide();
        }
    }

    private void displayPurchase(long price) {
        final View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.purchase_complete_notification, null);

        final TextView emojiView = dialogView.findViewById(R.id.dlg_emoji_display);
        // Show a smiley face!
        emojiView.setText(StoreUtils.getEmojiByUnicode(0x1F642));

        final TextView priceView = dialogView.findViewById(R.id.dlg_price_display);
        priceView.setText(StoreUtils.getPriceString(price, null));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .create()
                .show();
    }

    private void setupCustomerSession() {
        // CustomerSession only needs to be initialized once per app.
        mEphemeralKeyProvider = new SampleStoreEphemeralKeyProvider(
                string -> {
                    if (string.startsWith("Error: ")) {
                        new AlertDialog.Builder(StoreActivity.this)
                                .setMessage(string)
                                .show();
                    }
                });
        CustomerSession.initCustomerSession(this, mEphemeralKeyProvider);
    }
}
