package com.stripe.example.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.wallet.Cart;
import com.stripe.android.PaymentConfiguration;
import com.stripe.example.R;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

public class LauncherActivity extends AppCompatActivity {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://dashboard.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY =
            "put your key here";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        PaymentConfiguration.init(PUBLISHABLE_KEY);
        Button tokenButton = findViewById(R.id.btn_make_card_tokens);
        tokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });

        Button multilineButton = findViewById(R.id.btn_make_card_sources);
        multilineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LauncherActivity.this, PaymentMultilineActivity.class);
                startActivity(intent);
            }
        });

        Button sourceButton = findViewById(R.id.btn_make_sources);
        sourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, RedirectActivity.class);
                startActivity(intent);
            }
        });

        Button androidPayButton = findViewById(R.id.btn_android_pay_launch);
        androidPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSampleCartAndLaunchAndroidPayActivity();
            }
        });
    }

    private void createSampleCartAndLaunchAndroidPayActivity() {
        AndroidPayConfiguration.init(PUBLISHABLE_KEY, "USD");
        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.getInstance();
        androidPayConfiguration.setShippingAddressRequired(true);
        CartManager cartManager = new CartManager("USD");
        cartManager.addLineItem("Llama Food", 5000L);
        cartManager.addLineItem("Llama Shoes", 4, 2000L);
        cartManager.addShippingLineItem("Domestic shipping estimate", 1000L);

        try {
            Cart cart = cartManager.buildCart();
            Intent intent = new Intent(this, AndroidPayActivity.class)
                    .putExtra(StripeAndroidPayActivity.EXTRA_CART, cart);
            startActivity(intent);
        } catch (CartContentException unexpected) {
            // Ignore for now.
        }
    }
}
