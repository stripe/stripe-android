package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import static com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION;

/**
 * An {@link Activity} that relays the intent extras that it received as a result and finishes.
 */
public class PaymentRelayActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent().setAction(UL_HANDLE_CHALLENGE_ACTION));
        setResult(Activity.RESULT_OK, new Intent().putExtras(getIntent().getExtras()));
        finish();
    }
}
