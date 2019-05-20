package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.stripe.android.CustomerSession;
import com.stripe.android.StripeError;
import com.stripe.android.model.Customer;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.android.view.PaymentMethodsActivityStarter;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import java.lang.ref.WeakReference;

/**
 * An example activity that handles working with a {@link CustomerSession}, allowing you to
 * add and select sources for the current customer.
 */
public class CustomerSessionActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SELECT_SOURCE = 55;

    private Button mSelectSourceButton;
    private TextView mSelectedSourceTextView;
    private ProgressBar mProgressBar;
    private ErrorDialogHandler mErrorDialogHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_session);
        setTitle(R.string.customer_payment_data_example);
        mProgressBar = findViewById(R.id.customer_progress_bar);
        mSelectedSourceTextView = findViewById(R.id.tv_customer_default_source_acs);
        mSelectSourceButton = findViewById(R.id.btn_launch_payment_methods_acs);
        mSelectSourceButton.setEnabled(false);
        mErrorDialogHandler = new ErrorDialogHandler(this);
        CustomerSession.initCustomerSession(this,
                new ExampleEphemeralKeyProvider(new ProgressListenerImpl(this)));

        mProgressBar.setVisibility(View.VISIBLE);
        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerRetrievalListenerImpl(this));

        mSelectSourceButton.setOnClickListener(v -> launchWithCustomer());
    }

    private void launchWithCustomer() {
        new PaymentMethodsActivityStarter(this).startForResult(REQUEST_CODE_SELECT_SOURCE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == RESULT_OK) {
            final String selectedSource = data
                    .getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);
            Source source = Source.fromString(selectedSource);
            // Note: it isn't possible for a null or non-card source to be returned.
            if (source != null && Source.CARD.equals(source.getType())) {
                SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                mSelectedSourceTextView.setText(buildCardString(cardData));
            }
        }
    }

    @NonNull
    private String buildCardString(@NonNull SourceCardData data) {
        return data.getBrand() + getString(R.string.ending_in) + data.getLast4();
    }

    private void onCustomerRetrieved() {
        mSelectSourceButton.setEnabled(true);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private void onRetrieveError(@Nullable String errorMessage) {
        mSelectSourceButton.setEnabled(false);
        mErrorDialogHandler.show(errorMessage);
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private static final class CustomerRetrievalListenerImpl
            extends CustomerSession.ActivityCustomerRetrievalListener<CustomerSessionActivity> {
        private CustomerRetrievalListenerImpl(@NonNull CustomerSessionActivity activity) {
            super(activity);
        }

        @Override
        public void onCustomerRetrieved(@NonNull Customer customer) {
            final CustomerSessionActivity activity = getActivity();
            if (activity != null) {
                activity.onCustomerRetrieved();
            }
        }

        @Override
        public void onError(int httpCode, @Nullable String errorMessage,
                            @Nullable StripeError stripeError) {
            final CustomerSessionActivity activity = getActivity();
            if (activity != null) {
                activity.onRetrieveError(errorMessage);
            }
        }
    }

    private static final class ProgressListenerImpl
            implements ExampleEphemeralKeyProvider.ProgressListener {

        @NonNull private final WeakReference<CustomerSessionActivity> mActivityRef;

        private ProgressListenerImpl(@NonNull CustomerSessionActivity activity) {
            this.mActivityRef = new WeakReference<>(activity);
        }

        @Override
        public void onStringResponse(@NonNull String response) {
            final CustomerSessionActivity activity = mActivityRef.get();
            if (activity != null && response.startsWith("Error: ")) {
                activity.mErrorDialogHandler.show(response);
            }
        }
    }
}
