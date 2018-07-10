package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.android.model.SourceParams;
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
import java.util.concurrent.Callable;

import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class PaymentIntentActivity extends AppCompatActivity {
    private static final String TAG = PaymentIntentActivity.class.getName();

    private static final String RETURN_SCHEMA = "stripe://payment_intent_return";
    private ProgressDialogController mProgressDialogController;
    private ErrorDialogHandler mErrorDialogHandler;
    private CompositeSubscription mCompositeSubscription;
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
        Button createPaymentIntent = findViewById(R.id.btn_create_payment_intent);
        mRetrievePaymentIntent = findViewById(R.id.btn_retrieve_payment_intent);
        mConfirmPaymentIntent = findViewById(R.id.btn_confirm_payment_intent);
        mPaymentIntentValue = findViewById(R.id.payment_intent_value);
        mCardInputWidget = findViewById(R.id.card_input_widget);
        mProgressDialogController =
                new ProgressDialogController(getSupportFragmentManager());

        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        mCompositeSubscription = new CompositeSubscription();
        mStripe = new Stripe(this);
        Retrofit retrofit = RetrofitFactory.getInstance();
        mStripeService = retrofit.create(StripeService.class);

        createPaymentIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPaymentIntent();
            }
        });

        mRetrievePaymentIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrievePaymentIntent(mClientSecret);
            }
        });
        mConfirmPaymentIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Card card = mCardInputWidget.getCard();
                if (card != null) {
                    confirmPaymentIntent(mClientSecret, card);
                }
            }
        });
    }

    private Map<String, Object> createPaymentIntentParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("allowed_source_types[]", "card");
        params.put("amount", 1000);
        params.put("currency", "usd");
        return params;
    }

    void createPaymentIntent() {
        Subscription subscription = mStripeService.createPaymentIntent(createPaymentIntentParams())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.setMessageResource(R.string.creating_payment_intent);
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
                        // Because we've made the mapping above, we're now subscribing
                        // to the result of creating a 3DS Source
                        new Action1<ResponseBody>() {
                            @Override
                            public void call(ResponseBody responseBody) {
                                try {
                                    JSONObject jsonObject = new JSONObject(responseBody.string());
                                    mClientSecret = jsonObject.optString("secret");
                                    if (mClientSecret != null) {
                                        mConfirmPaymentIntent.setEnabled(true);
                                        mRetrievePaymentIntent.setEnabled(true);
                                    }

                                } catch (IOException | JSONException exception) {
                                    Log.e(TAG, exception.toString());
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mErrorDialogHandler.showError(throwable.getLocalizedMessage());
                            }
                        }
                );
        mCompositeSubscription.add(subscription);
    }

    void retrievePaymentIntent(final String clientSecret) {
        final Observable<PaymentIntent> paymentIntentObservable =
                Observable.fromCallable(
                        new Callable<PaymentIntent>() {
                            @Override
                            public PaymentIntent call() throws Exception {
                                PaymentIntentParams paymentIntentParams = PaymentIntentParams.createRetrievePaymentIntent(clientSecret);
                                return mStripe.retrievePaymentIntentSynchronous(
                                        paymentIntentParams,
                                        PaymentConfiguration.getInstance().getPublishableKey());
                            }
                        });
        Subscription subscription = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.setMessageResource(R.string.retrieving_payment_intent);
                        mProgressDialogController.startProgress();
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.finishProgress();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()).subscribe(
                        // Because we've made the mapping above, we're now subscribing
                        // to the result of creating a 3DS Source
                        new Action1<PaymentIntent>() {
                            @Override
                            public void call(PaymentIntent paymentIntent) {
                                mPaymentIntentValue.setText(paymentIntent.toJson().toString());
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e(TAG, throwable.toString());
                            }
                        }
                );
        mCompositeSubscription.add(subscription);
    }

    void confirmPaymentIntent(final String clientSecret, Card card) {
        final SourceParams sourceParams = SourceParams.createCardParams(card);
        final Observable<PaymentIntent> paymentIntentObservable =
                Observable.fromCallable(
                        new Callable<PaymentIntent>() {
                            @Override
                            public PaymentIntent call() throws Exception {
                                PaymentIntentParams paymentIntentParams =
                                        PaymentIntentParams.createConfirmPaymentIntentWithSourceData(
                                                sourceParams,
                                                clientSecret,
                                                RETURN_SCHEMA);
                                return mStripe.confirmPaymentIntentSynchronous(
                                        paymentIntentParams,
                                        PaymentConfiguration.getInstance().getPublishableKey());
                            }
                        });
        Subscription subscription = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.setMessageResource(R.string.confirming_payment_intent);
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
                        // to the result of creating a 3DS Source
                        new Action1<PaymentIntent>() {
                            @Override
                            public void call(PaymentIntent paymentIntent) {
                                mPaymentIntentValue.setText(paymentIntent.toString());
                                if (paymentIntent.getStatus().equals("requires_source_action")) {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, paymentIntent.getAuthorizationUrl());
                                    startActivity(browserIntent);
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                Log.e(TAG, throwable.toString());
                            }
                        }
                );
        mCompositeSubscription.add(subscription);
    }

}
