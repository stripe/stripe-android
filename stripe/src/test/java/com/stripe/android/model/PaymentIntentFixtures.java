package com.stripe.android.model;

import java.util.Objects;

public final class PaymentIntentFixtures {
    public static final PaymentIntent PI_REQUIRES_3DS2 = Objects.requireNonNull(
            PaymentIntent.fromString("{\n" +
                    "    \"id\": \"pi_1EceMnCRMbs6FrXfCXdF8dnx\",\n" +
                    "    \"object\": \"payment_intent\",\n" +
                    "    \"amount\": 1000,\n" +
                    "    \"amount_capturable\": 0,\n" +
                    "    \"amount_received\": 0,\n" +
                    "    \"application\": null,\n" +
                    "    \"application_fee_amount\": null,\n" +
                    "    \"canceled_at\": null,\n" +
                    "    \"cancellation_reason\": null,\n" +
                    "    \"capture_method\": \"automatic\",\n" +
                    "    \"charges\": {\n" +
                    "        \"object\": \"list\",\n" +
                    "        \"data\": [],\n" +
                    "        \"has_more\": false,\n" +
                    "        \"total_count\": 0,\n" +
                    "        \"url\": \"/v1/charges?payment_intent=pi_1EceMnCRMbs6FrXfCXdF8dnx\"\n" +
                    "    },\n" +
                    "    \"client_secret\": \"pi_1EceMnCRMbs6FrXfCXdF8dnx_secret_vew0L3IGaO0x9o0eyRMGzKr0k\",\n" +
                    "    \"confirmation_method\": \"automatic\",\n" +
                    "    \"created\": 1558469721,\n" +
                    "    \"currency\": \"usd\",\n" +
                    "    \"customer\": null,\n" +
                    "    \"description\": null,\n" +
                    "    \"invoice\": null,\n" +
                    "    \"last_payment_error\": null,\n" +
                    "    \"livemode\": false,\n" +
                    "    \"metadata\": {},\n" +
                    "    \"next_action\": {\n" +
                    "        \"type\": \"use_stripe_sdk\",\n" +
                    "        \"use_stripe_sdk\": {\n" +
                    "            \"type\": \"stripe_3ds2_fingerprint\",\n" +
                    "            \"three_d_secure_2_source\": \"src_1EceOlCRMbs6FrXf2hqrI1g5\",\n" +
                    "            \"directory_server_name\": \"visa\",\n" +
                    "            \"server_transaction_id\": \"e64bb72f-60ac-4845-b8b6-47cfdb0f73aa\",\n" +
                    "            \"three_ds_method_url\": \"\"\n" +
                    "        }\n" +
                    "    },\n" +
                    "    \"on_behalf_of\": null,\n" +
                    "    \"payment_method\": \"pm_1EceOkCRMbs6FrXft9sFxCTG\",\n" +
                    "    \"payment_method_types\": [\n" +
                    "        \"card\"\n" +
                    "    ],\n" +
                    "    \"receipt_email\": null,\n" +
                    "    \"review\": null,\n" +
                    "    \"shipping\": null,\n" +
                    "    \"source\": null,\n" +
                    "    \"statement_descriptor\": null,\n" +
                    "    \"status\": \"requires_action\",\n" +
                    "    \"transfer_data\": null,\n" +
                    "    \"transfer_group\": null\n" +
                    "}"));

    public static final PaymentIntent.RedirectData REDIRECT_DATA =
            new PaymentIntent.RedirectData("https://example.com",
                    "yourapp://post-authentication-return-url");
}
