package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

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
                startActivity(PaymentIntentActivity.class);
            }
        });

        findViewById(R.id.btn_make_card_tokens).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(PaymentActivity.class);
            }
        });

        findViewById(R.id.btn_make_card_payment_methods)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(PaymentMultilineActivity.class);
                    }
                });

        findViewById(R.id.btn_make_sources).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(RedirectActivity.class);
            }
        });

        findViewById(R.id.btn_customer_session_launch)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(CustomerSessionActivity.class);
                    }
                });

        findViewById(R.id.btn_payment_session_launch)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startActivity(PaymentSessionActivity.class);
                    }
                });

        findViewById(R.id.btn_payment_with_google_launch)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(PayWithGoogleActivity.class);
                    }
                });

    }

    private void startActivity(@NonNull Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

}
