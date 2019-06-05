package com.stripe.example.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionConfig;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.StripeError;
import com.stripe.android.model.Address;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.view.ShippingInfoWidget;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

import static com.stripe.android.PayWithGoogleUtils.getPriceString;
import static com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowExtras.EVENT_SHIPPING_INFO_SUBMITTED;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_DEFAULT_SHIPPING_METHOD;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_IS_SHIPPING_INFO_VALID;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_SHIPPING_INFO_DATA;
import static com.stripe.android.view.PaymentFlowExtras.EXTRA_VALID_SHIPPING_METHODS;

/**
 * An example activity that handles working with a {@link PaymentSession}, allowing you to collect
 * information needed to request payment for the current customer.
 */
public class PaymentSessionActivity extends AppCompatActivity {

    private BroadcastReceiver mBroadcastReceiver;
    private ErrorDialogHandler mErrorDialogHandler;
    private PaymentSession mPaymentSession;
    private ProgressBar mProgressBar;
    private TextView mResultTextView;
    private TextView mResultTitleTextView;
    private Button mSelectPaymentButton;
    private Button mSelectShippingButton;
    private PaymentSessionData mPaymentSessionData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_session);
        mProgressBar = findViewById(R.id.customer_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
        mSelectPaymentButton = findViewById(R.id.btn_select_payment_method_aps);
        mSelectShippingButton = findViewById(R.id.btn_start_payment_flow);
        mErrorDialogHandler = new ErrorDialogHandler(this);
        mResultTitleTextView = findViewById(R.id.tv_payment_session_data_title);
        mResultTextView = findViewById(R.id.tv_payment_session_data);

        // CustomerSession only needs to be initialized once per app.
        final CustomerSession customerSession = setupCustomerSession();
        setupPaymentSession(customerSession);

        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(@NonNull Context context, @NonNull Intent intent) {
                final ShippingInformation shippingInformation = intent
                        .getParcelableExtra(EXTRA_SHIPPING_INFO_DATA);
                final Intent shippingInfoProcessedIntent =
                        new Intent(EVENT_SHIPPING_INFO_PROCESSED);
                if (!isValidShippingInfo(shippingInformation)) {
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
                } else {
                    final ArrayList<ShippingMethod> shippingMethods = createSampleShippingMethods();
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
                    shippingInfoProcessedIntent.putParcelableArrayListExtra(
                            EXTRA_VALID_SHIPPING_METHODS, shippingMethods);
                    shippingInfoProcessedIntent.putExtra(EXTRA_DEFAULT_SHIPPING_METHOD,
                            shippingMethods.get(1));
                }
                localBroadcastManager.sendBroadcast(shippingInfoProcessedIntent);
            }

            private boolean isValidShippingInfo(@NonNull ShippingInformation shippingInfo) {
                return shippingInfo.getAddress() != null &&
                        Locale.US.getCountry().equals(shippingInfo.getAddress().getCountry());
            }
        };
        localBroadcastManager.registerReceiver(mBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED));
        mSelectPaymentButton.setOnClickListener(v ->
                mPaymentSession.presentPaymentMethodSelection(true));
        mSelectShippingButton.setOnClickListener(v -> mPaymentSession.presentShippingFlow());
    }

    @NonNull
    private CustomerSession setupCustomerSession() {
        CustomerSession.initCustomerSession(this,
                new ExampleEphemeralKeyProvider(new ProgressListenerImpl(this)));
        final CustomerSession customerSession = CustomerSession.getInstance();
        customerSession.retrieveCurrentCustomer(
                new InitialCustomerRetrievalListener(this));
        return customerSession;
    }

    private void setupPaymentSession(@NonNull CustomerSession customerSession) {
        mPaymentSession = new PaymentSession(this);
        final boolean paymentSessionInitialized = mPaymentSession.init(
                new PaymentSessionListenerImpl(this, customerSession),
                new PaymentSessionConfig.Builder()
                        .setPrepopulatedShippingInfo(getExampleShippingInfo())
                        .setHiddenShippingInfoFields(ShippingInfoWidget.PHONE_FIELD,
                                ShippingInfoWidget.CITY_FIELD)
                        .build());
        if (paymentSessionInitialized) {
            mPaymentSession.setCartTotal(2000L);
        }
    }

    @NonNull
    private String formatStringResults(@NonNull PaymentSessionData data) {
        final Currency currency = Currency.getInstance("USD");
        final StringBuilder stringBuilder = new StringBuilder();

        if (data.getPaymentMethod() != null) {
            final PaymentMethod paymentMethod = data.getPaymentMethod();
            final PaymentMethod.Card card = paymentMethod.card;

            if (card != null) {
                stringBuilder.append("Payment Info:\n").append(card.brand)
                        .append(" ending in ")
                        .append(card.last4)
                        .append(data.isPaymentReadyToCharge() ? " IS " : " IS NOT ")
                        .append("ready to charge.\n\n");
            }
        }
        if (data.getShippingInformation() != null) {
            stringBuilder.append("Shipping Info: \n");
            stringBuilder.append(data.getShippingInformation());
            stringBuilder.append("\n\n");
        }
        if (data.getShippingMethod() != null) {
            stringBuilder.append("Shipping Method: \n");
            stringBuilder.append(data.getShippingMethod()).append('\n');
            if (data.getShippingTotal() > 0) {
                stringBuilder.append("Shipping total: ")
                        .append(getPriceString(data.getShippingTotal(), currency));
            }
        }

        return stringBuilder.toString();
    }

    @NonNull
    private ArrayList<ShippingMethod> createSampleShippingMethods() {
        final ArrayList<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground",
                "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex",
                "Arrives tomorrow", 599, "USD"));
        return shippingMethods;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPaymentSession.handlePaymentData(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPaymentSession.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @NonNull
    private ShippingInformation getExampleShippingInfo() {
        final Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "(555) 555-5555");
    }

    private void onPaymentSessionDataChanged(@NonNull CustomerSession customerSession,
                                             @NonNull PaymentSessionData data) {
        mPaymentSessionData = data;
        mProgressBar.setVisibility(View.VISIBLE);
        customerSession.retrieveCurrentCustomer(
                new PaymentSessionChangeCustomerRetrievalListener(this));
    }

    private static final class PaymentSessionListenerImpl
            extends PaymentSession.ActivityPaymentSessionListener<PaymentSessionActivity> {
        @NonNull private final CustomerSession mCustomerSession;

        private PaymentSessionListenerImpl(@NonNull PaymentSessionActivity activity,
                                           @NonNull CustomerSession customerSession) {
            super(activity);
            mCustomerSession = customerSession;
        }

        @Override
        public void onCommunicatingStateChanged(boolean isCommunicating) {
            final PaymentSessionActivity activity = getListenerActivity();
            if (activity == null) {
                return;
            }

            activity.mProgressBar
                    .setVisibility(isCommunicating ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public void onError(int errorCode, @NonNull String errorMessage) {
            final PaymentSessionActivity activity = getListenerActivity();
            if (activity == null) {
                return;
            }

            activity.mErrorDialogHandler.show(errorMessage);
        }

        @Override
        public void onPaymentSessionDataChanged(@NonNull PaymentSessionData data) {
            final PaymentSessionActivity activity = getListenerActivity();
            if (activity == null) {
                return;
            }

            activity.onPaymentSessionDataChanged(mCustomerSession, data);
        }
    }

    private static final class InitialCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity> {
        private InitialCustomerRetrievalListener(@NonNull PaymentSessionActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentSessionActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.mProgressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onError(int httpCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentSessionActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.mErrorDialogHandler.show(errorMessage);
            activity.mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private static final class PaymentSessionChangeCustomerRetrievalListener
            extends CustomerSession.ActivityCustomerRetrievalListener<PaymentSessionActivity> {
        private PaymentSessionChangeCustomerRetrievalListener(
                @NonNull PaymentSessionActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final PaymentSessionActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.mProgressBar.setVisibility(View.INVISIBLE);
            activity.mSelectPaymentButton.setEnabled(true);
            activity.mSelectShippingButton.setEnabled(true);

            if (activity.mPaymentSessionData != null) {
                activity.mResultTitleTextView.setVisibility(View.VISIBLE);
                activity.mResultTextView.setText(
                        activity.formatStringResults(activity.mPaymentSessionData));
            }
        }

        @Override
        public void onError(int httpCode, @NonNull String errorMessage,
                            @Nullable StripeError stripeError) {
            final PaymentSessionActivity activity = getActivity();
            if (activity == null) {
                return;
            }

            activity.mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private static final class ProgressListenerImpl
            implements ExampleEphemeralKeyProvider.ProgressListener {
        @NonNull private final WeakReference<PaymentSessionActivity> mActivityRef;

        private ProgressListenerImpl(@NonNull PaymentSessionActivity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onStringResponse(@NonNull String response) {
            final PaymentSessionActivity activity = mActivityRef.get();
            if (activity != null && response.startsWith("Error: ")) {
                activity.mErrorDialogHandler.show(response);
            }
        }
    }
}
