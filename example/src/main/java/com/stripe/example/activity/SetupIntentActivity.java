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
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.SetupIntent;
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
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;

public class SetupIntentActivity extends AppCompatActivity {
    private static final String TAG = SetupIntentActivity.class.getName();

    private static final String RETURN_URL = "stripe://setup_intent_return";

    @NonNull
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    private ProgressDialogController mProgressDialogController;
    private ErrorDialogHandler mErrorDialogHandler;
    private Stripe mStripe;
    private StripeService mStripeService;
    private String mClientSecret;
    private Button mCreatePaymentMethod;
    private Button mConfirmSetupIntent;
    private Button mRetrieveSetupIntent;
    private CardInputWidget mCardInputWidget;
    private TextView mSetupIntentValue;
    private PaymentMethod mPaymentMethod;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_intent_demo);
        final Button createSetupIntent = findViewById(R.id.btn_create_setup_intent);
        mCreatePaymentMethod = findViewById(R.id.btn_create_payment_method);
        mRetrieveSetupIntent = findViewById(R.id.btn_retrieve_setup_intent);
        mConfirmSetupIntent = findViewById(R.id.btn_confirm_setup_intent);
        mSetupIntentValue = findViewById(R.id.setup_intent_value);
        mCardInputWidget = findViewById(R.id.card_input_widget);

        mProgressDialogController = new ProgressDialogController(getSupportFragmentManager(),
                getResources());
        mErrorDialogHandler = new ErrorDialogHandler(this);
        mStripe = new Stripe(getApplicationContext(),
                PaymentConfiguration.getInstance().getPublishableKey());
        final Retrofit retrofit = RetrofitFactory.getInstance();
        mStripeService = retrofit.create(StripeService.class);

        createSetupIntent.setOnClickListener(v -> createSetupIntent());
        mCreatePaymentMethod.setOnClickListener(v -> {
            final Card card = mCardInputWidget.getCard();
            if (card != null) {
                createPaymentMethod(card);
            }
        });
        mRetrieveSetupIntent.setOnClickListener(v -> retrieveSetupIntent());
        mConfirmSetupIntent.setOnClickListener(v -> confirmSetupIntent());
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
            Toast.makeText(SetupIntentActivity.this,
                    "Retrieving SetupIntent after authorizing",
                    Toast.LENGTH_SHORT)
                    .show();
            mClientSecret = intent.getData().getQueryParameter(
                    "setup_intent_client_secret");
            retrieveSetupIntent();
        }
    }

    private void createSetupIntent() {
        final Disposable disposable = mStripeService.createSetupIntent(new HashMap<>())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.creating_setup_intent))
                .doOnComplete(() ->
                        mProgressDialogController.dismiss())

                // Because we've made the mapping above, we're now subscribing
                // to the result of creating a 3DS Source
                .subscribe(this::onCreatedSetupIntent,
                        throwable -> mErrorDialogHandler.show(throwable.getLocalizedMessage())
                );
        mCompositeDisposable.add(disposable);
    }

    private void onCreatedSetupIntent(@NonNull ResponseBody responseBody) {
        try {
            final JSONObject jsonObject = new JSONObject(responseBody.string());
            mSetupIntentValue.setText(jsonObject.toString());
            mClientSecret = jsonObject.getString("secret");
            mCreatePaymentMethod.setEnabled(mClientSecret != null);
        } catch (IOException | JSONException exception) {
            Log.e(TAG, exception.toString());
        }
    }

    private void retrieveSetupIntent() {
        final Observable<SetupIntent> setupIntentObservable = Observable.fromCallable(
                () -> mStripe.retrieveSetupIntentSynchronous(mClientSecret));

        final Disposable disposable = setupIntentObservable
                .subscribeOn(Schedulers.io())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.retrieving_setup_intent))
                .doOnComplete(() -> mProgressDialogController.dismiss())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        setupIntent -> mSetupIntentValue.setText(setupIntent != null ?
                                new JSONObject(setupIntent.toMap()).toString() :
                                getString(R.string.error_while_retrieving_setup_intent)),
                        throwable -> Log.e(TAG, throwable.toString())
                );
        mCompositeDisposable.add(disposable);
    }

    private void createPaymentMethod(@NonNull final Card card) {
        final PaymentMethodCreateParams paymentMethodCreateParams =
                PaymentMethodCreateParams.create(card.toPaymentMethodParamsCard(), null);

        final Observable<PaymentMethod> paymentMethodObservable = Observable.fromCallable(
                () -> mStripe.createPaymentMethodSynchronous(paymentMethodCreateParams,
                        PaymentConfiguration.getInstance().getPublishableKey()));

        final Disposable disposable = paymentMethodObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.creating_payment_method))
                .doOnComplete(() ->
                        mProgressDialogController.dismiss())
                .subscribe(
                        paymentMethod -> {
                            if (paymentMethod != null) {
                                mSetupIntentValue.setText(paymentMethod.id);
                                mPaymentMethod = paymentMethod;
                                mConfirmSetupIntent.setEnabled(true);
                                mRetrieveSetupIntent.setEnabled(true);
                            }
                        },
                        throwable -> Log.e(TAG, throwable.toString())
                );
        mCompositeDisposable.add(disposable);
    }

    private void confirmSetupIntent() {
        final Observable<SetupIntent> setupIntentObservable = Observable.fromCallable(
                () -> {
                    final ConfirmSetupIntentParams confirmSetupIntentParams =
                            ConfirmSetupIntentParams.create(
                                    Objects.requireNonNull(mPaymentMethod.id), mClientSecret,
                                    RETURN_URL);
                    return mStripe.confirmSetupIntentSynchronous(confirmSetupIntentParams,
                            PaymentConfiguration.getInstance().getPublishableKey());
                });

        final Disposable disposable = setupIntentObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe((d) ->
                        mProgressDialogController.show(R.string.confirm_setup_intent))
                .doOnComplete(() ->
                        mProgressDialogController.dismiss())
                .subscribe(
                        setupIntent -> {
                            if (setupIntent != null) {
                                mSetupIntentValue
                                        .setText(new JSONObject(setupIntent.toMap()).toString());

                                if (setupIntent.requiresAction()) {
                                    Toast.makeText(SetupIntentActivity.this,
                                            "Redirecting to redirect URL",
                                            Toast.LENGTH_SHORT)
                                            .show();
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            setupIntent.getRedirectUrl()));
                                }
                            }
                        },
                        throwable -> Log.e(TAG, throwable.toString())
                );
        mCompositeDisposable.add(disposable);
    }
}
