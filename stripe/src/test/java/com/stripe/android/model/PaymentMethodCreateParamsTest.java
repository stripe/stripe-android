package com.stripe.android.model;

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
}
