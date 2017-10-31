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
import com.stripe.android.model.Address;
import com.stripe.android.model.Customer;
import com.stripe.android.model.CustomerSource;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.view.ShippingInfoWidget;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

import static com.stripe.android.PayWithGoogleUtils.getPriceString;
import static com.stripe.android.view.PaymentFlowActivity.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowActivity.EVENT_SHIPPING_INFO_SUBMITTED;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_DEFAULT_SHIPPING_METHOD;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_IS_SHIPPING_INFO_VALID;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_SHIPPING_INFO_DATA;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_VALID_SHIPPING_METHODS;

/**
 * An example activity that handles working with a {@link com.stripe.android.PaymentSession}, allowing you to
 * collect information needed to request payment for the current customer.
 */
public class PaymentSessionActivity extends AppCompatActivity {

    private BroadcastReceiver mBroadcastReceiver;
    private Customer mCustomer;
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
        mSelectPaymentButton.setEnabled(false);
        mSelectShippingButton = findViewById(R.id.btn_start_payment_flow);
        mSelectShippingButton.setEnabled(false);
        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        mResultTitleTextView = findViewById(R.id.tv_payment_session_data_title);
        mResultTextView = findViewById(R.id.tv_payment_session_data);
        setupCustomerSession(); // CustomerSession only needs to be initialized once per app.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ShippingInformation shippingInformation = intent.getParcelableExtra(EXTRA_SHIPPING_INFO_DATA);
                Intent shippingInfoProcessedIntent = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
                if (shippingInformation.getAddress() == null || !shippingInformation.getAddress().getCountry().equals(Locale.US.getCountry())) {
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, false);
                } else {
                    ArrayList<ShippingMethod> shippingMethods = createSampleShippingMethods();
                    shippingInfoProcessedIntent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
                    shippingInfoProcessedIntent.putParcelableArrayListExtra(EXTRA_VALID_SHIPPING_METHODS, shippingMethods);
                    shippingInfoProcessedIntent.putExtra(EXTRA_DEFAULT_SHIPPING_METHOD, shippingMethods.get(1));
                }
                LocalBroadcastManager.getInstance(PaymentSessionActivity.this).sendBroadcast(shippingInfoProcessedIntent);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED));
        mSelectPaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPaymentSession.presentPaymentMethodSelection();
            }
        });
        mSelectShippingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPaymentSession.presentShippingFlow();
            }
        });

    }

    private void setupCustomerSession() {
        CustomerSession.initCustomerSession(
                new ExampleEphemeralKeyProvider(
                        new ExampleEphemeralKeyProvider.ProgressListener() {
                            @Override
                            public void onStringResponse(String string) {
                                if (string.startsWith("Error: ")) {
                                    mErrorDialogHandler.showError(string);
                                }
                            }
                        }));
        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mCustomer = customer;
                        mProgressBar.setVisibility(View.INVISIBLE);
                        setupPaymentSession();
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        mCustomer = null;
                        mSelectPaymentButton.setEnabled(false);
                        mSelectShippingButton.setEnabled(false);
                        mErrorDialogHandler.showError(errorMessage);
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void setupPaymentSession() {
        mPaymentSession = new PaymentSession(this);
        boolean paymentSessionInitialized = mPaymentSession.init(new PaymentSession.PaymentSessionListener() {
            @Override
            public void onCommunicatingStateChanged(boolean isCommunicating) {
                if (isCommunicating) {
                    mProgressBar.setVisibility(View.VISIBLE);
                } else {
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onError(int errorCode, @Nullable String errorMessage) {
                mErrorDialogHandler.showError(errorMessage);
            }

            @Override
            public void onPaymentSessionDataChanged(@NonNull PaymentSessionData data) {
                mPaymentSessionData = data;
                checkForCustomerUpdates();
            }
        }, new PaymentSessionConfig.Builder()
                .setPrepopulatedShippingInfo(getExampleShippingInfo())
                .setHiddenShippingInfoFields(ShippingInfoWidget.PHONE_FIELD, ShippingInfoWidget.CITY_FIELD)
                .build());
        if (paymentSessionInitialized) {
            mSelectPaymentButton.setEnabled(true);
            mSelectShippingButton.setEnabled(true);
            mPaymentSession.setCartTotal(2000L);
        }
    }

    private void checkForCustomerUpdates() {
        mProgressBar.setVisibility(View.VISIBLE);
        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mCustomer = customer;
                        mProgressBar.setVisibility(View.INVISIBLE);
                        if (mPaymentSessionData != null) {
                            mResultTitleTextView.setVisibility(View.VISIBLE);
                            mResultTextView.setText(formatStringResults(mPaymentSessionData));
                        }
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private String formatStringResults(PaymentSessionData data) {
        Currency currency = Currency.getInstance("USD");
        StringBuilder stringBuilder = new StringBuilder();

        if (data.getSelectedPaymentMethodId() != null && mCustomer != null) {
            CustomerSource source = mCustomer.getSourceById(data.getSelectedPaymentMethodId());
            if (source != null) {
                Source cardSource = source.asSource();
                stringBuilder.append("Payment Info:\n");
                if (cardSource != null) {
                    SourceCardData scd = (SourceCardData) cardSource.getSourceTypeModel();
                    stringBuilder.append(scd.getBrand())
                            .append(" ending in ")
                            .append(scd.getLast4());
                } else {
                    stringBuilder.append('\n').append(source.toString()).append('\n');
                }
                String isOrNot = data.isPaymentReadyToCharge() ? " IS " : " IS NOT ";
                stringBuilder.append(isOrNot).append("ready to charge.\n\n");
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

    private ArrayList<ShippingMethod> createSampleShippingMethods() {
        ArrayList<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD"));
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

    private ShippingInformation getExampleShippingInfo() {
        Address address = new Address.Builder()
                .setCity("San Francisco")
                .setCountry("US")
                .setLine1("123 Market St")
                .setLine2("#345")
                .setPostalCode("94107")
                .setState("CA")
                .build();
        return new ShippingInformation(address, "Fake Name", "(555) 555-5555");
    }
}
