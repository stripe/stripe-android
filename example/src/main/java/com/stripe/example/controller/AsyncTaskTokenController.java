package com.stripe.example.controller;

import android.os.Build;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;

import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

/**
 * Created by mrmcduff on 12/5/16.
 */

public class AsyncTaskTokenController {

    private CardInformationReader mCardInformationReader;
    private ErrorDialogHandler mErrorDialogHandler;
    private ListViewController mOutputListController;
    private ProgressDialogController mProgressDialogController;
    private String mPublishableKey;

    public AsyncTaskTokenController(
            @NonNull Button button,
            @NonNull CardInformationReader cardInformationReader,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController,
            @NonNull String publishableKey) {
        mCardInformationReader = cardInformationReader;
        mErrorDialogHandler = errorDialogHandler;
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
                        mErrorDialogHandler.showError(error.getLocalizedMessage());
                        mProgressDialogController.finishProgress();
                    }
                });
    }
}
