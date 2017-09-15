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

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSessionData;
import com.stripe.android.model.Customer;
import com.stripe.android.view.PaymentFlowConfig;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import static com.stripe.android.view.PaymentFlowActivity.EVENT_SHIPPING_INFO_PROCESSED;
import static com.stripe.android.view.PaymentFlowActivity.EVENT_SHIPPING_INFO_SUBMITTED;
import static com.stripe.android.view.PaymentFlowActivity.EXTRA_IS_SHIPPING_INFO_VALID;

/**
 * An example activity that handles working with a {@link com.stripe.android.PaymentSession}, allowing you to
 * collect information needed to request payment for the current customer.
 */
public class PaymentSessionActivity extends AppCompatActivity {

    private BroadcastReceiver mBroadcastReceiver;
    private ErrorDialogHandler mErrorDialogHandler;
    private PaymentSession mPaymentSession;
    private ProgressBar mProgressBar;
    private Button mStartPaymentFlowButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_session);
        mProgressBar = findViewById(R.id.customer_progress_bar);
        mProgressBar.setVisibility(View.VISIBLE);
        mStartPaymentFlowButton = findViewById(R.id.btn_start_payment_flow);
        mStartPaymentFlowButton.setEnabled(false);
        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        setupCustomerSession();
        setupPaymentSession();
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                intent = new Intent(EVENT_SHIPPING_INFO_PROCESSED);
                intent.putExtra(EXTRA_IS_SHIPPING_INFO_VALID, true);
                LocalBroadcastManager.getInstance(PaymentSessionActivity.this).sendBroadcast(intent);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(EVENT_SHIPPING_INFO_SUBMITTED));
        mStartPaymentFlowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPaymentSession.presentShippingFlow(new PaymentFlowConfig.Builder().build());
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
                        mStartPaymentFlowButton.setEnabled(true);
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(int errorCode, @Nullable String errorMessage) {
                        mStartPaymentFlowButton.setEnabled(false);
                        mErrorDialogHandler.showError(errorMessage);
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void setupPaymentSession() {
        mPaymentSession = new PaymentSession(this);
        mPaymentSession.init(new PaymentSession.PaymentSessionListener() {
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
                // TODO: update UI
            }
        });
    }

}
