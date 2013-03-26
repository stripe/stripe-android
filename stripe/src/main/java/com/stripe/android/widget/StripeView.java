package com.stripe.android.widget;

import com.stripe.android.R;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;

import android.content.Context;
import android.util.AttributeSet;

import com.stripe.android.model.Card;
import com.stripe.exception.CardException;

public class StripeView extends PaymentKitView {
    public StripeView(Context context) {
        super(context);
    }

    public StripeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StripeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void createToken(String publishableKey, TokenCallback callback) {
        Card card = getCard();

        if (!card.validateNumber()) {
            String msg = getContext().getResources().getString(
                    R.string.__stripe_invalid_number_message);
            callback.onError(new CardException(msg, "invalid_number", "number", null));
            return;
        }

        if (!card.validateExpMonth()) {
            String msg = getContext().getResources().getString(
                    R.string.__stripe_invalid_expiry_month_message);
            callback.onError(new CardException(msg, "invalid_expiry_month", "exp_month", null));
            return;
        }

        if (!card.validateExpYear()) {
            String msg = getContext().getResources().getString(
                    R.string.__stripe_invalid_expiry_year_message);
            callback.onError(new CardException(msg, "invalid_expiry_year", "exp_year", null));
            return;
        }

        if (!card.validateCVC()) {
            String msg = getContext().getResources().getString(
                    R.string.__stripe_invalid_cvc_message);
            callback.onError(new CardException(msg, "invalid_cvc", "cvc", null));
            return;
        }

        if (!card.validateCard()) {
            String msg = getContext().getResources().getString(
                    R.string.__stripe_invalid_card_message);
            callback.onError(new CardException(msg, null, null, null));
            return;
        }

        new Stripe().createToken(card, publishableKey, callback);
    }
}