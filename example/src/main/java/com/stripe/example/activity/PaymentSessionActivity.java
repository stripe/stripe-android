package com.stripe.example.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.stripe.android.CustomerSession;
import com.stripe.android.PaymentSession;
import com.stripe.android.PaymentSession.ShippingInfoSubmittedListener;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.ShippingMethod;
import com.stripe.example.R;
import com.stripe.example.controller.ErrorDialogHandler;
import com.stripe.example.service.ExampleEphemeralKeyProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * An example activity that handles working with a {@link PaymentSession}.
 */
public class PaymentSessionActivity extends AppCompatActivity {

    PaymentSession mPaymentSession;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_session);
        Button shippingFlowButton = findViewById(R.id.btn_shipping_flow);
        CustomerSession.initCustomerSession(
                new ExampleEphemeralKeyProvider(
                        new ExampleEphemeralKeyProvider.ProgressListener() {
                            @Override
                            public void onStringResponse(String string) {
                                if (string.startsWith("Error: ")) {
                                    new ErrorDialogHandler(getSupportFragmentManager()).showError(string);
                                }
                            }
                        }));
        mPaymentSession = new PaymentSession(this);
        shippingFlowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPaymentSession.launchShippingFlow(new ShippingInfoSubmittedListener() {
                    @Override
                    public void onShippingInfoSubmitted(@NonNull ShippingInformation shippingInformation) {
                        List<ShippingMethod> shippingMethods = new ArrayList<>();
                        ShippingMethod fedEx = new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD");
                        ShippingMethod ups = new ShippingMethod("UPS", "fedex", "Arrives 3-5 days", 599, "USD");
                        ShippingMethod carrierPigeon = new ShippingMethod("Carrier pigeon", "carrier pigeon", "Arrives 2-3 Months", 2999, "USD");
                        shippingMethods.add(fedEx);
                        shippingMethods.add(ups);
                        shippingMethods.add(carrierPigeon);
                        mPaymentSession.onShippingInfoProcessed(shippingInformation, true, "", shippingMethods, carrierPigeon);
                    }
                });
            }
        });
    }
}
