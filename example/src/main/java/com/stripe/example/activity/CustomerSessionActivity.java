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
import com.stripe.android.model.GooglePayMethod;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceCardData;
import com.stripe.android.model.StripePaymentSource;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.android.view.PaymentMethodsActivityStarter;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import org.json.JSONException;
import org.json.JSONObject;

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
        mErrorDialogHandler = new ErrorDialogHandler(getSupportFragmentManager());
        CustomerSession.initCustomerSession(
                new ExampleEphemeralKeyProvider(
                    new ExampleEphemeralKeyProvider.ProgressListener() {
                        @Override
                        public void onStringResponse(String string) {
                            if (string.startsWith("Error: ")) {
                                mErrorDialogHandler.show(string);
                            }
                        }
                    }));

        mProgressBar.setVisibility(View.VISIBLE);
        CustomerSession.getInstance().retrieveCurrentCustomer(
                new CustomerSession.CustomerRetrievalListener() {
                    @Override
                    public void onCustomerRetrieved(@NonNull Customer customer) {
                        mSelectSourceButton.setEnabled(true);
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onError(int httpCode, @Nullable String errorMessage,
                                        @Nullable StripeError stripeError) {
                        mSelectSourceButton.setEnabled(false);
                        mErrorDialogHandler.show(errorMessage);
                        mProgressBar.setVisibility(View.INVISIBLE);
                    }
                });

        mSelectSourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchWithCustomer();
            }
        });
    }

    private void launchWithCustomer() {
        final Intent paymentMethodsIntent = new PaymentMethodsActivityStarter(this).newIntent()
                .putExtra(PaymentMethodsActivity.EXTRA_SHOW_GOOGLE_PAY, true);
        this.startActivityForResult(paymentMethodsIntent, REQUEST_CODE_SELECT_SOURCE);

        // new PaymentMethodsActivityStarter(this).startForResult(REQUEST_CODE_SELECT_SOURCE);
    }

    @Nullable
    private String readPaymentObjectType(String selectedPayment) {
        // This should be available through a higher level static
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(selectedPayment);
        } catch (JSONException ignored) {
            jsonObject = null;
        }
        if(jsonObject == null)
            return null;

        return jsonObject.optString(StripePaymentSource.FIELD_OBJECT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_SOURCE && resultCode == RESULT_OK) {
            String selectedPayment = data.getStringExtra(PaymentMethodsActivity.EXTRA_SELECTED_PAYMENT);
            String objectType = readPaymentObjectType(selectedPayment);
            if(objectType.equals(GooglePayMethod.VALUE_GOOGLE_PAY)) {
                mSelectedSourceTextView.setText("Google Pay");
            } else {
                // plain source
                Source source = Source.fromString(selectedPayment);
                if (source != null && Source.CARD.equals(source.getType())) {
                    SourceCardData cardData = (SourceCardData) source.getSourceTypeModel();
                    mSelectedSourceTextView.setText(buildCardString(cardData));
                }
            }
        }
    }

    @NonNull
    private String buildCardString(@NonNull SourceCardData data) {
        return data.getBrand() + getString(R.string.ending_in) + data.getLast4();
    }
}
