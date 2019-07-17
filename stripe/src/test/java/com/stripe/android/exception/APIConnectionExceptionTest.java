package com.stripe.android.exception;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class APIConnectionExceptionTest {

    @Test
    public void testCreate() {
        final APIConnectionException ex = APIConnectionException.create(
                "https://api.stripe.com/v1/payment_methods",
                new IllegalArgumentException("Invalid id")
        );
        assertEquals("IOException during API request to Stripe " +
                        "(https://api.stripe.com/v1/payment_methods): Invalid id. " +
                        "Please check your internet connection and try again. " +
                        "If this problem persists, you should check Stripe's service " +
                        "status at https://twitter.com/stripestatus, " +
                        "or let us know at support@stripe.com.",
                ex.getMessage());
    }
}