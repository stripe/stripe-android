package com.stripe.samplestore;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.stripe.wrap.pay.AndroidPayConfiguration;

public class StoreActivity
        extends AppCompatActivity
        implements StoreAdapter.TotalItemsChangedListener{

    // Put your publishable key here. It should start with "pk_test_"
    private static final String PUBLISHABLE_KEY =
            "pk_test_9UVLd6CCQln8IhUSsmRyqQu4";

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

        initAndroidPay();

        mGoToCartButton = (FloatingActionButton) findViewById(R.id.fab_checkout);
        mStoreAdapter = new StoreAdapter(this);
        ItemDivider dividerDecoration = new ItemDivider(this, R.drawable.item_divider);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_store_items);


        mGoToCartButton.hide();
        Toolbar myToolBar = (Toolbar) findViewById(R.id.my_toolbar);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PURCHASE_REQUEST && resultCode == RESULT_OK) {
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

    private void initAndroidPay() {
        AndroidPayConfiguration payConfiguration = AndroidPayConfiguration.init("USD");
        payConfiguration.setPhoneNumberRequired(false);
        payConfiguration.setShippingAddressRequired(true);
        payConfiguration.setPublicApiKey(PUBLISHABLE_KEY);
    }

    private void displayPurchase(long price) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.purchase_complete_notification, null);

        TextView emojiView = (TextView) dialogView.findViewById(R.id.dlg_emoji_display);
        // Show a smiley face!
        emojiView.setText(StoreUtils.getEmojiByUnicode(0x1F642));
        TextView priceView = (TextView) dialogView.findViewById(R.id.dlg_price_display);
        priceView.setText(StoreUtils.getPriceString(price, null));

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
