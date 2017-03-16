package com.stripe.example.activity;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;
import com.stripe.example.adapter.PollingAdapter;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.ProgressDialogController;

import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PollingActivity extends AppCompatActivity {

    private static final String FUNCTIONAL_SOURCE_PUBLISHABLE_KEY =
            "pk_test_vOo1umqsYxSrP5UXfOeL3ecm";

    private CardInputWidget mCardInputWidget;
    private CompositeSubscription mCompositeSubscription;
    private PollingAdapter mPollingAdapter;
    private ErrorDialogHandler mErrorDialogHandler;
    private ProgressDialogController mProgressDialogController;
    private Stripe mStripe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polling);

        mCompositeSubscription = new CompositeSubscription();
        mCardInputWidget = (CardInputWidget) findViewById(R.id.card_input_widget);
        mErrorDialogHandler = new ErrorDialogHandler(this.getSupportFragmentManager());
        mProgressDialogController = new ProgressDialogController(this.getSupportFragmentManager());
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
                .doOnSubscribe(
                        new Action0() {
                            @Override
                            public void call() {
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
                            @Override
                            public Observable<Source> call(Source source) {
                                if (source == null || !Source.CARD.equals(source.getType())) {
                                    return null;
                                }

                                final SourceParams threeDParams = SourceParams.createThreeDSecureParams(
                                        1000L,
                                        "EUR",
                                        "example://return",
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
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                addNewSource(source);
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

    void addNewSource(Source source) {
        mPollingAdapter.addItem(source.getType(), source.getStatus(), source.getRedirect().getUrl());
    }
}
