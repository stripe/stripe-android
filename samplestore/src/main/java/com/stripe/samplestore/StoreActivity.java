package com.stripe.samplestore;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.samplestore.service.SampleStoreEphemeralKeyProvider;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    public static Intent createPurchaseCompleteIntent(long amount) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_PRICE_PAID, amount);
        return returnIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        PaymentConfiguration.init(PUBLISHABLE_KEY);
        mGoToCartButton = findViewById(R.id.fab_checkout);
        mStoreAdapter = new StoreAdapter(this);
        ItemDivider dividerDecoration = new ItemDivider(this, R.drawable.item_divider);
        RecyclerView recyclerView = findViewById(R.id.rv_store_items);

        mGoToCartButton.hide();
        Toolbar myToolBar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolBar);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(dividerDecoration);
        recyclerView.setAdapter(mStoreAdapter);

        mGoToCartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStoreAdapter.launchPurchaseActivityWithCart();
            }
        });
        setupCustomerSession();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.purchase_complete_notification, null);

        TextView emojiView = dialogView.findViewById(R.id.dlg_emoji_display);
        // Show a smiley face!
        emojiView.setText(StoreUtils.getEmojiByUnicode(0x1F642));
        TextView priceView = dialogView.findViewById(R.id.dlg_price_display);
        priceView.setText(StoreUtils.getPriceString(price, null));

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setupCustomerSession() {
        // CustomerSession only needs to be initialized once per app.
        CustomerSession.initCustomerSession(
                new SampleStoreEphemeralKeyProvider(
                        new SampleStoreEphemeralKeyProvider.ProgressListener() {
                            @Override
                            public void onStringResponse(String string) {
                                if (string.startsWith("Error: ")) {
                                    new AlertDialog.Builder(StoreActivity.this).setMessage(string).show();
                                }
                            }
                        }));
    }
}
