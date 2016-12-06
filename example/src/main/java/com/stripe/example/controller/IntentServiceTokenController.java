package com.stripe.example.controller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.text.IDNA;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.stripe.android.model.Card;
import com.stripe.example.activity.PaymentActivity;
import com.stripe.example.service.TokenIntentService;

/**
 * Example class showing how to save tokens with an {@link android.app.IntentService} doing your
 * background I/O work.
 */
public class IntentServiceTokenController {

    private Activity mActivity;
    private CardInformationReader mCardInformationReader;
    private ErrorDialogHandler mErrorDialogHandler;
    private ListViewController mOutputListViewController;
    private ProgressDialogController mProgressDialogController;
    private String mPublishableKey;

    private TokenBroadcastReceiver mTokenBroadcastReceiver;

    public IntentServiceTokenController (
            @NonNull AppCompatActivity appCompatActivity,
            @NonNull Button button,
            @NonNull CardInformationReader cardInformationReader,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController,
            @NonNull String publishableKey) {

        mActivity = appCompatActivity;
        mCardInformationReader = cardInformationReader;
        mErrorDialogHandler = errorDialogHandler;
        mOutputListViewController = outputListController;
        mProgressDialogController = progressDialogController;
        mPublishableKey = publishableKey;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveCard();
            }
        });
        registerBroadcastReceiver();
    }

    public void detach() {
        if (mTokenBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(mActivity)
                    .unregisterReceiver(mTokenBroadcastReceiver);
            mTokenBroadcastReceiver = null;
            mActivity = null;
        }
    }

    private void registerBroadcastReceiver() {
        mTokenBroadcastReceiver = new TokenBroadcastReceiver();
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(
                mTokenBroadcastReceiver,
                new IntentFilter(TokenIntentService.TOKEN_ACTION));
    }

    private void saveCard() {
        Card cardToSave = mCardInformationReader.readCardData();
        Intent tokenServiceIntent = TokenIntentService.createTokenIntent(
                mActivity,
                cardToSave.getNumber(),
                cardToSave.getExpMonth(),
                cardToSave.getExpYear(),
                cardToSave.getCVC(),
                mPublishableKey);
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
