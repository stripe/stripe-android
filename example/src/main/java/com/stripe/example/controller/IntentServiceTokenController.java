package com.stripe.example.controller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.stripe.android.model.Card;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.service.TokenIntentService;

/**
 * Example class showing how to save tokens with an {@link android.app.IntentService} doing your
 * background I/O work.
 */
public class IntentServiceTokenController {

    private Activity mActivity;
    private CardInputWidget mCardInputWidget;
    private ErrorDialogHandler mErrorDialogHandler;
    private ListViewController mOutputListViewController;
    private ProgressDialogController mProgressDialogController;

    private TokenBroadcastReceiver mTokenBroadcastReceiver;

    public IntentServiceTokenController (
            @NonNull AppCompatActivity appCompatActivity,
            @NonNull Button button,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController) {

        mActivity = appCompatActivity;
        mCardInputWidget = cardInputWidget;
        mErrorDialogHandler = errorDialogHandler;
        mOutputListViewController = outputListController;
        mProgressDialogController = progressDialogController;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCard();
            }
        });
        registerBroadcastReceiver();
    }

    /**
     * Unregister the {@link BroadcastReceiver}.
     */
    public void detach() {
        if (mTokenBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(mActivity)
                    .unregisterReceiver(mTokenBroadcastReceiver);
            mTokenBroadcastReceiver = null;
            mActivity = null;
        }
        mCardInputWidget = null;
    }

    private void registerBroadcastReceiver() {
        mTokenBroadcastReceiver = new TokenBroadcastReceiver();
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(
                mTokenBroadcastReceiver,
                new IntentFilter(TokenIntentService.TOKEN_ACTION));
    }

    private void saveCard() {
        Card cardToSave = mCardInputWidget.getCard();
        if (cardToSave == null) {
            mErrorDialogHandler.showError("Invalid Card Data");
            return;
        }
        Intent tokenServiceIntent = TokenIntentService.createTokenIntent(
                mActivity,
                cardToSave.getNumber(),
                cardToSave.getExpMonth(),
                cardToSave.getExpYear(),
                cardToSave.getCVC());
        mProgressDialogController.startProgress();
        mActivity.startService(tokenServiceIntent);
    }

    private class TokenBroadcastReceiver extends BroadcastReceiver {

        // Prevent instantiation of a local broadcast receiver outside this class.
        private TokenBroadcastReceiver() { }

        @Override
        public void onReceive(Context context, Intent intent) {
            mProgressDialogController.finishProgress();

            if (intent == null) {
                return;
            }

            if (intent.hasExtra(TokenIntentService.STRIPE_ERROR_MESSAGE)) {
                mErrorDialogHandler.showError(
                        intent.getStringExtra(TokenIntentService.STRIPE_ERROR_MESSAGE));
                return;
            }

            if (intent.hasExtra(TokenIntentService.STRIPE_CARD_TOKEN_ID) &&
                    intent.hasExtra(TokenIntentService.STRIPE_CARD_LAST_FOUR)) {
                mOutputListViewController.addToList(
                        intent.getStringExtra(TokenIntentService.STRIPE_CARD_LAST_FOUR),
                        intent.getStringExtra(TokenIntentService.STRIPE_CARD_TOKEN_ID));
            }
        }
    }
}
