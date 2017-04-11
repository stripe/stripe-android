package com.stripe.samplestore;

import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class StoreActivity extends AppCompatActivity {

    static final int PURCHASE_REQUEST = 37;

    private static final String EXTRA_EMOJI_PURCHASED = "EXTRA_EMOJI_PURCHASED";
    private static final String EXTRA_PRICE_PAID = "EXTRA_PRICE_PAID";

    public static Intent createPurchaseCompleteIntent(int emojiUnicode, long amount) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_EMOJI_PURCHASED, emojiUnicode);
        returnIntent.putExtra(EXTRA_PRICE_PAID, amount);
        return returnIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store);

        StoreAdapter adapter = new StoreAdapter(this);
        ItemDivider dividerDecoration = new ItemDivider(this, R.drawable.item_divider);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv_store_items);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(dividerDecoration);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PURCHASE_REQUEST && resultCode == RESULT_OK) {
            int emojiUnicode = data.getExtras().getInt(EXTRA_EMOJI_PURCHASED, -1);
            long price = data.getExtras().getLong(EXTRA_PRICE_PAID, -1L);
            if (emojiUnicode != -1 && price != -1L) {
                displayPurchase(emojiUnicode, price);
            }
        }
    }

    private void displayPurchase(int emojiUnicode, long price) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.purchase_complete_notification, null);

        TextView emojiView = (TextView) dialogView.findViewById(R.id.dlg_emoji_display);
        emojiView.setText(StoreUtils.getEmojiByUnicode(emojiUnicode));
        TextView priceView = (TextView) dialogView.findViewById(R.id.dlg_price_display);
        priceView.setText(StoreUtils.getPriceString(price, null));

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
