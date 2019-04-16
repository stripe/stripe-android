package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

    private static final String RETURN_URL = "stripe://payment_intent_return";

    @NonNull private final CompositeSubscription mCompositeSubscription =
            new CompositeSubscription();
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

        createPaymentIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPaymentIntent();
            }
        });

        mRetrievePaymentIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retrievePaymentIntent();
            }
        });
        mConfirmPaymentIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Card card = mCardInputWidget.getCard();
                if (card != null) {
                    confirmPaymentIntent(card);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        mCompositeSubscription.unsubscribe();
        super.onDestroy();
    }

    @NonNull
    private Map<String, Object> createPaymentIntentParams() {
        final Map<String, Object> params = new HashMap<>();
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
                        mProgressDialogController.show(R.string.creating_payment_intent);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.dismiss();
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
                                    mConfirmPaymentIntent.setEnabled(mClientSecret != null);
                                    mRetrievePaymentIntent.setEnabled(mClientSecret != null);

                                } catch (IOException | JSONException exception) {
                                    Log.e(TAG, exception.toString());
                                }
                            }
                        },
                        new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mErrorDialogHandler.show(throwable.getLocalizedMessage());
                            }
                        }
                );
        mCompositeSubscription.add(subscription);
    }

    private void retrievePaymentIntent() {
        final Observable<PaymentIntent> paymentIntentObservable = Observable.fromCallable(
                new Callable<PaymentIntent>() {
                    @Override
                    public PaymentIntent call() throws Exception {
                        return mStripe.retrievePaymentIntentSynchronous(
                                PaymentIntentParams.createRetrievePaymentIntentParams(mClientSecret),
                                PaymentConfiguration.getInstance().getPublishableKey());
                    }
                });
        final Subscription subscription = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.show(R.string.retrieving_payment_intent);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.dismiss();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()).subscribe(
                        new Action1<PaymentIntent>() {
                            @Override
                            public void call(PaymentIntent paymentIntent) {
                                if (paymentIntent != null) {
                                    mPaymentIntentValue.setText(paymentIntent.toJson().toString());
                                } else {
                                    mPaymentIntentValue.setText(R.string.error_while_retrieving_payment_intent);
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

    private void confirmPaymentIntent(@NonNull final Card card) {
        final SourceParams sourceParams = SourceParams.createCardParams(card);
        final Observable<PaymentIntent> paymentIntentObservable = Observable.fromCallable(
                new Callable<PaymentIntent>() {
                    @Override
                    public PaymentIntent call() throws Exception {
                        final PaymentIntentParams paymentIntentParams = PaymentIntentParams
                                .createConfirmPaymentIntentWithSourceDataParams(
                                        sourceParams, mClientSecret, RETURN_URL);
                        return mStripe.confirmPaymentIntentSynchronous(
                                paymentIntentParams,
                                PaymentConfiguration.getInstance().getPublishableKey());
                    }
                });
        final Subscription subscription = paymentIntentObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.show(R.string.confirming_payment_intent);
                    }
                })
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        mProgressDialogController.dismiss();
                    }
                })
                .subscribe(
                        new Action1<PaymentIntent>() {
                            @Override
                            public void call(@Nullable PaymentIntent paymentIntent) {
                                if (paymentIntent != null) {
                                    mPaymentIntentValue.setText(paymentIntent.toString());

                                    final PaymentIntent.Status status = PaymentIntent.Status
                                            .fromCode(paymentIntent.getStatus());

                                    if (PaymentIntent.Status.RequiresAction == status ||
                                            PaymentIntent.Status.RequiresSourceAction == status) {
                                        startActivity(new Intent(Intent.ACTION_VIEW,
                                                paymentIntent.getRedirectUrl()));
                                    }
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
