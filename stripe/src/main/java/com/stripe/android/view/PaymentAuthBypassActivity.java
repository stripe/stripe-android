package com.stripe.android.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

/**
 * An {@link Activity} that is used when payment authentication can be bypassed.
 *
 * It sets a result and immediately finishes in order to pass the result back.
 */
public class PaymentAuthBypassActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_OK, new Intent().putExtras(getIntent().getExtras()));
        finish();
    }
}
