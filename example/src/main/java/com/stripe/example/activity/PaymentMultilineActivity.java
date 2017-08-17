package com.stripe.example.activity;

import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.SourceParams;
import com.stripe.android.view.CardMultilineWidget;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.ProgressDialogController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PaymentMultilineActivity extends AppCompatActivity {

    ProgressDialogController mProgressDialogController;
    ErrorDialogHandler mErrorDialogHandler;

    CardMultilineWidget mCardMultilineWidget;
    CompositeSubscription mCompositeSubscription;

    private SimpleAdapter mSimpleAdapter;
    private List<Map<String, String>> mCardSources= new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_multiline);

        mCompositeSubscription = new CompositeSubscription();
        mCardMultilineWidget = findViewById(R.id.card_multiline_widget);

        mProgressDialogController =
                new ProgressDialogController(getSupportFragmentManager());

        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());

        ListView listView = findViewById(R.id.card_list_pma);
        mSimpleAdapter = new SimpleAdapter(
                this,
                mCardSources,
                R.layout.list_item_layout,
                new String[]{"last4", "tokenId"},
                new int[]{R.id.last4, R.id.tokenId});

        listView.setAdapter(mSimpleAdapter);
        mCompositeSubscription.add(
                RxView.clicks(findViewById(R.id.save_payment)).subscribe(new Action1<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        saveCard();
                    }
                }));
    }

    private void saveCard() {
        Card card = mCardMultilineWidget.getCard();
        if (card == null) {
            return;
        }

        final Stripe stripe = new Stripe(this);
        final SourceParams cardSourceParams = SourceParams.createCardParams(card);
        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        final Observable<Source> tokenObservable =
                Observable.fromCallable(
                        new Callable<Source>() {
                            @Override
                            public Source call() throws Exception {
                                return stripe.createSourceSynchronous(cardSourceParams,
                                        PaymentConfiguration.getInstance().getPublishableKey());
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
                        new Action1<Source>() {
                            @Override
                            public void call(Source source) {
                                addToList(source);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mErrorDialogHandler.showError(throwable.getLocalizedMessage());
                            }
                        }));
    }

    private void addToList(@Nullable Source source) {
        if (source == null || !Source.CARD.equals(source.getType())) {
            return;
        }
        SourceCardData sourceCardData = (SourceCardData) source.getSourceTypeModel();

        String endingIn = getString(R.string.endingIn);
        Map<String, String> map = new HashMap<>();
        map.put("last4", endingIn + " " + sourceCardData.getLast4());
        map.put("tokenId", source.getId());
        mCardSources.add(map);
        mSimpleAdapter.notifyDataSetChanged();
    }

}
