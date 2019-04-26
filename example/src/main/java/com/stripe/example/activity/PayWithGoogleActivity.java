package com.stripe.example.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.Token;
import com.stripe.example.R;

import java.util.Arrays;

public class PayWithGoogleActivity extends AppCompatActivity {

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 53;

    private View mPayWithGoogleButton;
    private PaymentsClient mPaymentsClient;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay_with_google);
        mPaymentsClient =
                Wallet.getPaymentsClient(this,
                        new Wallet.WalletOptions.Builder()
                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                                .build());

        mProgressBar = findViewById(R.id.pwg_progress_bar);
        mPayWithGoogleButton = findViewById(R.id.btn_buy_pwg);
        mPayWithGoogleButton.setEnabled(false);
        mPayWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payWithGoogle();
            }
        });

        isReadyToPay();
    }

    private void payWithGoogle() {
        AutoResolveHelper.resolveTask(
                mPaymentsClient.loadPaymentData(createPaymentDataRequest()),
                PayWithGoogleActivity.this,
                LOAD_PAYMENT_DATA_REQUEST_CODE);
    }

    private void isReadyToPay() {
        mProgressBar.setVisibility(View.VISIBLE);
        final IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();
        final Task<Boolean> task = mPaymentsClient.isReadyToPay(request);
        task.addOnCompleteListener(new OnCompleteListener<Boolean>() {
            public void onComplete(@NonNull Task<Boolean> task) {
                try {
                    final boolean result = task.getResult(ApiException.class);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    if (result) {
                        Toast.makeText(PayWithGoogleActivity.this, "Ready",
                                Toast.LENGTH_SHORT).show();
                        mPayWithGoogleButton.setEnabled(true);
                    } else {
                        Toast.makeText(PayWithGoogleActivity.this, "No PWG",
                                Toast.LENGTH_SHORT).show();
                        //hide Google as payment option
                    }
                } catch (ApiException exception) {
                    Toast.makeText(PayWithGoogleActivity.this,
                            "Exception: " + exception.getLocalizedMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            switch (resultCode) {
                case Activity.RESULT_OK: {
                    final PaymentData paymentData = PaymentData.getFromIntent(data);
                    if (paymentData == null) {
                        break;
                    }
                    // You can get some data on the user's card, such as the brand and
                    // last 4 digits
                    CardInfo info = paymentData.getCardInfo();
                    // You can also pull the user address from the PaymentData object.
                    UserAddress address = paymentData.getShippingAddress();
                    // This is the raw string version of your Stripe token.
                    String rawToken = paymentData.getPaymentMethodToken().getToken();

                    // Now that you have a Stripe token object, charge that by using the id
                    Token stripeToken = Token.fromString(rawToken);
                    if (stripeToken != null) {
                        // This chargeToken function is a call to your own server, which
                        // should then connect to Stripe's API to finish the charge.
                        // chargeToken(stripeToken.getId());
                        Toast.makeText(PayWithGoogleActivity.this,
                                "Got token " + stripeToken.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                    break;
                }
                case Activity.RESULT_CANCELED: {
                    Toast.makeText(PayWithGoogleActivity.this,
                            "Canceled", Toast.LENGTH_LONG).show();

                    break;
                }
                case AutoResolveHelper.RESULT_ERROR: {
                    final Status status = AutoResolveHelper.getStatusFromIntent(data);
                    final String statusMessage = status != null ? status.getStatusMessage() : "";
                    Toast.makeText(PayWithGoogleActivity.this,
                            "Got error " + statusMessage,
                            Toast.LENGTH_SHORT).show();

                    // Log the status for debugging
                    // Generally there is no need to show an error to
                    // the user as the Google Payment API will do that
                    break;
                }
                default: {
                    break;
                }
            }
        }
    }

    @NonNull
    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(
                        WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey",
                        PaymentConfiguration.getInstance().getPublishableKey())
                .addParameter("stripe:version", "2018-11-08")
                .build();
    }

    @NonNull
    private PaymentDataRequest createPaymentDataRequest() {
        return PaymentDataRequest.newBuilder()
                .setTransactionInfo(
                        TransactionInfo.newBuilder()
                                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                .setTotalPrice("10.00")
                                .setCurrencyCode("USD")
                                .build())
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .setCardRequirements(
                        CardRequirements.newBuilder()
                                .addAllowedCardNetworks(Arrays.asList(
                                        WalletConstants.CARD_NETWORK_AMEX,
                                        WalletConstants.CARD_NETWORK_DISCOVER,
                                        WalletConstants.CARD_NETWORK_VISA,
                                        WalletConstants.CARD_NETWORK_MASTERCARD))
                                .build())
                .setPaymentMethodTokenizationParameters(createTokenizationParameters())
                .build();
    }
}
