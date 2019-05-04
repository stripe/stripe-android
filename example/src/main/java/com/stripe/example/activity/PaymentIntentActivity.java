package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.view.CardInputWidget;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.example.module.RetrofitFactory;
import com.stripe.example.service.StripeService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;

public class PaymentIntentActivity extends AppCompatActivity {
    private static final String TAG = PaymentIntentActivity.class.getName();

    private static final String RETURN_URL = "stripe://payment_intent_return";

    @NonNull
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private ProgressDialogController mProgressDialogController;
    private ErrorDialogHandler mErrorDialogHandler;
    private Stripe mStripe;
    private StripeService mStripeService;
    private String mClientSecret;
    private Button mConfirmPaymentIntent;
    private Button mRetrievePaymentIntent;
    private CardInputWidget mCardInputWidget;
    private TextView mPaymentIntentValue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_intent_demo);
        final Button createPaymentIntent = findViewById(R.id.btn_create_payment_intent);
        mRetrievePaymentIntent = findViewById(R.id.btn_retrieve_payment_intent);
        mConfirmPaymentIntent = findViewById(R.id.btn_confirm_payment_intent);
        mPaymentIntentValue = findViewById(R.id.payment_intent_value);
        mCardInputWidget = findViewById(R.id.card_input_widget);

        mProgressDialogController = new ProgressDialogController(getSupportFragmentManager(),
                getResources());
        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        mStripe = new Stripe(getApplicationContext());
        Retrofit retrofit = RetrofitFactory.getInstance();
        mStripeService = retrofit.create(StripeService.class);

        createPaymentIntent.setOnClickListener(v -> createPaymentIntent());

        mRetrievePaymentIntent.setOnClickListener(v -> retrievePaymentIntent());
        mConfirmPaymentIntent.setOnClickListener(v -> {
            final Card card = mCardInputWidget.getCard();
            if (card != null) {
                confirmPaymentIntent(card);
            }
        });
    }

    @Override
    protected void onDestroy() {
        mCompositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);

        if (intent.getData() != null && intent.getData().getQuery() != null) {
            Toast.makeText(PaymentIntentActivity.this,
                    "Retrieving PaymentIntent after authorizing",
                    Toast.LENGTH_SHORT)
                    .show();
            mClientSecret = intent.getData().getQueryParameter(
                    "payment_intent_client_secret");
            retrievePaymentIntent();
        }
    }

    @NonNull
    private Map<String, Object> createPaymentIntentParams() {
        final Map<String, Object> params = new HashMap<>();
        params.put("payment_method_types[]", "card");
        params.put("amount", 1000);
        params.put("currency", "usd");
        return params;
    }

    void createPaymentIntent() {
        final Map<String, Object> params = createPaymentIntentParams();
        final Disposable disposable = mStripeService.createPaymentIntent(params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.creating_payment_intent))
                .doOnComplete(() ->
                        mProgressDialogController.dismiss())

                // Because we've made the mapping above, we're now subscribing
                // to the result of creating a 3DS Source
                .subscribe(
                        responseBody -> {
                            try {
                                JSONObject jsonObject = new JSONObject(responseBody.string());
                                mPaymentIntentValue.setText(jsonObject.toString());
                                mClientSecret = jsonObject.optString("secret");
                                mConfirmPaymentIntent.setEnabled(mClientSecret != null);
                                mRetrievePaymentIntent.setEnabled(mClientSecret != null);

                            } catch (IOException | JSONException exception) {
                                Log.e(TAG, exception.toString());
                            }
                        },
                        throwable -> mErrorDialogHandler.show(throwable.getLocalizedMessage())
                );
        mCompositeDisposable.add(disposable);
    }

    private void retrievePaymentIntent() {
        final Observable<PaymentIntent> paymentIntentObservable = Observable.fromCallable(
                () -> mStripe.retrievePaymentIntentSynchronous(
                        PaymentIntentParams
                                .createRetrievePaymentIntentParams(mClientSecret),
                        PaymentConfiguration.getInstance().getPublishableKey()));
        final Disposable disposable = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.retrieving_payment_intent))
                .doOnComplete(() -> mProgressDialogController.dismiss())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        paymentIntent -> mPaymentIntentValue.setText(paymentIntent != null ?
                                paymentIntent.toJson().toString() :
                                getString(R.string.error_while_retrieving_payment_intent)),
                        throwable -> Log.e(TAG, throwable.toString())
                );
        mCompositeDisposable.add(disposable);
    }

    private void confirmPaymentIntent(@NonNull final Card card) {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null);
        final Observable<PaymentIntent> paymentIntentObservable = Observable.fromCallable(
                () -> {
                    final PaymentIntentParams paymentIntentParams = PaymentIntentParams
                            .createConfirmPaymentIntentWithPaymentMethodCreateParams(
                                    paymentMethodCreateParams, mClientSecret, RETURN_URL);
                    return mStripe.confirmPaymentIntentSynchronous(
                            paymentIntentParams,
                            PaymentConfiguration.getInstance().getPublishableKey());
                });

        final Disposable disposable = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.confirming_payment_intent))
                .doOnComplete(() ->
                        mProgressDialogController.dismiss())
                .subscribe(
                        paymentIntent -> {
                            if (paymentIntent != null) {
                                mPaymentIntentValue.setText(paymentIntent.toJson().toString());

                                if (paymentIntent.requiresAction()) {
                                    Toast.makeText(PaymentIntentActivity.this,
                                            "Redirecting to redirect URL",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            paymentIntent.getRedirectUrl()));
                                }
                            }
                        },
                        throwable -> Log.e(TAG, throwable.toString())
                );
        mCompositeDisposable.add(disposable);
    }
}
