package com.stripe.example.controller;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.example.R;

/**
 * Logic needed to create tokens using the {@link android.os.AsyncTask} methods included in the
 * sdk: {@link Stripe#createToken(Card, String, TokenCallback)}.
 */
public class AsyncTaskTokenController {

    private CardInformationReader mCardInformationReader;
    private MessageDialogHandler mMessageDialogHandler;
    private ListViewController mOutputListController;
    private ProgressDialogController mProgressDialogController;
    private String mPublishableKey;

    public AsyncTaskTokenController(
            @NonNull Button button,
            @NonNull CardInformationReader cardInformationReader,
            @NonNull MessageDialogHandler messageDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController,
            @NonNull String publishableKey) {
        mCardInformationReader = cardInformationReader;
        mMessageDialogHandler = messageDialogHandler;
        mPublishableKey = publishableKey;
        mProgressDialogController = progressDialogController;
        mOutputListController = outputListController;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCard();
            }
        });
    }

    private void saveCard() {
        Card cardToSave = mCardInformationReader.readCardData();
        mProgressDialogController.startProgress();
        new Stripe().createToken(
                cardToSave,
                mPublishableKey,
                new TokenCallback() {
                    public void onSuccess(Token token) {
                        mOutputListController.addToList(token);
                        mProgressDialogController.finishProgress();
                    }
                    public void onError(Exception error) {
                        mMessageDialogHandler.showMessage(R.string.validationErrors, error.getLocalizedMessage());
                        mProgressDialogController.finishProgress();
                    }
                });
    }
}
