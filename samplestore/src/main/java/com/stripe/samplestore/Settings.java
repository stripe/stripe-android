package com.stripe.samplestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * See https://github.com/stripe/stripe-android#configuring-the-samplestore-app for instructions
 * on how to configure the app before running it.
 */
final class Settings {
    /**
     * Set to the base URL of your test backend. If you are using
     * <a href="https://github.com/stripe/example-ios-backend">example-ios-backend</a>,
     * the URL will be something like `https://hidden-beach-12345.herokuapp.com/`.
     */
    @NonNull static final String BASE_URL = "put your base url here";

    /**
     * Set to publishable key from https://dashboard.stripe.com/test/apikeys
     */
    @NonNull static final String PUBLISHABLE_KEY = "pk_test_your_key_goes_here";

    /**
     * Optionally, set to a Connect Account id to use for API requests to test Connect
     *
     * See https://dashboard.stripe.com/test/connect/accounts/overview
     */
    @Nullable static final String STRIPE_ACCOUNT_ID = null;
}
