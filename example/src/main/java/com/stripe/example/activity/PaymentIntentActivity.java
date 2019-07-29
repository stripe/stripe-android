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

import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.PaymentIntentResult;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.PaymentIntent;
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
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
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
    @Nullable private String mClientSecret;
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
        mErrorDialogHandler = new ErrorDialogHandler(this);
        mStripe = new Stripe(getApplicationContext(),
                PaymentConfiguration.getInstance().getPublishableKey());
        final Retrofit retrofit = RetrofitFactory.getInstance();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mStripe.onPaymentResult(requestCode, data, new ApiResultCallback<PaymentIntentResult>() {
            @Override
            public void onSuccess(@NonNull PaymentIntentResult result) {
                mClientSecret = result.getIntent().getClientSecret();
                displayPaymentIntent(result.getIntent());
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(PaymentIntentActivity.this,
                        "Error: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
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
                .subscribe(this::onCreatedPaymentIntent,
                        throwable -> mErrorDialogHandler.show(throwable.getLocalizedMessage())
                );
        mCompositeDisposable.add(disposable);
    }

    private void onCreatedPaymentIntent(@NonNull ResponseBody responseBody) {
        try {
            final JSONObject jsonObject = new JSONObject(responseBody.string());
            mPaymentIntentValue.setText(jsonObject.toString());
            mClientSecret = jsonObject.getString("secret");
            mConfirmPaymentIntent.setEnabled(mClientSecret != null);
            mRetrievePaymentIntent.setEnabled(mClientSecret != null);
        } catch (IOException | JSONException exception) {
            Log.e(TAG, exception.toString());
        }
    }

    private void retrievePaymentIntent() {
        final Observable<PaymentIntent> paymentIntentObservable = Observable.fromCallable(() ->
                mStripe.retrievePaymentIntentSynchronous(Objects.requireNonNull(mClientSecret)));
        final Disposable disposable = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.retrieving_payment_intent))
                .doOnComplete(() -> mProgressDialogController.dismiss())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        this::displayPaymentIntent,
                        throwable -> Log.e(TAG, throwable.toString())
                );
        mCompositeDisposable.add(disposable);
    }

    private void displayPaymentIntent(@NonNull PaymentIntent paymentIntent) {
        mPaymentIntentValue.setText(new JSONObject(paymentIntent.toMap()).toString());
    }

    private void confirmPaymentIntent(@NonNull final Card card) {
        mStripe.confirmPayment(this,
                ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                        PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null),
                        Objects.requireNonNull(mClientSecret), RETURN_URL));
    }
}
