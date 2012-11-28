package com.stripe.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.stripe.*;
import com.stripe.dialog.ValidationErrorDialog;
import com.stripe.dialog.ValidationProgressDialog;

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

    ValidationProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payment_activity);
    }

    public void saveCreditCard(PaymentForm form) {

        Card card = new Card(
                form.getCardNumber(),
                form.getExpMonth(),
                form.getExpYear(),
                form.getCvc());

        Validation validation = card.validateCard();
        if (validation.isValid) {
            startProgress();
            new Stripe(PUBLISHABLE_KEY).createToken(
                    card,
                    new MyStripeSuccessHandler(),
                    new MyStripeErrorHandler()
            );
        } else {
            handleError(validation.getLocalizedErrors(this));
        }
    }

    private void startProgress() {
        String progressMessage = getResources().getString(R.string.progressMessage);
        this.progressDialog = new ValidationProgressDialog(this, progressMessage);
        this.progressDialog.show();
    }

    private void finishProgress() {
        this.progressDialog.dismiss();
    }

    private void handleError(String error) {
        new ValidationErrorDialog(this, error).show();
    }

    private TokenList getTokenList() {
        return (TokenList)(getSupportFragmentManager().findFragmentById(R.id.token_list));
    }

    public class MyStripeSuccessHandler extends StripeSuccessHandler {
        @Override
        public void onSuccess(Token token) {
            getTokenList().addToList(token);
            finishProgress();
        }
    }

    public class MyStripeErrorHandler extends StripeErrorHandler {
        @Override
        public void onError(StripeError error) {
            handleError(error.getLocalizedString(PaymentActivity.this));
            finishProgress();
        }
    }
}
