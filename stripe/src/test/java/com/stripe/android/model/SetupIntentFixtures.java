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
    public static final SetupIntent SI_NEXT_ACTION_REDIRECT =
            Objects.requireNonNull(SetupIntent.fromString(SI_NEXT_ACTION_REDIRECT_JSON));
}
