package com.stripe.example.activity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.stripe.android.TokenCallback;
import com.stripe.android.model.Token;
import com.stripe.android.widget.StripeView;
import com.stripe.example.R;
import com.stripe.example.TokenList;
import com.stripe.example.dialog.ErrorDialogFragment;
import com.stripe.example.dialog.ProgressDialogFragment;

public class PaymentActivity extends FragmentActivity {

    /*
     * You need to set this to your stripe test publishable key.
     *
     * For more info, see https://stripe.com/docs/stripe.js
     *
     * E.g.
     *
     *   private static final String publishableKey = "pk_something123456789";
     *
     */
    public static final String PUBLISHABLE_KEY = YOUR_PUBLISHABLE_KEY;

    private ProgressDialogFragment progressFragment;

    private StripeView stripeView;
    private StripeView.OnValidationChangeListener validationListener
        = new StripeView.OnValidationChangeListener() {
            @Override
            public void onChange(boolean valid) {
                saveButton.setEnabled(valid);
            }
    };
    private View saveButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_activity);

        progressFragment = ProgressDialogFragment.newInstance(R.string.progressMessage);
        stripeView = (StripeView) findViewById(R.id.stripe);
        saveButton = findViewById(R.id.save_button);
    }

    @Override
    public void onResume() {
        super.onResume();
        stripeView.registerListener(validationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        stripeView.unregisterListener(validationListener);
    }

    public void saveCreditCard(View view) {
        startProgress();
        stripeView.createToken(PUBLISHABLE_KEY, new TokenCallback() {
            public void onSuccess(Token token) {
                getTokenList().addToList(token);
                finishProgress();
            }
            public void onError(Exception error) {
                handleError(error.getLocalizedMessage());
                finishProgress();
            }
        });
    }

    private void startProgress() {
        progressFragment.show(getSupportFragmentManager(), "progress");
    }

    private void finishProgress() {
        progressFragment.dismiss();
    }

    private void handleError(String error) {
        DialogFragment fragment = ErrorDialogFragment.newInstance(R.string.validationErrors, error);
        fragment.show(getSupportFragmentManager(), "error");
    }

    private TokenList getTokenList() {
        return (TokenList)(getSupportFragmentManager().findFragmentById(R.id.token_list));
    }
}