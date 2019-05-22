package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.Stripe;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.model.PaymentIntentParams;
import com.stripe.example.R;
import com.stripe.example.controller.ProgressDialogController;
import com.stripe.example.module.RetrofitFactory;
import com.stripe.example.service.StripeService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * An example of creating a PaymentIntent, then confirming it with PaymentAuthenticationController.
 */
public class PaymentAuthActivity extends AppCompatActivity {

    /**
     * See https://stripe.com/docs/payments/3d-secure#three-ds-cards for more options.
     */
    private static final String PAYMENT_METHOD_3DS2_REQUIRED = "pm_card_threeDSecure2Required";
    private static final String PAYMENT_METHOD_3DS_REQUIRED = "pm_card_threeDSecureRequired";

    private static final String RETURN_URL = "stripe://payment_auth";

    private static final String STATE_STATUS = "status";

    @NonNull
    private final CompositeDisposable mCompositeSubscription = new CompositeDisposable();

    private Stripe mStripe;
    private StripeService mStripeService;
    private ProgressDialogController mProgressDialogController;
    private TextView mStatusTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_auth);

        mStatusTextView = findViewById(R.id.status);
        if (savedInstanceState != null) {
            mStatusTextView.setText(savedInstanceState.getString(STATE_STATUS));
        }

        mProgressDialogController = new ProgressDialogController(getSupportFragmentManager(),
                getResources());

        mStripeService = RetrofitFactory.getInstance().create(StripeService.class);
        mStripe = new Stripe(this, PaymentConfiguration.getInstance().getPublishableKey());
        findViewById(R.id.buy_button).setOnClickListener((v) -> createPaymentIntent());
    }

    private void confirmPaymentIntent(@NonNull String paymentIntentClientSecret) {
        mStatusTextView.append("\n\nStarting payment authentication");
        mStripe.startPaymentAuth(this,
                PaymentIntentParams.createConfirmPaymentIntentWithPaymentMethodId(
                        PAYMENT_METHOD_3DS2_REQUIRED,
                        paymentIntentClientSecret,
                        RETURN_URL));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mStatusTextView.append("\n\nPayment authentication completed");
        mStripe.onPaymentAuthResult(requestCode, resultCode, data,
                new AuthResultListener(this));
    }

    @Override
    protected void onDestroy() {
        mCompositeSubscription.dispose();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putString(STATE_STATUS, mStatusTextView.getText().toString());
    }

    private void createPaymentIntent() {
        mCompositeSubscription.add(
                mStripeService.createPaymentIntent(createPaymentIntentParams())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe((d) -> {
                            mStatusTextView.setText(R.string.creating_payment_intent);
                            mProgressDialogController.show(R.string.creating_payment_intent);
                        })
                        .doOnComplete(() ->
                                mProgressDialogController.dismiss())
                        .subscribe(this::handleCreatePaymentIntentResponse));
    }

    private void handleCreatePaymentIntentResponse(@NonNull ResponseBody responseBody) {
        try {
            final JSONObject responseData = new JSONObject(responseBody.string());
            mStatusTextView.append("\n\n" +
                    getString(R.string.payment_intent_status,
                            responseData.getString("status")));
            final String secret = responseData.getString("secret");
            confirmPaymentIntent(secret);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
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

    private static class AuthResultListener implements ApiResultCallback<PaymentIntent> {
        @NonNull private final WeakReference<PaymentAuthActivity> mActivityRef;

        private AuthResultListener(@NonNull PaymentAuthActivity activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onSuccess(@NonNull PaymentIntent paymentIntent) {
            final PaymentAuthActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

            activity.mStatusTextView.append("\n\n" +
                    activity.getString(R.string.payment_intent_status, paymentIntent.getStatus()));
        }

        @Override
        public void onError(@NonNull Exception e) {
            final PaymentAuthActivity activity = mActivityRef.get();
            if (activity == null) {
                return;
            }

            activity.mStatusTextView.append("\n\nException: " + e.getMessage());
        }
    }
}
