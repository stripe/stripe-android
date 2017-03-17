package com.stripe.example.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.stripe.android.Stripe;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.net.PollingResponse;
import com.stripe.android.net.PollingResponseHandler;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;
import com.stripe.example.adapter.PollingAdapter;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.PollingDialogController;
import com.stripe.example.controller.ProgressDialogController;

import java.util.Locale;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Activity that lets you poll for a 3DS source update.
 */
public class PollingActivity extends AppCompatActivity {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";
    private static final String RETURN_URL = "stripe://example";

    private static final String QUERY_CLIENT_SECRET = "client_secret";
    private static final String QUERY_SOURCE_ID = "source";

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private PollingAdapter mPollingAdapter;
    private ErrorDialogHandler mErrorDialogHandler;
    private PollingDialogController mPollingDialogController;
    private ProgressDialogController mProgressDialogController;
    private Stripe mStripe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polling);

        mCompositeSubscription = new CompositeSubscription();
        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_widget_three_d);
        mErrorDialogHandler = new ErrorDialogHandler(this.getSupportFragmentManager());
        mProgressDialogController = new ProgressDialogController(this.getSupportFragmentManager());
        mPollingDialogController = new PollingDialogController(this);
        mStripe = new Stripe(this);

        Button threeDSecureButton = (Button) findViewById(R.id.btn_three_d_secure);
        threeDSecureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        beginSequence();
                    }
                });
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        RecyclerView.LayoutManager linearLayoutManager = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        mPollingAdapter = new PollingAdapter();
        recyclerView.setAdapter(mPollingAdapter);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null && intent.getData().getQuery() != null) {
            String clientSecret = intent.getData().getQueryParameter(QUERY_CLIENT_SECRET);
            String sourceId = intent.getData().getQueryParameter(QUERY_SOURCE_ID);
            if (clientSecret != null && sourceId != null) {
                addNewSource(sourceId, clientSecret);
            }
            mPollingDialogController.dismissDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }

    void beginSequence() {
        Card displayCard = mCardInputWidget.getCard();
        if (displayCard == null) {
            return;
        }
        createCardSource(displayCard);
    }

    void createCardSource(@NonNull Card card) {
        final SourceParams cardSourceParams = SourceParams.createCardParams(card);
        if (cardSourceParams == null) {
            return;
        }

        final Observable<Source> cardSourceObservable =
                Observable.fromCallable(
                        new Callable<Source>() {
                            @Override
                            public Source call() throws Exception {
                                return mStripe.createSourceSynchronous(
                                        cardSourceParams,
                                        FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
                            }
                        });

        mCompositeSubscription.add(cardSourceObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.setMessageResource(R.string.createSource);
                        mProgressDialogController.startProgress();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.finishProgress();
                    }
                })
                .flatMap(
                        new Func1<Source, Observable<Source>>() {
                            // This mapping converts the card source into a 3DS source
                            @Override
                            public Observable<Source> call(Source source) {
                                if (source == null || !Source.CARD.equals(source.getType())) {
                                    return null;
                                }

                                // This represents a request for a 3DS purchase of 10.00 euro.
                                final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                                        1000L,
                                        "EUR",
                                        RETURN_URL,
                                        source.getId());

                                return Observable.fromCallable(
                                        new Callable<Source>() {
                                            @Override
                                            public Source call() throws Exception {
                                                return mStripe.createSourceSynchronous(
                                                        threeDParams,
                                                        FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
                                            }
                                        });
                            }
                        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        // Because we've made the mapping above, we're now subscribing
                        // to the result of creating a 3DS Source
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                showDialog(source);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mErrorDialogHandler.showError(throwable.getMessage());
                            }
                        }
                ));
    }

    void showDialog(final Source source) {
        mPollingDialogController.showDialog(source.getRedirect().getUrl());
    }

    void addNewSource(String sourceId, String clientSecret) {
        mProgressDialogController.setMessageResource(R.string.pollingSource);
        mProgressDialogController.startProgress();
        mStripe.pollSource(
                sourceId,
                clientSecret,
                FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                new PollingResponseHandler() {
                    int count = 0;
                    @Override
                    public void onPollingResponse(PollingResponse pollingResponse) {
                        count++;
                        mProgressDialogController.finishProgress();
                        if (pollingResponse.isSuccess()) {
                            Source source = pollingResponse.getSource();
                            if (source == null) {
                                return;
                            }
                            mPollingAdapter.addItem(
                                    getCountString(count),
                                    source.getStatus(),
                                    source.getId());
                        } else if (pollingResponse.isExpired()){
                            Source source = pollingResponse.getSource();
                            mPollingAdapter.addItem(
                                    getCountString(count),
                                    source.getStatus(),
                                    "Polling Expired");
                        } else {
                            StripeException stripeEx = pollingResponse.getStripeException();
                            Source source = pollingResponse.getSource();
                            if (stripeEx != null) {
                                mPollingAdapter.addItem(
                                        getCountString(count),
                                        "error",
                                        stripeEx.getMessage());
                            } else {
                                mPollingAdapter.addItem(
                                        getCountString(count),
                                        source.getStatus(),
                                        source.getId());
                            }
                        }
                    }

                    @Override
                    public void onRetry(int millis) {
                        count++;
                    }
                },
                null);
    }

    private static String getCountString(int count) {
        return String.format(Locale.ENGLISH, "API Queries: %d", count);
    }
}
