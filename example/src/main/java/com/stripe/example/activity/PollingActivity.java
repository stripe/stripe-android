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
            // The client secret and source ID found here is identical to
            // that of the source used to get the redirect URL.
            String clientSecret = intent.getData().getQueryParameter(QUERY_CLIENT_SECRET);
            String sourceId = intent.getData().getQueryParameter(QUERY_SOURCE_ID);
            if (clientSecret != null && sourceId != null) {
                pollForSourceChanges(sourceId, clientSecret);
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

    /**
     * To start the 3DS cycle, create a {@link Source} out of the user-entered {@link Card}.
     *
     * @param card the {@link Card} used to create a source
     */
    void createCardSource(@NonNull Card card) {
        final SourceParams cardSourceParams = SourceParams.createCardParams(card);
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
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.setMessageResource(R.string.createSource);
                        mProgressDialogController.startProgress();
                    }
                })
                .subscribe(
                        // Because we've made the mapping above, we're now subscribing
                        // to the result of creating a 3DS Source
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                // The card Source can be used to create a 3DS Source
                                createThreeDSecureSource(source.getId());
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

    /**
     * Create the 3DS Source as a separate call to the API. This is what is needed
     * to verify the third-party approval. The only information from the Card source
     * that is used is the ID field.
     *
     * @param sourceId the {@link Source#mId} from the {@link Card}-created {@link Source}.
     */
    void createThreeDSecureSource(String sourceId) {
        // This represents a request for a 3DS purchase of 10.00 euro.
        final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                1000L,
                "EUR",
                RETURN_URL,
                sourceId);

        Observable<Source> threeDSecureObservable = Observable.fromCallable(
                new Callable<Source>() {
                    @Override
                    public Source call() throws Exception {
                        return mStripe.createSourceSynchronous(
                                threeDParams,
                                FUNCTIONAL_SOURCE_PUBLISHABLE_KEY);
                    }
                });

        mCompositeSubscription.add(threeDSecureObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.finishProgress();
                    }
                })
                .subscribe(
                        // Because we've made the mapping above, we're now subscribing
                        // to the result of creating a 3DS Source
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                // Once a 3DS Source is created, that is used
                                // to initiate the third-party verification
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

    /**
     * Show a dialog with a link to the external verification site.
     *
     * @param source the {@link Source} to verify
     */
    void showDialog(final Source source) {
        mPollingDialogController.showDialog(source.getRedirect().getUrl());
    }

    /**
     * Start polling for changes to the {@link Source#mStatus status} after
     * coming back from the redirect.
     *
     * @param sourceId the {@link Source#mId} being polled
     * @param clientSecret the {@link Source#mClientSecret}
     */
    void pollForSourceChanges(String sourceId, String clientSecret) {
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
                        Source source = pollingResponse.getSource();
                        if (source == null) {
                            mPollingAdapter.addItem(
                                    getCountString(count),
                                    "No source found",
                                    "Error");
                            return;
                        }
                        if (pollingResponse.isSuccess()) {
                            mPollingAdapter.addItem(
                                    getCountString(count),
                                    source.getStatus(),
                                    source.getId());
                        } else if (pollingResponse.isExpired()){
                            mPollingAdapter.addItem(
                                    getCountString(count),
                                    source.getStatus(),
                                    "Polling Expired");
                        } else {
                            StripeException stripeEx = pollingResponse.getStripeException();
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
