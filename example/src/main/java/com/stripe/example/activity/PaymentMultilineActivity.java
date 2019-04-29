package com.stripe.example.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.jakewharton.rxbinding.view.RxView;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.view.CardMultilineWidget;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.ProgressDialogController;

import java.util.AbstractMap;
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

    private ProgressDialogController mProgressDialogController;
    private ErrorDialogHandler mErrorDialogHandler;

    private CardMultilineWidget mCardMultilineWidget;

    @NonNull private final CompositeSubscription mCompositeSubscription =
            new CompositeSubscription();

    private SimpleAdapter mSimpleAdapter;
    @NonNull private final List<Map<String, String>> mCardSources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_multiline);

        mCardMultilineWidget = findViewById(R.id.card_multiline_widget);

        mProgressDialogController = new ProgressDialogController(getSupportFragmentManager(),
                getResources());

        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());

        final ListView listView = findViewById(R.id.card_list_pma);
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
        final Card card = mCardMultilineWidget.getCard();
        if (card == null) {
            return;
        }

        final Stripe stripe = new Stripe(getApplicationContext());
        final PaymentMethodCreateParams cardSourceParams =
                PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null);
        // Note: using this style of Observable creation results in us having a method that
        // will not be called until we subscribe to it.
        final Observable<PaymentMethod> tokenObservable =
                Observable.fromCallable(
                        new Callable<PaymentMethod>() {
                            @Override
                            public PaymentMethod call() throws Exception {
                                return stripe.createPaymentMethodSynchronous(cardSourceParams,
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
                                mProgressDialogController.show(R.string.progressMessage);
                            }
                        })
                .doOnUnsubscribe(
                        new Action0() {
                            @Override
                            public void call() {
                                mProgressDialogController.dismiss();
                            }
                        })
                .subscribe(
                        new Action1<PaymentMethod>() {
                            @Override
                            public void call(PaymentMethod paymentMethod) {
                                addToList(paymentMethod);
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mErrorDialogHandler.show(throwable.getLocalizedMessage());
                            }
                        }));
    }

    private void addToList(@Nullable PaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod.card == null) {
            return;
        }

        final PaymentMethod.Card paymentMethodCard = paymentMethod.card;
        final String endingIn = getString(R.string.endingIn);
        final AbstractMap<String, String> map = new HashMap<>();
        map.put("last4", endingIn + " " + paymentMethodCard.last4);
        map.put("tokenId", paymentMethod.id);
        mCardSources.add(map);
        mSimpleAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        mCompositeSubscription.unsubscribe();
        super.onDestroy();
    }
}
