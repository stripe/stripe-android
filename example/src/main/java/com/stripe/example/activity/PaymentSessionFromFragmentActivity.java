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
import android.widget.Toast;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.StripeError;
import com.stripe.android.model.Customer;
import com.stripe.android.view.ShippingInfoWidget;
import com.stripe.example.R;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class PaymentSessionFromFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.payment_session_from_fragment_layout);

        setTitle(R.string.launch_payment_session_from_fragment);

        final Fragment newFragment = new PaymentSessionLauncherFragment();

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.root, newFragment, PaymentSessionLauncherFragment.class.getSimpleName())
                .commit();
    }

    public static class PaymentSessionLauncherFragment extends Fragment {

        private PaymentSession mPaymentSession;
        private Button mLaunchButton;

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mPaymentSession = createPaymentSession(createCustomerSession());

            mLaunchButton = Objects.requireNonNull(getView())
                    .findViewById(R.id.launch_payment_session);
            mLaunchButton.setOnClickListener((l) ->
                    mPaymentSession.presentPaymentMethodSelection());
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            return getLayoutInflater().inflate(R.layout.launch_payment_session_fragment, null);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mPaymentSession.handlePaymentData(requestCode, resultCode, data);
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
                                            mLaunchButton.setEnabled(true);
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
    }
}
