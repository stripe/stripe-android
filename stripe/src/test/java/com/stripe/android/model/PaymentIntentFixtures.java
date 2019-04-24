package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.Objects;

public final class PaymentIntentFixtures {
    @NonNull public static final PaymentIntent PI_REQUIRES_ACTION =
            Objects.requireNonNull(PaymentIntent.fromString("{\n" +
                    "\t\"id\": \"pi_1EZlvVCRMbs6FrXfKpq2xMmy\",\n" +
                    "\t\"object\": \"payment_intent\",\n" +
                    "\t\"amount\": 1000,\n" +
                    "\t\"amount_capturable\": 0,\n" +
                    "\t\"amount_received\": 0,\n" +
                    "\t\"application\": null,\n" +
                    "\t\"application_fee_amount\": null,\n" +
                    "\t\"canceled_at\": null,\n" +
                    "\t\"cancellation_reason\": null,\n" +
                    "\t\"capture_method\": \"automatic\",\n" +
                    "\t\"charges\": {\n" +
                    "\t\t\"object\": \"list\",\n" +
                    "\t\t\"data\": [],\n" +
                    "\t\t\"has_more\": false,\n" +
                    "\t\t\"total_count\": 0,\n" +
                    "\t\t\"url\": \"/v1/charges?payment_intent=pi_1EZlvVCRMbs6FrXfKpq2xMmy\"\n" +
                    "\t},\n" +
                    "\t\"client_secret\": \"pi_1EZlvVCRMbs6FrXfKpq2xMmy_secret_cmhLfbSA54n4\",\n" +
                    "\t\"confirmation_method\": \"automatic\",\n" +
                    "\t\"created\": 1557783797,\n" +
                    "\t\"currency\": \"usd\",\n" +
                    "\t\"customer\": null,\n" +
                    "\t\"description\": null,\n" +
                    "\t\"invoice\": null,\n" +
                    "\t\"last_payment_error\": null,\n" +
                    "\t\"livemode\": false,\n" +
                    "\t\"metadata\": {},\n" +
                    "\t\"next_action\": {\n" +
                    "\t\t\"redirect_to_url\": {\n" +
                    "\t\t\t\"return_url\": \"stripe://deeplink\",\n" +
                    "\t\t\t\"url\": \"https://hooks.stripe.com/3d_secure_2_eap/begin_test/src_1Ecaz6CRMbs6FrXfuYKBRSUG/src_client_secret_F6octeOshkgxT47dr0ZxSZiv\"\n" +
                    "\t\t},\n" +
                    "\t\t\"type\": \"redirect_to_url\"\n" +
                    "\t},\n" +
                    "\t\"on_behalf_of\": null,\n" +
                    "\t\"payment_method\": \"pm_1Ecaz6CRMbs6FrXfQs3wNGwB\",\n" +
                    "\t\"payment_method_types\": [\n" +
                    "\t\t\"card\"\n" +
                    "\t],\n" +
                    "\t\"receipt_email\": null,\n" +
                    "\t\"review\": null,\n" +
                    "\t\"shipping\": null,\n" +
                    "\t\"source\": null,\n" +
                    "\t\"statement_descriptor\": null,\n" +
                    "\t\"status\": \"requires_action\",\n" +
                    "\t\"transfer_data\": null,\n" +
                    "\t\"transfer_group\": null\n" +
                    "}"
            ));

    public static final PaymentIntent.RedirectData REDIRECT_DATA =
            new PaymentIntent.RedirectData("https://example.com",
                    "yourapp://post-authentication-return-url");
}
