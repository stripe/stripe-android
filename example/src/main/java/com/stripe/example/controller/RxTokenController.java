package com.stripe.example.controller;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;

import com.jakewharton.rxbinding2.view.RxView;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;

import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Class containing all the logic needed to create a token and listen for the results using
 * RxJava.
 */
public class RxTokenController {

    @NonNull private final CompositeDisposable mCompositeDisposable;
    @NonNull private final Context mContext;
    @NonNull private final ErrorDialogHandler mErrorDialogHandler;
    @NonNull private final ListViewController mOutputListController;
    @NonNull private final ProgressDialogController mProgressDialogController;

    @Nullable private CardInputWidget mCardInputWidget;

    public RxTokenController(
            @NonNull Button button,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull Context context,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController) {
        mCompositeDisposable = new CompositeDisposable();

        mCardInputWidget = cardInputWidget;
        mContext = context;
        mErrorDialogHandler = errorDialogHandler;
        mOutputListController = outputListController;
        mProgressDialogController = progressDialogController;

        mCompositeDisposable.add(
                RxView.clicks(button).subscribe(aVoid -> saveCard())
        );
    }

    /**
     * Release subscriptions to prevent memory leaks.
     */
    public void detach() {
        mCompositeDisposable.dispose();
        mCardInputWidget = null;
    }

    private void saveCard() {
        final Card cardToSave = Objects.requireNonNull(mCardInputWidget).getCard();
        if (cardToSave == null) {
            mErrorDialogHandler.show("Invalid Card Data");
            return;
        }
        final Stripe stripe = new Stripe(mContext,
                PaymentConfiguration.getInstance().getPublishableKey());

        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        final Observable<Token> tokenObservable =
                Observable.fromCallable(() -> stripe.createTokenSynchronous(cardToSave));

        mCompositeDisposable.add(tokenObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        (d) -> mProgressDialogController.show(R.string.progressMessage))
                .doOnComplete(mProgressDialogController::dismiss)
                .subscribe(
                        mOutputListController::addToList,
                        throwable -> mErrorDialogHandler.show(throwable.getLocalizedMessage())
                )
        );
    }
}
