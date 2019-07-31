package com.stripe.android.model;

import java.util.HashMap;
import java.util.Map;

public final class PaymentMethodFixtures {
    public static final PaymentMethod.Card CARD = new PaymentMethod.Card.Builder()
            .setBrand(PaymentMethod.Card.Brand.VISA)
            .setChecks(new PaymentMethod.Card.Checks.Builder()
                    .setAddressLine1Check("unchecked")
                    .setAddressPostalCodeCheck(null)
                    .setCvcCheck("unchecked")
                    .build())
            .setCountry("US")
            .setExpiryMonth(8)
            .setExpiryYear(2022)
            .setFunding("credit")
            .setLast4("4242")
            .setThreeDSecureUsage(new PaymentMethod.Card.ThreeDSecureUsage.Builder()
                    .setSupported(true)
                    .build())
            .setWallet(null)
            .build();

    public static final PaymentMethod.BillingDetails BILLING_DETAILS =
            new PaymentMethod.BillingDetails.Builder()
                    .setAddress(new Address.Builder()
                            .setLine1("510 Townsend St")
                            .setCity("San Francisco")
                            .setState("CA")
                            .setPostalCode("94103")
                            .setCountry("USA")
                            .build())
                    .setEmail("patrick@example.com")
                    .setName("Patrick")
                    .setPhone("123-456-7890")
                    .build();

    public static final PaymentMethod CARD_PAYMENT_METHOD;

    static {
        final Map<String, String> metadata = new HashMap<>();
        metadata.put("order_id", "123456789");

        CARD_PAYMENT_METHOD = new PaymentMethod.Builder()
                .setId("pm_123456789")
                .setCreated(1550757934255L)
                .setLiveMode(true)
                .setType("card")
                .setCustomerId("cus_AQsHpvKfKwJDrF")
                .setBillingDetails(BILLING_DETAILS)
                .setCard(PaymentMethodFixtures.CARD)
                .setMetadata(metadata)
                .build();
    }
}
