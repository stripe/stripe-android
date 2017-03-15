package com.stripe.example.controller;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.Stripe;
import com.stripe.android.exception.PollingFailedException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.stripe.android.net.PollingResponseHandler;
import com.stripe.android.view.CardInputWidget;

import java.util.List;
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

    private AppCompatActivity mAppCompatActivity;
    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private Context mContext;
    private ErrorDialogHandler mErrorDialogHandler;
    private ListViewController mOutputListController;
    private ProgressDialogController mProgressDialogController;
    private String mPublishableKey;
    private Stripe mStripe;

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    public RxTokenController (
            @NonNull Button button,
            @NonNull AppCompatActivity appCompatActivity,
            @NonNull CardInputWidget cardInputWidget,
            @NonNull Context context,
            @NonNull ErrorDialogHandler errorDialogHandler,
            @NonNull ListViewController outputListController,
            @NonNull ProgressDialogController progressDialogController,
            @NonNull String publishableKey) {
        mCompositeSubscription = new CompositeSubscription();


        mCardInputWidget = cardInputWidget;
        mContext = context;

        mAppCompatActivity = appCompatActivity;
        mStripe = new Stripe(mContext);
        mErrorDialogHandler = errorDialogHandler;
        mOutputListController = outputListController;
        mProgressDialogController = progressDialogController;
        mPublishableKey = FUNCTIONAL_SOURCE_PUBLISHABLE_KEY;

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
        mCardInputWidget = null;
    }

    private void saveCard() {
        final Card cardToSave = mCardInputWidget.getCard();
        if (cardToSave == null) {
            mErrorDialogHandler.showError("Invalid Card Data");
            return;
        }

        final SourceParams cardParams = SourceParams.createCardParams(cardToSave);

        final Observable<Source> cardSourceObservable =
                Observable.fromCallable(
                        new Callable<Source>() {
                            @Override
                            public Source call() throws Exception {
                                return mStripe.createSourceSynchronous(
                                        cardParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
                            }
                        });

        mCompositeSubscription.add(cardSourceObservable
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
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                createThreeDSource(source);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {

                            }
                        }));

    }

    private void createThreeDSource(Source cardSource) {
        final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                1000L, "EUR", "example://wassup", cardSource.getId());

        final Observable<Source> threeDSourceObservable =
                Observable.fromCallable(
                        new Callable<Source>() {
                            @Override
                            public Source call() throws Exception {
                                return mStripe.createSourceSynchronous(
                                        threeDParams, FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
                            }
                        });

        mCompositeSubscription.add(threeDSourceObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                pollForUpdates(source);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {

                            }
                        }));
    }

    private void pollForUpdates(final Source threeDSource) {
        Log.d("chewie", threeDSource.getRedirect().getUrl());
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mAppCompatActivity);
        dialogBuilder.setMessage("Go to the site in the console and approve to succeed");
        dialogBuilder.setNeutralButton("I'm ready", new DialogInterface.OnClickListener() {
            int recount = 0;
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mStripe.pollSource(
                        threeDSource.getId(),
                        threeDSource.getClientSecret(),
                        mPublishableKey,
                        new PollingResponseHandler() {
                            @Override
                            public void onSuccess() {
                                mErrorDialogHandler.showError("Somehow we succeeded");
                            }

                            @Override
                            public void onRetry(int millis) {
                                Log.d("chewie", "Retry number " + ++recount + " delay " + millis);
                            }

                            @Override
                            public void onError(StripeException stripeEx) {
                                String message;
                                if (stripeEx instanceof PollingFailedException) {
                                    message = "Polling error: ";
                                    boolean isTimeout = ((PollingFailedException) stripeEx).isExpired();
                                    if (isTimeout) {
                                        message += "expiration";
                                    }
                                } else {
                                    message = "Failed with some other exception: " + stripeEx.getLocalizedMessage();
                                }
                                mErrorDialogHandler.showError(message);
                            }
                        },
                        5000);
            }
        });
        dialogBuilder.create().show();
//        dialogBuilder.create().show(mFragmentManager, "alert");
    }
}
