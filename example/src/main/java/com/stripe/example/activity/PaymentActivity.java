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

    private ProgressDialogFragment mProgressFragment;

    private StripeView mStripeView;
    private StripeView.OnValidationChangeListener mValidationListener
        = new StripeView.OnValidationChangeListener() {
            @Override
            public void onChange(boolean valid) {
                mSaveButton.setEnabled(valid);
            }
    };
    private View mSaveButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_activity);

        mProgressFragment = ProgressDialogFragment.newInstance(R.string.progressMessage);
        mStripeView = (StripeView) findViewById(R.id.stripe);
        mSaveButton = findViewById(R.id.save_button);
    }

    @Override
    public void onResume() {
        super.onResume();
        mStripeView.registerListener(mValidationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mStripeView.unregisterListener(mValidationListener);
    }

    public void saveCreditCard(View view) {
        mSaveButton.setEnabled(false);
        startProgress();
        mStripeView.createToken(PUBLISHABLE_KEY, new TokenCallback() {
            public void onSuccess(Token token) {
                getTokenList().addToList(token);
                finishProgress();
                mSaveButton.setEnabled(true);
            }
            public void onError(Exception error) {
                handleError(error.getLocalizedMessage());
                finishProgress();
                mSaveButton.setEnabled(true);
            }
        });
    }

    private void startProgress() {
        mProgressFragment.show(getSupportFragmentManager(), "progress");
    }

    private void finishProgress() {
        mProgressFragment.dismiss();
    }

    private void handleError(String error) {
        DialogFragment fragment = ErrorDialogFragment.newInstance(R.string.validationErrors, error);
        fragment.show(getSupportFragmentManager(), "error");
    }

    private TokenList getTokenList() {
        return (TokenList)(getSupportFragmentManager().findFragmentById(R.id.token_list));
    }
}