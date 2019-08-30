package com.stripe.android.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stripe.android.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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

    public static final PaymentMethod FPX_PAYMENT_METHOD =
            new PaymentMethod.Builder()
                    .setId("pm_1F5GlnH8dsfnfKo3gtixzcq0")
                    .setCreated(1565290527L)
                    .setLiveMode(true)
                    .setType("fpx")
                    .setBillingDetails(PaymentMethodFixtures.BILLING_DETAILS)
                    .setFpx(new PaymentMethod.Fpx.Builder()
                            .setBank("hsbc")
                            .setAccountHolderType("individual")
                            .build())
                    .build();

    public static final List<PaymentMethod> CARD_PAYMENT_METHODS =
            Arrays.asList(
                    new PaymentMethod.Builder()
                            .setType("card")
                            .setCreated(1000L)
                            .setId("pm_1000")
                            .setCard(new PaymentMethod.Card.Builder()
                                    .setBrand("visa")
                                    .setLast4("4242")
                                    .build())
                            .build(),
                    new PaymentMethod.Builder()
                            .setType("card")
                            .setCreated(2000L)
                            .setId("pm_2000")
                            .setCard(new PaymentMethod.Card.Builder()
                                    .setBrand("visa")
                                    .setLast4("3063")
                                    .build())
                            .build(),
                    new PaymentMethod.Builder()
                            .setType("card")
                            .setCreated(3000L)
                            .setId("pm_3000")
                            .setCard(new PaymentMethod.Card.Builder()
                                    .setBrand("visa")
                                    .setLast4("3220")
                                    .build())
                            .build()
            );

    @NonNull
    public static PaymentMethod createCard() {
        return createCard(null);
    }

    @NonNull
    public static PaymentMethod createCard(@Nullable Long createdOrigin) {
        final String id = "pm_" + UUID.randomUUID().toString().replace("-", "");
        return new PaymentMethod.Builder()
                .setType("card")
                .setCreated(
                        ThreadLocalRandom.current().nextLong(
                                ObjectUtils.getOrDefault(createdOrigin, 1L),
                                10000000
                        )
                )
                .setId(id)
                .setCard(new PaymentMethod.Card.Builder()
                        .setBrand("visa")
                        .setLast4(Integer.toString(
                                ThreadLocalRandom.current().nextInt(1000, 9999)
                        ))
                        .build())
                .build();
    }

    @NonNull
    public static List<PaymentMethod> createCards(int size) {
        final List<PaymentMethod> paymentMethods = new ArrayList<>(size);

        long origin = 1L;
        for (int i = 0; i < size; i++) {
            final PaymentMethod paymentMethod = createCard(origin);
            paymentMethods.add(paymentMethod);
            origin = Objects.requireNonNull(paymentMethod.created);
        }

        return paymentMethods;
    }

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
