package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.Objects;

public class SetupIntentFixtures {

    @NonNull
    public static final String SI_NEXT_ACTION_REDIRECT_JSON = "{\n" +
            "  \"id\": \"seti_1EqTSZGMT9dGPIDGVzCUs6dV\",\n" +
            "  \"object\": \"setup_intent\",\n" +
            "  \"cancellation_reason\": null,\n" +
            "  \"client_secret\": \"seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y\",\n" +
            "  \"created\": 1561677666,\n" +
            "  \"description\": \"a description\",\n" +
            "  \"last_setup_error\": null,\n" +
            "  \"livemode\": false,\n" +
            "  \"next_action\": {\n" +
            "    \"redirect_to_url\": {\n" +
            "      \"return_url\": \"stripe://setup_intent_return\",\n" +
            "      \"url\": \"https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02\"\n" +
            "    },\n" +
            "    \"type\": \"redirect_to_url\"\n" +
            "  },\n" +
            "  \"payment_method\": \"pm_1EqTSoGMT9dGPIDG7dgafX1H\",\n" +
            "  \"payment_method_types\": [\n" +
            "    \"card\"\n" +
            "  ],\n" +
            "  \"status\": \"requires_action\",\n" +
            "  \"usage\": \"off_session\"\n" +
            "}";

    @NonNull
    public static final SetupIntent SI_WITH_LAST_PAYMENT_ERROR = Objects.requireNonNull(
            SetupIntent.fromString("{\n" +
                    "\t\"id\": \"seti_1EqTSZGMT9dGPIDGVzCUs6dV\",\n" +
                    "\t\"object\": \"setup_intent\",\n" +
                    "\t\"cancellation_reason\": null,\n" +
                    "\t\"client_secret\": \"seti_1EqTSZGMT9dGPIDGVzCUs6dV_secret_FL9mS9ILygVyGEOSmVNqHT83rxkqy0Y\",\n" +
                    "\t\"created\": 1561677666,\n" +
                    "\t\"description\": \"a description\",\n" +
                    "\t\"last_setup_error\": {\n" +
                    "\t\t\"code\": \"payment_intent_authentication_failure\",\n" +
                    "\t\t\"doc_url\": \"https://stripe.com/docs/error-codes/payment-intent-authentication-failure\",\n" +
                    "\t\t\"message\": \"The provided PaymentMethod has failed authentication. You can provide payment_method_data or a new PaymentMethod to attempt to fulfill this PaymentIntent again.\",\n" +
                    "\t\t\"payment_method\": {\n" +
                    "\t\t\t\"id\": \"pm_1F7J1bCRMbs6FrXfQKsYwO3U\",\n" +
                    "\t\t\t\"object\": \"payment_method\",\n" +
                    "\t\t\t\"billing_details\": {\n" +
                    "\t\t\t\t\"address\": {\n" +
                    "\t\t\t\t\t\"city\": null,\n" +
                    "\t\t\t\t\t\"country\": null,\n" +
                    "\t\t\t\t\t\"line1\": null,\n" +
                    "\t\t\t\t\t\"line2\": null,\n" +
                    "\t\t\t\t\t\"postal_code\": null,\n" +
                    "\t\t\t\t\t\"state\": null\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"email\": null,\n" +
                    "\t\t\t\t\"name\": null,\n" +
                    "\t\t\t\t\"phone\": null\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"card\": {\n" +
                    "\t\t\t\t\"brand\": \"visa\",\n" +
                    "\t\t\t\t\"checks\": {\n" +
                    "\t\t\t\t\t\"address_line1_check\": null,\n" +
                    "\t\t\t\t\t\"address_postal_code_check\": null,\n" +
                    "\t\t\t\t\t\"cvc_check\": null\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"country\": null,\n" +
                    "\t\t\t\t\"exp_month\": 8,\n" +
                    "\t\t\t\t\"exp_year\": 2020,\n" +
                    "\t\t\t\t\"funding\": \"credit\",\n" +
                    "\t\t\t\t\"generated_from\": null,\n" +
                    "\t\t\t\t\"last4\": \"3220\",\n" +
                    "\t\t\t\t\"three_d_secure_usage\": {\n" +
                    "\t\t\t\t\t\"supported\": true\n" +
                    "\t\t\t\t},\n" +
                    "\t\t\t\t\"wallet\": null\n" +
                    "\t\t\t},\n" +
                    "\t\t\t\"created\": 1565775851,\n" +
                    "\t\t\t\"customer\": null,\n" +
                    "\t\t\t\"livemode\": false,\n" +
                    "\t\t\t\"metadata\": {},\n" +
                    "\t\t\t\"type\": \"card\"\n" +
                    "\t\t},\n" +
                    "\t\t\"type\": \"invalid_request_error\"\n" +
                    "\t},\n" +
                    "\t\"livemode\": false,\n" +
                    "\t\"next_action\": {\n" +
                    "\t\t\"redirect_to_url\": {\n" +
                    "\t\t\t\"return_url\": \"stripe://setup_intent_return\",\n" +
                    "\t\t\t\"url\": \"https://hooks.stripe.com/redirect/authenticate/src_1EqTStGMT9dGPIDGJGPkqE6B?client_secret=src_client_secret_FL9m741mmxtHykDlRTC5aQ02\"\n" +
                    "\t\t},\n" +
                    "\t\t\"type\": \"redirect_to_url\"\n" +
                    "\t},\n" +
                    "\t\"payment_method\": \"pm_1EqTSoGMT9dGPIDG7dgafX1H\",\n" +
                    "\t\"payment_method_types\": [\n" +
                    "\t\t\"card\"\n" +
                    "\t],\n" +
                    "\t\"status\": \"requires_action\",\n" +
                    "\t\"usage\": \"off_session\"\n" +
                    "}")
    );

    @NonNull
    public static final SetupIntent SI_NEXT_ACTION_REDIRECT =
            Objects.requireNonNull(SetupIntent.fromString(SI_NEXT_ACTION_REDIRECT_JSON));
}
