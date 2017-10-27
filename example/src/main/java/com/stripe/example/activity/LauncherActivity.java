package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.stripe.android.PaymentConfiguration;
import com.stripe.example.R;

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

        Button customerSessionButton = findViewById(R.id.btn_customer_session_launch);
        customerSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LauncherActivity.this, CustomerSessionActivity.class);
                startActivity(intent);
            }
        });

        Button selectShippingAddressButton = findViewById(R.id.btn_payment_session_launch);
        selectShippingAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LauncherActivity.this, PaymentSessionActivity.class);
                startActivity(intent);
            }
        });

        Button payWithGoogleButton = findViewById(R.id.btn_payment_with_google_launch);
        payWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, PayWithGoogleActivity.class);
                startActivity(intent);
            }
        });
    }

}
