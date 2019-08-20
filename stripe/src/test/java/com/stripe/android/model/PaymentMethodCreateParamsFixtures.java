package com.stripe.android.model;

import android.support.annotation.NonNull;

import java.util.Map;

public final class PaymentMethodCreateParamsFixtures {
    public static final PaymentMethodCreateParams.Card CARD =
            new PaymentMethodCreateParams.Card.Builder()
                    .setNumber("4242424242424242")
                    .setExpiryMonth(1)
                    .setExpiryYear(2024)
                    .setCvc("111")
                    .build();

    public static final PaymentMethod.BillingDetails BILLING_DETAILS =
            new PaymentMethod.BillingDetails.Builder()
                    .setName("Home")
                    .setEmail("me@example.com")
                    .setPhone("1-800-555-1234")
                    .setAddress(new Address.Builder()
                            .setLine1("123 Main St")
                            .setCity("Los Angeles")
                            .setState("CA")
                            .setCountry("US")
                            .build())
                    .build();

    public static final PaymentMethodCreateParams DEFAULT_CARD =
            PaymentMethodCreateParams.create(
                    CARD,
                    BILLING_DETAILS
            );

    public static final PaymentMethodCreateParams DEFAULT_FPX =
            PaymentMethodCreateParams.create(
                    new PaymentMethodCreateParams.Fpx.Builder()
                            .setBank("hsbc")
                            .build()
            );

    @NonNull
    public static PaymentMethodCreateParams createWith(
            @NonNull Map<String, String> metadata) {
        return PaymentMethodCreateParams.create(
                CARD,
                BILLING_DETAILS,
                metadata
        );
    }

    private PaymentMethodCreateParamsFixtures() {}
}
