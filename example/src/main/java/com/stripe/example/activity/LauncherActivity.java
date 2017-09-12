package com.stripe.example.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.wallet.Cart;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.Address;
import com.stripe.android.model.ShippingMethod;
import com.stripe.android.view.AddAddressActivity;
import com.stripe.android.view.AddAddressWidget;
import com.stripe.android.view.ShippingFlowActivity;
import com.stripe.example.R;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

import java.util.ArrayList;
import java.util.List;

public class LauncherActivity extends AppCompatActivity {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://dashboard.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY =
            "put your key here";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        PaymentConfiguration.init(PUBLISHABLE_KEY);
        PaymentConfiguration.getInstance().setShippingMethods(createSampleShippingMethods());
        Button tokenButton = findViewById(R.id.btn_make_card_tokens);
        tokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, PaymentActivity.class);
                startActivity(intent);
            }
        });

        Button multilineButton = findViewById(R.id.btn_make_card_sources);
        multilineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LauncherActivity.this, PaymentMultilineActivity.class);
                startActivity(intent);
            }
        });

        Button sourceButton = findViewById(R.id.btn_make_sources);
        sourceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LauncherActivity.this, RedirectActivity.class);
                startActivity(intent);
            }
        });

        Button androidPayButton = findViewById(R.id.btn_android_pay_launch);
        androidPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSampleCartAndLaunchAndroidPayActivity();
            }
        });

        Button customerSessionButton = findViewById(R.id.btn_customer_session_launch);
        customerSessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LauncherActivity.this, CustomerSessionActivity.class);
                startActivity(intent);
            }
        });
        
        Button addAddressButton = findViewById(R.id.btn_add_address_launch);
        addAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<String> hiddenFields = new ArrayList<>();
                hiddenFields.add(AddAddressWidget.NAME_FIELD);
                ArrayList<String> optionalFields = new ArrayList<>();
                optionalFields.add(AddAddressWidget.POSTAL_CODE_FIELD);
                Address address = new Address.Builder().setCity("San Francisco").build();
                Intent intent = AddAddressActivity.newIntent(LauncherActivity.this , optionalFields, hiddenFields, address);
                startActivity(intent);
            }
        });

        Button selectShippingAddressButton = findViewById(R.id.btn_select_shipping_method_launch);
        selectShippingAddressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new ShippingFlowActivity.IntentBuilder().build(LauncherActivity.this);
                startActivity(intent);
            }
        });

    }

    private void createSampleCartAndLaunchAndroidPayActivity() {
        AndroidPayConfiguration.init(PUBLISHABLE_KEY, "USD");
        AndroidPayConfiguration androidPayConfiguration = AndroidPayConfiguration.getInstance();
        androidPayConfiguration.setShippingAddressRequired(true);
        CartManager cartManager = new CartManager("USD");
        cartManager.addLineItem("Llama Food", 5000L);
        cartManager.addLineItem("Llama Shoes", 4, 2000L);
        cartManager.addShippingLineItem("Domestic shipping estimate", 1000L);

        try {
            Cart cart = cartManager.buildCart();
            Intent intent = new Intent(this, AndroidPayActivity.class)
                    .putExtra(StripeAndroidPayActivity.EXTRA_CART, cart);
            startActivity(intent);
        } catch (CartContentException unexpected) {
            // Ignore for now.
        }
    }

    private List<ShippingMethod> createSampleShippingMethods() {
        List<ShippingMethod> shippingMethods = new ArrayList<>();
        shippingMethods.add(new ShippingMethod("UPS Ground", "ups-ground", "Arrives in 3-5 days", 0, "USD"));
        shippingMethods.add(new ShippingMethod("FedEx", "fedex", "Arrives tomorrow", 599, "USD"));
        return shippingMethods;
    }
}
