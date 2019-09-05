package com.stripe.android.model;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaymentMethodCreateParamsTest {

    @Test
    public void card_toPaymentMethodParamsCard() {
        final PaymentMethodCreateParams.Card expectedCard =
                new PaymentMethodCreateParams.Card.Builder()
                        .setNumber("4242424242424242")
                        .setCvc("123")
                        .setExpiryMonth(8)
                        .setExpiryYear(2019)
                        .build();
        assertEquals(expectedCard, CardFixtures.CARD.toPaymentMethodParamsCard());
    }

    @Test
    public void createFromGooglePay_withNoBillingAddress() throws JSONException {
        final PaymentMethodCreateParams createdParams =
                PaymentMethodCreateParams.createFromGooglePay(
                        GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_NO_BILLING_ADDRESS);

        final PaymentMethodCreateParams expectedParams = PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.create("tok_1F4ACMCRMbs6FrXf6fPqLnN7"),
                new PaymentMethod.BillingDetails.Builder()
                        .setEmail("")
                        .build()
        );
        assertEquals(expectedParams, createdParams);
    }

    @Test
    public void createFromGooglePay_withFullBillingAddress() throws JSONException {
        final PaymentMethodCreateParams createdParams =
                PaymentMethodCreateParams.createFromGooglePay(
                        GooglePayFixtures.GOOGLE_PAY_RESULT_WITH_FULL_BILLING_ADDRESS);

        final PaymentMethodCreateParams expectedParams = PaymentMethodCreateParams.create(
                PaymentMethodCreateParams.Card.create("tok_1F4VSjBbvEcIpqUbSsbEtBap"),
                new PaymentMethod.BillingDetails.Builder()
                        .setPhone("1-888-555-1234")
                        .setEmail("stripe@example.com")
                        .setName("Stripe Johnson")
                        .setAddress(new Address.Builder()
                                .setLine1("510 Townsend St")
                                .setLine2("")
                                .setCity("San Francisco")
                                .setState("CA")
                                .setPostalCode("94103")
                                .setCountry("US")
                                .build())
                        .build()
        );
        assertEquals(expectedParams, createdParams);
    }

    @Test
    public void equals_withFpx() {
        assertEquals(createFpx(), createFpx());
    }

    @NonNull
    private PaymentMethodCreateParams createFpx() {
        return PaymentMethodCreateParams.create(
                new PaymentMethodCreateParams.Fpx.Builder()
                        .setBank("hsbc")
                        .build(),
                new PaymentMethod.BillingDetails.Builder()
                        .setPhone("1-888-555-1234")
                        .setEmail("stripe@example.com")
                        .setName("Stripe Johnson")
                        .setAddress(new Address.Builder()
                                .setLine1("510 Townsend St")
                                .setLine2("")
                                .setCity("San Francisco")
                                .setState("CA")
                                .setPostalCode("94103")
                                .setCountry("US")
                                .build())
                        .build()
        );
    }
}
