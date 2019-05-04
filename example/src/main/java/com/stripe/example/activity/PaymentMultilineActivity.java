package com.stripe.example.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.jakewharton.rxbinding2.view.RxView;
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

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class PaymentMultilineActivity extends AppCompatActivity {

    private ProgressDialogController mProgressDialogController;
    private ErrorDialogHandler mErrorDialogHandler;

    private CardMultilineWidget mCardMultilineWidget;

    @NonNull private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
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
        mCompositeDisposable.add(
                RxView.clicks(findViewById(R.id.save_payment)).subscribe(aVoid -> saveCard()));
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
                        () -> stripe.createPaymentMethodSynchronous(cardSourceParams,
                                PaymentConfiguration.getInstance().getPublishableKey()));

        mCompositeDisposable.add(tokenObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(
                        (d) -> mProgressDialogController.show(R.string.progressMessage))
                .doOnComplete(
                        () -> mProgressDialogController.dismiss())
                .subscribe(
                        this::addToList,
                        throwable -> mErrorDialogHandler.show(throwable.getLocalizedMessage())));
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
        mCompositeDisposable.dispose();
        super.onDestroy();
    }
}
