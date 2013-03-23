package com.stripe.example.activity;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.widget.PaymentKitView;
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

    private PaymentKitView paymentKitView;
    private PaymentKitView.OnValidationChangeListener validationListener
        = new PaymentKitView.OnValidationChangeListener() {
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
        paymentKitView = (PaymentKitView) findViewById(R.id.payment_kit);
        saveButton = findViewById(R.id.save_button);
    }

    @Override
    public void onResume() {
        super.onResume();
        paymentKitView.registerListener(validationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        paymentKitView.unregisterListener(validationListener);
    }

    public void saveCreditCard(View view) {
        Card card = paymentKitView.getCard();

        boolean validation = card.validateCard();
        if (validation) {
            startProgress();
            new Stripe().createToken(
                    card,
                    PUBLISHABLE_KEY,
                    new TokenCallback() {
                        public void onSuccess(Token token) {
                            getTokenList().addToList(token);
                            finishProgress();
                        }
                    public void onError(Exception error) {
                            handleError(error.getLocalizedMessage());
                            finishProgress();
                        }
                    });
        } else {
            handleError("You did not enter a valid card");
        }
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
