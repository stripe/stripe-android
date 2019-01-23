package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

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

        findViewById(R.id.btn_payment_intent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherActivity.this, PaymentIntentActivity.class));
            }
        });

        findViewById(R.id.btn_make_card_tokens).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherActivity.this, PaymentActivity.class));
            }
        });

        findViewById(R.id.btn_make_card_sources).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LauncherActivity.this, PaymentMultilineActivity.class));
            }
        });

        findViewById(R.id.btn_make_sources).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherActivity.this, RedirectActivity.class));
            }
        });

        findViewById(R.id.btn_customer_session_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LauncherActivity.this, CustomerSessionActivity.class));
            }
        });

        findViewById(R.id.btn_payment_session_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LauncherActivity.this, PaymentSessionActivity.class));
            }
        });

        findViewById(R.id.btn_payment_with_google_launch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LauncherActivity.this, PayWithGoogleActivity.class));
            }
        });

    }

}
