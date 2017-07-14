package com.stripe.example.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;
import com.stripe.example.adapter.RedirectAdapter;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.RedirectDialogController;
import com.stripe.example.controller.ProgressDialogController;

import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * Activity that lets you redirect for a 3DS source verification.
 */
public class RedirectActivity extends AppCompatActivity {

    private static final String RETURN_SCHEMA = "stripe://";
    private static final String RETURN_HOST_ASYNC = "async";
    private static final String RETURN_HOST_SYNC = "sync";

    private static final String QUERY_CLIENT_SECRET = "client_secret";
    private static final String QUERY_SOURCE_ID = "source";

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private RedirectAdapter mRedirectAdapter;
    private ErrorDialogHandler mErrorDialogHandler;
    private RedirectDialogController mRedirectDialogController;
    private Source mRedirectSource;
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
        mRedirectDialogController = new RedirectDialogController(this);
        mStripe = new Stripe(this);

        Button threeDSecureButton = (Button) findViewById(R.id.btn_three_d_secure);
        threeDSecureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        beginSequence();
                    }
                });

        Button threeDSyncButton = (Button) findViewById(R.id.btn_three_d_secure_sync);
        threeDSyncButton.setOnClickListener(
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
        mRedirectAdapter = new RedirectAdapter();
        recyclerView.setAdapter(mRedirectAdapter);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getData() != null && intent.getData().getQuery() != null) {
            // The client secret and source ID found here is identical to
            // that of the source used to get the redirect URL.
            String clientSecret = intent.getData().getQueryParameter(QUERY_CLIENT_SECRET);
            String sourceId = intent.getData().getQueryParameter(QUERY_SOURCE_ID);
            if (clientSecret != null
                    && sourceId != null
                    && clientSecret.equals(mRedirectSource.getClientSecret())
                    && sourceId.equals(mRedirectSource.getId())) {
                updateSourceList(mRedirectSource);
                mRedirectSource = null;
            }
            mRedirectDialogController.dismissDialog();
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
                                        PaymentConfiguration.getInstance().getPublishableKey());
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
                                mRedirectAdapter.addItem(
                                        source.getStatus(),
                                        sourceCardData.getThreeDSecureStatus(),
                                        source.getId(),
                                        source.getType());
                                // If we need to get 3DS verification for this card, we
                                // first create a 3DS Source.
                                if (SourceCardData.REQUIRED.equals(
                                        sourceCardData.getThreeDSecureStatus())) {

                                    // The card Source can be used to create a 3DS Source
                                    createThreeDSecureSource(source.getId());
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
    void createThreeDSecureSource(String sourceId) {
        // This represents a request for a 3DS purchase of 10.00 euro.
        final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                1000L,
                "EUR",
                getUrl(true),
                sourceId);

        Observable<Source> threeDSecureObservable = Observable.fromCallable(
                new Callable<Source>() {
                    @Override
                    public Source call() throws Exception {
                        return mStripe.createSourceSynchronous(
                                threeDParams,
                                PaymentConfiguration.getInstance().getPublishableKey());
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
        mRedirectSource = source;
        mRedirectDialogController.showDialog(source.getRedirect().getUrl());
    }

    private void updateSourceList(@Nullable Source source) {
        if (source == null) {
            mRedirectAdapter.addItem(
                    "No source found",
                    "Stopped",
                    "Error",
                    "None");
            return;
        }

        mRedirectAdapter.addItem(
                source.getStatus(),
                "complete",
                source.getId(),
                source.getType());
    }

    /**
     * Helper method to determine the return URL we use. This is one way to return basic information
     * to the activity (via the return Intent's host field). Because polling has been deprecated,
     * we no longer use this parameter in the example application, but it is used here to see the
     * relationship with the returned value for any parameters you may want to send.
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
