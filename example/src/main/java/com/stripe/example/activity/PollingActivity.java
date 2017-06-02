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
import com.stripe.android.model.SourceCardData;
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

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://manage.stripe.com/account/apikeys
     */
    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "put your key here";
    private static final String RETURN_SCHEMA = "stripe://";
    private static final String RETURN_HOST_ASYNC = "async";
    private static final String RETURN_HOST_SYNC = "sync";

    private static final String QUERY_CLIENT_SECRET = "client_secret";
    private static final String QUERY_SOURCE_ID = "source";

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private PollingAdapter mPollingAdapter;
    private ErrorDialogHandler mErrorDialogHandler;
    private PollingDialogController mPollingDialogController;
    private Source mPollingSource;
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
                        beginSequence(false);
                    }
                });

        Button threeDSyncButton = (Button) findViewById(R.id.btn_three_d_secure_sync);
        threeDSyncButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        beginSequence(true);
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

            String host = intent.getData().getHost();
            String clientSecret = intent.getData().getQueryParameter(QUERY_CLIENT_SECRET);
            String sourceId = intent.getData().getQueryParameter(QUERY_SOURCE_ID);
            if (clientSecret != null
                    && sourceId != null
                    && clientSecret.equals(mPollingSource.getClientSecret())
                    && sourceId.equals(mPollingSource.getId())) {
                if (RETURN_HOST_SYNC.equals(host)) {
                    pollSynchronouslyForSourceChanges(sourceId, clientSecret);
                } else {
                    pollForSourceChanges(sourceId, clientSecret);
                }
            }
            mPollingDialogController.dismissDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCompositeSubscription.unsubscribe();
    }

    void beginSequence(boolean shouldPollWithBlockingMethod) {
        Card displayCard = mCardInputWidget.getCard();
        if (displayCard == null) {
            return;
        }
        createCardSource(displayCard, shouldPollWithBlockingMethod);
    }

    /**
     * To start the 3DS cycle, create a {@link Source} out of the user-entered {@link Card}.
     *
     * @param card the {@link Card} used to create a source
     */
    void createCardSource(@NonNull Card card, final boolean shouldPollWithBlockingMethod) {
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
                                SourceCardData sourceCardData =
                                        (SourceCardData) source.getSourceTypeModel();

                                // Making a note of the Card Source in our list.
                                mPollingAdapter.addItem(
                                        source.getStatus(),
                                        sourceCardData.getThreeDSecureStatus(),
                                        source.getId(),
                                        source.getType());
                                // If we need to get 3DS verification for this card, we
                                // first create a 3DS Source.
                                if (SourceCardData.REQUIRED.equals(
                                        sourceCardData.getThreeDSecureStatus())) {

                                    // The card Source can be used to create a 3DS Source
                                    createThreeDSecureSource(source.getId(),
                                            shouldPollWithBlockingMethod);
                                } else {
                                    mProgressDialogController.finishProgress();
                                }

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
    void createThreeDSecureSource(String sourceId, boolean shouldPollWithBlockingMethod) {
        // This represents a request for a 3DS purchase of 10.00 euro.
        final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                1000L,
                "EUR",
                getUrl(shouldPollWithBlockingMethod),
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
        // Caching the source object here because this app makes a lot of them.
        mPollingSource = source;
        mPollingDialogController.showDialog(source.getRedirect().getUrl());
    }

    /**
     * Start polling for changes to the {@link Source#mStatus status} after
     * coming back from the redirect. This method generates a background thread to handle
     * the IO necessary for the transaction automatically.
     *
     * @param sourceId the {@link Source#mId} being polled
     * @param clientSecret the {@link Source#mClientSecret}
     */
    void pollForSourceChanges(final String sourceId, String clientSecret) {
        mProgressDialogController.setMessageResource(R.string.pollingSource);
        mProgressDialogController.startProgress();
        mStripe.pollSource(
                sourceId,
                clientSecret,
                FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                new PollingResponseHandler() {
                    @Override
                    public void onPollingResponse(PollingResponse pollingResponse) {
                        mProgressDialogController.finishProgress();
                        updatePollingSourceList(pollingResponse);
                    }
                },
                null);
    }

    /**
     * Start polling for changes to the {@link Source#mStatus status} after
     * coming back from the redirect. This method requires additional code to handle
     * the threading for the IO. Below is an example using RxJava.
     *
     * @param sourceId the {@link Source#mId} being polled
     * @param clientSecret the {@link Source#mClientSecret}
     */
    private void pollSynchronouslyForSourceChanges(
            final String sourceId,
            final String clientSecret) {

        Observable<PollingResponse> sourceUpdateObservable = Observable.fromCallable(
                new Callable<PollingResponse>() {
                    @Override
                    public PollingResponse call() throws Exception {
                        return mStripe.pollSourceSynchronous(
                                sourceId,
                                clientSecret,
                                FUNCTIONAL_SOURCE_PUBLISHABLE_KEY,
                                null);
                    }
                });

        mCompositeSubscription.add(sourceUpdateObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.setMessageResource(R.string.pollingSource);
                        mProgressDialogController.startProgress();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.finishProgress();
                    }
                })
                .subscribe(
                        new Action1<PollingResponse>() {
                            @Override
                            public void call(PollingResponse pollingResponse) {
                                updatePollingSourceList(pollingResponse);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mErrorDialogHandler.showError(throwable.getLocalizedMessage());
                            }
                        })
        );
    }

    private void updatePollingSourceList(PollingResponse pollingResponse) {
        Source source = pollingResponse.getSource();
        if (source == null) {
            mPollingAdapter.addItem(
                    "No source found",
                    "Stopped",
                    "Error",
                    "None");
            return;
        }

        if (pollingResponse.isSuccess()) {
            mPollingAdapter.addItem(
                    source.getStatus(),
                    "complete",
                    source.getId(),
                    source.getType());
        } else if (pollingResponse.isExpired()){
            mPollingAdapter.addItem(
                    "Expired",
                    "Stopped",
                    source.getId(),
                    source.getType());
        } else {
            StripeException stripeEx = pollingResponse.getStripeException();
            if (stripeEx != null) {
                mPollingAdapter.addItem(
                        "error",
                        "ERR",
                        stripeEx.getMessage(),
                        source.getType());
            } else {
                mPollingAdapter.addItem(
                        source.getStatus(),
                        "failed",
                        source.getId(),
                        source.getType());
            }
        }
    }

    private static String getCountString(int count) {
        return String.format(Locale.ENGLISH, "API Queries: %d", count);
    }

    /**
     * Helper method to determine the return URL we use. This is how we know
     * from the callback whether to use the Synchronous or Asynchronous polling method,
     * which is purely a matter of preference.
     *
     * @param isSync whether or not to use a URL that tells us to use a sync method when we come
     *               back to the application
     * @return a return url to be sent to the vendor
     */
    private static String getUrl(boolean isSync) {
        if (isSync) {
            return RETURN_SCHEMA + RETURN_HOST_SYNC;
        } else {
            return RETURN_SCHEMA + RETURN_HOST_ASYNC;
        }
    }
}
