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
import com.stripe.example.R;

/**
 * Logic needed to create tokens using the {@link android.os.AsyncTask} methods included in the
 * sdk: {@link Stripe#createToken(Card, String, TokenCallback)}.
 */
public class AsyncTaskTokenController {

    @NonNull private final Stripe mStripe;
    @NonNull private final ErrorDialogHandler mErrorDialogHandler;
    @NonNull private final ProgressDialogController mProgressDialogController;

    @Nullable private CardInputWidget mCardInputWidget;

    public AsyncTaskTokenController(
            @NonNull Button button,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull Context context,
            @NonNull final ErrorDialogHandler errorDialogHandler,
            @NonNull final ListViewController outputListController,
            @NonNull final ProgressDialogController progressDialogController) {
        mCardInputWidget = cardInputWidget;
        mStripe = new Stripe(context);
        mErrorDialogHandler = errorDialogHandler;
        mProgressDialogController = progressDialogController;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCard(new TokenCallbackImpl(
                        errorDialogHandler,
                        outputListController,
                        progressDialogController
                ));
            }
        });
    }

    public void detach() {
        mCardInputWidget = null;
    }

    private void saveCard(@NonNull TokenCallback tokenCallback) {
        final Card cardToSave = mCardInputWidget != null ? mCardInputWidget.getCard() : null;
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data");
            return;
        }

        mProgressDialogController.show(R.string.progressMessage);
        mStripe.createToken(
                cardToSave,
                PaymentConfiguration.getInstance().getPublishableKey(),
                tokenCallback);
    }

    private static class TokenCallbackImpl implements TokenCallback {
        @NonNull private final ErrorDialogHandler mErrorDialogHandler;
        @NonNull private final ListViewController mOutputListController;
        @NonNull private final ProgressDialogController mProgressDialogController;

        private TokenCallbackImpl(@NonNull ErrorDialogHandler errorDialogHandler,
                                 @NonNull ListViewController outputListController,
                                 @NonNull ProgressDialogController progressDialogController) {
            this.mErrorDialogHandler = errorDialogHandler;
            this.mOutputListController = outputListController;
            this.mProgressDialogController = progressDialogController;
        }

        public void onSuccess(@NonNull Token token) {
            mOutputListController.addToList(token);
            mProgressDialogController.dismiss();
        }
        public void onError(@NonNull Exception error) {
            mErrorDialogHandler.show(error.getLocalizedMessage());
            mProgressDialogController.dismiss();
        }
    }
}
