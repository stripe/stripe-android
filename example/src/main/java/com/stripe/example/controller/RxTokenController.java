package com.stripe.example.controller;

import android.support.annotation.NonNull;
import android.widget.Button;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.example.R;

import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;


/**
 * Class containing all the logic needed to create a token and listen for the results using
 * RxJava.
 */
public class RxTokenController {

    private CardInformationReader mCardInformationHolder;
    private CompositeSubscription mCompositeSubscription;
    private MessageDialogHandler mMessageDialogHandler;
    private ListViewController mOutputListController;
    private ProgressDialogController mProgressDialogController;
    private String mPublishableKey;

    public RxTokenController (
            @NonNull Button button,
            @NonNull CardInformationReader cardInformationReader,
            @NonNull MessageDialogHandler messageDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController,
            @NonNull String publishableKey) {
        mCompositeSubscription = new CompositeSubscription();

        mCardInformationHolder = cardInformationReader;
        mMessageDialogHandler = messageDialogHandler;
        mOutputListController = outputListController;
        mProgressDialogController = progressDialogController;
        mPublishableKey = publishableKey;

        mCompositeSubscription.add(
                RxView.clicks(button).subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        saveCard();
                    }
                })
        );
    }

    /**
     * Release subscriptions to prevent memory leaks.
     */
    public void detach() {
        if  (mCompositeSubscription != null) {
            mCompositeSubscription.unsubscribe();
        }
    }

    private void saveCard() {
        final Card cardToSave = mCardInformationHolder.readCardData();
        final Stripe stripe = new Stripe();

        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        final Observable<Token> tokenObservable =
                Observable.fromCallable(
                        new Callable<Token>() {
                            @Override
                            public Token call() throws Exception {
                                return stripe.createTokenSynchronous(cardToSave, mPublishableKey);
                            }
                        });

        mCompositeSubscription.add(tokenObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                mProgressDialogController.startProgress();
                            }
                        })
                .doOnUnsubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                mProgressDialogController.finishProgress();
                            }
                        })
                .subscribe(
                        new Action1<Token>() {
                            @Override
                            public void call(Token token) {
                                mOutputListController.addToList(token);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mMessageDialogHandler.showMessage(R.string.validationErrors, throwable.getLocalizedMessage());
                            }
                        }));
    }
}
