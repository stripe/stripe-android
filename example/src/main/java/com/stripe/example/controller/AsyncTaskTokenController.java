package com.stripe.example.controller;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;

/**
 * Logic needed to create tokens using the {@link android.os.AsyncTask} methods included in the
 * sdk: {@link Stripe#createToken(Card, String, TokenCallback)}.
 */
public class AsyncTaskTokenController {

    @NonNull private final Stripe mStripe;
    @NonNull private final ErrorDialogHandler mErrorDialogHandler;
    @NonNull private final ListViewController mOutputListController;
    @NonNull private final ProgressDialogController mProgressDialogController;

    @Nullable private CardInputWidget mCardInputWidget;

    public AsyncTaskTokenController(
            @NonNull Button button,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull Context context,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController) {
        mCardInputWidget = cardInputWidget;
        mStripe = new Stripe(context);
        mErrorDialogHandler = errorDialogHandler;
        mProgressDialogController = progressDialogController;
        mOutputListController = outputListController;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCard();
            }
        });
    }

    public void detach() {
        mCardInputWidget = null;
    }

    private void saveCard() {
        final Card cardToSave = mCardInputWidget.getCard();
        if (cardToSave == null) {
            mErrorDialogHandler.showError("Invalid Card Data");
            return;
        }
        mProgressDialogController.startProgress();
        mStripe.createToken(
                cardToSave,
                PaymentConfiguration.getInstance().getPublishableKey(),
                new TokenCallback() {
                    public void onSuccess(@NonNull Token token) {
                        mOutputListController.addToList(token);
                        mProgressDialogController.finishProgress();
                    }
                    public void onError(@NonNull Exception error) {
                        mErrorDialogHandler.showError(error.getLocalizedMessage());
                        mProgressDialogController.finishProgress();
                    }
                });
    }
}
