package com.stripe.example.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.stripe.example.R;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.*;
import com.stripe.example.dialog.ValidationErrorDialog;
import com.stripe.example.dialog.ValidationProgressDialog;
import com.stripe.example.PaymentForm;
import com.stripe.example.TokenList;


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
    public static final String PUBLISHABLE_KEY = "YOUR_PUBLISHABLE_KEY";

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

        boolean validation = card.validateCard();
        if (validation) {
            startProgress();
            try {
				new Stripe(PUBLISHABLE_KEY).createToken(
						card,
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
			} catch (AuthenticationException e) {
				handleError(e.getLocalizedMessage());
			}
           
        } else {
            handleError("You did not enter a valid card");
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
}
