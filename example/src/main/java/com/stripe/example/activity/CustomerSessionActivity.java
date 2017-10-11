package com.stripe.example.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.CustomerSession;
import com.stripe.android.model.Customer;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import static com.stripe.android.CustomerSession.EVENT_API_ERROR;
import static com.stripe.android.CustomerSession.EVENT_CUSTOMER_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_CUSTOMER_RETRIEVED;
import static com.stripe.android.CustomerSession.EXTRA_ERROR_CODE;
import static com.stripe.android.CustomerSession.EXTRA_ERROR_MESSAGE;

/**
 * An example activity that handles working with a {@link CustomerSession}, allowing you to
 * add and select sources for the current customer.
 */
public class CustomerSessionActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_SOURCE = 55;

    @Nullable private BroadcastReceiver mCustomerReceiver;
    @Nullable private BroadcastReceiver mErrorReceiver;
    private ErrorDialogHandler mErrorDialogHandler;
    private ProgressBar mProgressBar;
    private Button mSelectSourceButton;
    private TextView mSelectedSourceTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_session);
        setTitle(R.string.customer_payment_data_example);
        mProgressBar = findViewById(R.id.customer_progress_bar);
        mSelectedSourceTextView = findViewById(R.id.tv_customer_default_source_acs);
        mSelectSourceButton = findViewById(R.id.btn_launch_payment_methods_acs);
        mSelectSourceButton.setEnabled(false);
        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        initializeReceivers();
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

        CustomerSession.getInstance().retrieveCurrentCustomer(this);
        mProgressBar.setVisibility(View.VISIBLE);
        mSelectSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchWithCustomer();
            }
        });
    }

    private void initializeReceivers() {
        mCustomerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mSelectSourceButton.setEnabled(true);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        };

        mErrorReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE);
                mSelectSourceButton.setEnabled(false);
                mErrorDialogHandler.showError(errorMessage);
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mErrorReceiver,
                new IntentFilter(EVENT_API_ERROR));
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mCustomerReceiver,
                new IntentFilter(EVENT_CUSTOMER_RETRIEVED));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mErrorReceiver);
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mCustomerReceiver);
    }

    private void launchWithCustomer() {
        Intent payIntent = PaymentMethodsActivity.newIntent(this);
        startActivityForResult(payIntent, REQUEST_CODE_SELECT_SOURCE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == RESULT_OK) {
            String selectedSource = data.getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);
            Source source = Source.fromString(selectedSource);
            // Note: it isn't possible for a null or non-card source to be returned.
            if (source != null && Source.CARD.equals(source.getType())) {
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                mSelectedSourceTextView.setText(buildCardString(cardData));
            }
        }
    }

    private String buildCardString(@NonNull SourceCardData data) {
        return data.getBrand() + getString(R.string.ending_in) + data.getLast4();
    }
}
