package com.stripe.example.activity;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.CustomerSession;
import com.stripe.android.EphemeralKeyProvider;
import com.stripe.android.model.Customer;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.example.R;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import okhttp3.Response;

public class CustomerActivty extends AppCompatActivity {

    private EphemeralKeyProvider mEphemeralKeyProvider;

    private TextView mDebugTextView;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_activty);
        mProgressBar = findViewById(R.id.customer_progress_bar);
        mDebugTextView = findViewById(R.id.customer_debug);

        mEphemeralKeyProvider = new ExampleEphemeralKeyProvider(
                new ExampleEphemeralKeyProvider.ProgressListener() {
                    @Override
                    public void onProgressStart() {
                        mProgressBar.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onProgressStop() {
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onResponse(Response response) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(response.code());
                        if (response.code() == 200) {
                            stringBuilder.append(response.body().toString());
                        } else {
                            stringBuilder.append(response.message());
                        }
                        mDebugTextView.append(stringBuilder.toString());
                    }

                    @Override
                    public void onStringResponse(String string) {
                        mDebugTextView.append(string);
                    }
                });
        CustomerSession.initCustomerSession(mEphemeralKeyProvider);
        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
            @Override
            public void onCustomerRetrieved(@NonNull Customer customer) {
                mDebugTextView.append("\n" + customer.getId());
                launchWithCustomer();

            }

            @Override
            public void onError(int errorCode, @Nullable String errorMessage) {

            }
        });
    }

    private void launchWithCustomer() {
        Intent payIntent = PaymentMethodsActivity.newIntent(this);
        startActivityForResult(payIntent, 55);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 55 && resultCode == RESULT_OK) {
            String selectedSource = data.getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);
            mDebugTextView.setText("");
            mDebugTextView.append(selectedSource);
            CustomerSession.getInstance().setCustomerDefaultSource(selectedSource,
                    new CustomerSession.CustomerRetrievalListener() {
                        @Override
                        public void onCustomerRetrieved(@NonNull Customer customer) {
                            if (customer.getDefaultSource() != null) {
                                mDebugTextView.append("\nNEW DEFAULT IS\n");
                                mDebugTextView.append(customer.getDefaultSource());
                            }
                        }

                        @Override
                        public void onError(int errorCode, @Nullable String errorMessage) {

                        }
                    });
        }
    }
}
