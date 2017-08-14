package com.stripe.example.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.wallet.Cart;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.model.Customer;
import com.stripe.android.view.PaymentMethodsActivity;
import com.stripe.example.R;
import com.stripe.wrap.pay.AndroidPayConfiguration;
import com.stripe.wrap.pay.activity.StripeAndroidPayActivity;
import com.stripe.wrap.pay.utils.CartContentException;
import com.stripe.wrap.pay.utils.CartManager;

public class LauncherActivity extends AppCompatActivity {

    /*
     * Change this to your publishable key.
     *
     * You can get your key here: https://dashboard.stripe.com/account/apikeys
     */
    private static final String PUBLISHABLE_KEY =
            "pk_test_9UVLd6CCQln8IhUSsmRyqQu4";


    static final String EXAMPLE_JSON_SOURCE_CARD_DATA =
            "{\"exp_month\":12,\"exp_year\":2050," +
                    "\"address_line1_check\":\"unchecked\",\"address_zip_check\":" +
                    "\"unchecked\",\"brand\":\"Visa\",\"country\":\"US\",\"cvc_check\"" +
                    ":\"unchecked\",\"funding\":\"credit\",\"last4\":\"4242\",\"three_d_secure\"" +
                    ":\"optional\"}";
    static final String EXAMPLE_JSON_CARD_SOURCE = "{\n"+
            "\"id\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n"+
            "\"object\": \"source\",\n"+
            "\"amount\": 1000,\n"+
            "\"client_secret\": \"src_client_secret_of43INi1HteJwXVe3djAUosN\",\n"+
            "\"created\": 1488499654,\n"+
            "\"currency\": \"usd\",\n"+
            "\"flow\": \"receiver\",\n"+
            "\"livemode\": false,\n"+
            "\"metadata\": {\n"+
            "},\n"+
            "\"owner\": {\n"+
            "\"address\": null,\n"+
            "\"email\": \"jenny.rosen@example.com\",\n"+
            "\"name\": \"Jenny Rosen\",\n"+
            "\"phone\": \"4158675309\",\n"+
            "\"verified_address\": null,\n"+
            "\"verified_email\": null,\n"+
            "\"verified_name\": null,\n"+
            "\"verified_phone\": null\n"+
            "},\n"+
            "\"receiver\": {\n"+
            "\"address\": \"test_1MBhWS3uv4ynCfQXF3xQjJkzFPukr4K56N\",\n"+
            "\"amount_charged\": 0,\n"+
            "\"amount_received\": 0,\n"+
            "\"amount_returned\": 0\n"+
            "},\n"+
            "\"status\": \"pending\",\n"+
            "\"type\": \"card\",\n"+
            "\"usage\": \"single_use\",\n"+
            "\"card\": " + EXAMPLE_JSON_SOURCE_CARD_DATA + "\n"+
            "}";

    private static final String TEST_CUSTOMER_OBJECT =
            "{\n" +
                    "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"src_19t3xKBZqEXluyI4uz2dxAfQ\",\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" + EXAMPLE_JSON_CARD_SOURCE + "\n"+
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 1,\n" +
                    "    \"url\": \"/v1/customers/cus_AQsHpvKfKwJDrF/sources\"\n" +
                    "  }\n" +
                    "}";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        PaymentConfiguration.init(PUBLISHABLE_KEY);
        Button tokenButton = findViewById(R.id.btn_make_card_tokens);
        tokenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Customer customer = Customer.fromString(TEST_CUSTOMER_OBJECT);
                if (customer == null) {
                    return;
                }
                Intent intent = PaymentMethodsActivity.newIntent(LauncherActivity.this, customer);
//                Intent intent = new Intent(LauncherActivity.this, PaymentActivity.class);
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
                Intent intent = new Intent(LauncherActivity.this, CustomerActivty.class);
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
}
