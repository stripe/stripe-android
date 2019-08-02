package com.stripe.example.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.PaymentIntentResult;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.model.ConfirmPaymentIntentParams;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentIntent;
import com.stripe.android.view.ShippingInfoWidget;
import com.stripe.example.R;
import com.stripe.example.module.RetrofitFactory;
import com.stripe.example.service.ExampleEphemeralKeyProvider;
import com.stripe.example.service.StripeService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class FragmentExampesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fragments_example_layout);

        setTitle(R.string.launch_payment_session_from_fragment);

        final Fragment newFragment = new LauncherFragment();

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.root, newFragment, LauncherFragment.class.getSimpleName())
                .commit();
    }

    public static class LauncherFragment extends Fragment {
        private static final String PAYMENT_METHOD_3DS2_REQUIRED = "pm_card_threeDSecure2Required";
        private static final String RETURN_URL = "stripe://payment_auth";

        @NonNull
        private final CompositeDisposable mCompositeSubscription = new CompositeDisposable();

        private Stripe mStripe;
        private PaymentSession mPaymentSession;
        private StripeService mStripeService;

        private ProgressBar mProgressBar;
        private Button mLaunchPaymentSessionButton;
        private Button mLaunchPaymentAuthButton;
        private TextView mStatusTextView;

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            mStripeService = RetrofitFactory.getInstance().create(StripeService.class);
            mStripe = new Stripe(requireContext(),
                    PaymentConfiguration.getInstance().getPublishableKey());
            mPaymentSession = createPaymentSession(createCustomerSession());

            final View rootView = Objects.requireNonNull(getView());
            mStatusTextView = rootView.findViewById(R.id.status);
            mProgressBar = rootView.findViewById(R.id.progress_bar);

            mLaunchPaymentSessionButton = rootView.findViewById(R.id.launch_payment_session);
            mLaunchPaymentSessionButton.setOnClickListener((l) ->
                    mPaymentSession.presentPaymentMethodSelection());

            mLaunchPaymentAuthButton = rootView.findViewById(R.id.launch_payment_auth);
            mLaunchPaymentAuthButton.setOnClickListener((l) -> createPaymentIntent());
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return getLayoutInflater().inflate(R.layout.launch_payment_session_fragment, null);
        }

        @Override
        public void onPause() {
            mProgressBar.setVisibility(View.INVISIBLE);
            super.onPause();
        }

        @Override
        public void onDestroy() {
            mCompositeSubscription.dispose();
            super.onDestroy();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            mProgressBar.setVisibility(View.VISIBLE);

            final boolean isPaymentSessionResult =
                    mPaymentSession.handlePaymentData(requestCode, resultCode, data);
            if (isPaymentSessionResult) {
                Toast.makeText(
                        requireActivity(),
                        "Received PaymentSession result",
                        Toast.LENGTH_SHORT)
                        .show();
                return;
            }

            final boolean isPaymentResult = mStripe.onPaymentResult(requestCode, data,
                    new AuthResultListener(this));
        }

        private void createPaymentIntent() {
            mCompositeSubscription.add(
                    mStripeService.createPaymentIntent(createPaymentIntentParams())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doOnSubscribe((d) -> {
                                mProgressBar.setVisibility(View.VISIBLE);
                                mLaunchPaymentAuthButton.setEnabled(false);
                                mStatusTextView.setText(R.string.creating_payment_intent);
                            })
                            .subscribe(this::handleCreatePaymentIntentResponse));
        }

        private void onAuthComplete() {
            mLaunchPaymentAuthButton.setEnabled(true);
            mProgressBar.setVisibility(View.INVISIBLE);
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

        private void confirmPaymentIntent(@NonNull String paymentIntentClientSecret) {
            mStatusTextView.append("\n\nStarting payment authentication");
            mStripe.confirmPayment(this,
                    ConfirmPaymentIntentParams.createWithPaymentMethodId(
                            PAYMENT_METHOD_3DS2_REQUIRED,
                            paymentIntentClientSecret,
                            RETURN_URL));
        }

        @NonNull
        private PaymentSession createPaymentSession(
                @NonNull final CustomerSession customerSession) {
            final PaymentSession paymentSession = new PaymentSession(this);
            final boolean paymentSessionInitialized = paymentSession.init(
                    new PaymentSession.PaymentSessionListener() {
                        @Override
                        public void onCommunicatingStateChanged(boolean isCommunicating) {

                        }

                        @Override
                        public void onError(int errorCode, @NonNull String errorMessage) {

                        }

                        @Override
                        public void onPaymentSessionDataChanged(@NonNull PaymentSessionData data) {
                            customerSession.retrieveCurrentCustomer(
                                    new CustomerSession.CustomerRetrievalListener() {
                                        @Override
                                        public void onCustomerRetrieved(
                                                @NonNull Customer customer) {
                                            mLaunchPaymentSessionButton.setEnabled(true);
                                        }

                                        @Override
                                        public void onError(int errorCode,
                                                            @NonNull String errorMessage,
                                                            @Nullable StripeError stripeError) {

                                        }
                                    });
                        }
                    },
                    new PaymentSessionConfig.Builder()
                            .setHiddenShippingInfoFields(
                                    ShippingInfoWidget.CustomizableShippingField.PHONE_FIELD,
                                    ShippingInfoWidget.CustomizableShippingField.CITY_FIELD
                            )
                            .build());
            if (paymentSessionInitialized) {
                paymentSession.setCartTotal(2000L);
            }

            return paymentSession;
        }

        @NonNull
        private CustomerSession createCustomerSession() {
            CustomerSession.initCustomerSession(requireContext(),
                    new ExampleEphemeralKeyProvider(new ProgressListenerImpl(requireActivity())));
            final CustomerSession customerSession = CustomerSession.getInstance();
            customerSession.retrieveCurrentCustomer(
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {

                        }

                        @Override
                        public void onError(int errorCode, @NonNull String errorMessage,
                                            @Nullable StripeError stripeError) {

                        }
                    }
            );
            return customerSession;
        }


        private static final class ProgressListenerImpl
                implements ExampleEphemeralKeyProvider.ProgressListener {
            @NonNull private final WeakReference<Activity> mActivityRef;

            private ProgressListenerImpl(@NonNull Activity activity) {
                mActivityRef = new WeakReference<>(activity);
            }

            @Override
            public void onStringResponse(@NonNull String response) {
                final Activity activity = mActivityRef.get();
                if (activity != null && response.startsWith("Error: ")) {
                    Toast.makeText(activity, response, Toast.LENGTH_LONG).show();
                }
            }
        }


        private static class AuthResultListener implements ApiResultCallback<PaymentIntentResult> {
            @NonNull private final WeakReference<LauncherFragment> mFragmentRef;

            private AuthResultListener(@NonNull LauncherFragment fragment) {
                this.mFragmentRef = new WeakReference<>(fragment);
            }

            @Override
            public void onSuccess(@NonNull PaymentIntentResult paymentIntentResult) {
                final LauncherFragment fragment = mFragmentRef.get();
                if (fragment == null) {
                    return;
                }

                final PaymentIntent paymentIntent = paymentIntentResult.getIntent();
                fragment.mStatusTextView.append("\n\n" +
                        "Auth status: " + paymentIntentResult.getStatus() + "\n\n" +
                        fragment.getString(R.string.payment_intent_status,
                                paymentIntent.getStatus()));
                fragment.onAuthComplete();
            }

            @Override
            public void onError(@NonNull Exception e) {
                final LauncherFragment fragment = mFragmentRef.get();
                if (fragment == null) {
                    return;
                }

                fragment.mStatusTextView.append("\n\nException: " + e.getMessage());
                fragment.onAuthComplete();
            }
        }
    }
}
