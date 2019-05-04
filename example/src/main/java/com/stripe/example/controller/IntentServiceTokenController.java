package com.stripe.example.controller;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Button;

import com.stripe.android.model.Card;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;
import com.stripe.example.service.TokenIntentService;

/**
 * Example class showing how to save tokens with an {@link android.app.IntentService} doing your
 * background I/O work.
 */
public class IntentServiceTokenController {

    @NonNull private final ErrorDialogHandler mErrorDialogHandler;
    @NonNull private final ListViewController mOutputListViewController;
    @NonNull private final ProgressDialogController mProgressDialogController;

    private Activity mActivity;
    @Nullable private CardInputWidget mCardInputWidget;
    @Nullable private TokenBroadcastReceiver mTokenBroadcastReceiver;

    public IntentServiceTokenController (
            @NonNull Activity activity,
            @NonNull Button button,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController) {

        mActivity = activity;
        mCardInputWidget = cardInputWidget;
        mErrorDialogHandler = errorDialogHandler;
        mOutputListViewController = outputListController;
        mProgressDialogController = progressDialogController;

        button.setOnClickListener(v -> saveCard());
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
        final Card cardToSave = mCardInputWidget.getCard();
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data");
            return;
        }
        final Intent tokenServiceIntent = TokenIntentService.createTokenIntent(
                mActivity,
                cardToSave.getNumber(),
                cardToSave.getExpMonth(),
                cardToSave.getExpYear(),
                cardToSave.getCVC());
        mProgressDialogController.show(R.string.progressMessage);
        mActivity.startService(tokenServiceIntent);
    }

    private class TokenBroadcastReceiver extends BroadcastReceiver {

        // Prevent instantiation of a local broadcast receiver outside this class.
        private TokenBroadcastReceiver() { }

        @Override
        public void onReceive(Context context, Intent intent) {
            mProgressDialogController.dismiss();

            if (intent == null) {
                return;
            }

            if (intent.hasExtra(TokenIntentService.STRIPE_ERROR_MESSAGE)) {
                mErrorDialogHandler.show(
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
