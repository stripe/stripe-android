package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.stripe.android.PaymentConfiguration;
import com.stripe.example.R;

public class LauncherActivity extends AppCompatActivity {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://dashboard.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY =
            "pk_test_ykEgtXg3IeSaZhvYfrgQAwQN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        PaymentConfiguration.init(PUBLISHABLE_KEY);

        findViewById(R.id.btn_payment_intent)
                .setOnClickListener(v -> startActivity(PaymentIntentActivity.class));

        findViewById(R.id.btn_make_card_tokens)
                .setOnClickListener(v -> startActivity(PaymentActivity.class));

        findViewById(R.id.btn_make_card_payment_methods)
                .setOnClickListener(v -> startActivity(PaymentMultilineActivity.class));

        findViewById(R.id.btn_make_sources)
                .setOnClickListener(v -> startActivity(RedirectActivity.class));

        findViewById(R.id.btn_customer_session_launch)
                .setOnClickListener(v -> startActivity(CustomerSessionActivity.class));

        findViewById(R.id.btn_payment_session_launch)
                .setOnClickListener(v -> startActivity(PaymentSessionActivity.class));

        findViewById(R.id.btn_payment_with_google_launch)
                .setOnClickListener(v -> startActivity(PayWithGoogleActivity.class));

    }

    private void startActivity(@NonNull Class<?> activityClass) {
        startActivity(new Intent(this, activityClass));
    }

}
