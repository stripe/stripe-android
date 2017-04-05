package com.stripe.samplestore;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class PaymentActivity extends AppCompatActivity {

    private static final String EXTRA_EMOJI_INT = "EXTRA_EMOJI_INT";
    private static final String EXTRA_PRICE = "EXTRA_PRICE";

    public static Intent createIntent(
            @NonNull Context context,
            int emojiUnicode,
            int price) {
        Intent intent = new Intent(context, PaymentActivity.class);
        intent.putExtra(EXTRA_EMOJI_INT, emojiUnicode);
        intent.putExtra(EXTRA_PRICE, price);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        int emojiUnicode = getIntent().getExtras().getInt(EXTRA_EMOJI_INT);
        TextView emojiDisplay = (TextView) findViewById(R.id.tv_emoji_display);
        emojiDisplay.setText(new String(Character.toChars(emojiUnicode)));

    }
}
