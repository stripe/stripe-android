package com.stripe.android.exception;

import com.stripe.android.StripeErrorFixtures;

import org.junit.Test;

import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;

public class InvalidRequestExceptionTest {

    @Test
    public void getStripeError_shouldReturnStripeError() {
        final StripeException stripeException = new InvalidRequestException(null, null, null,
                HttpURLConnection.HTTP_BAD_REQUEST, null, null,
                StripeErrorFixtures.INVALID_REQUEST_ERROR, null);
        assertEquals(StripeErrorFixtures.INVALID_REQUEST_ERROR, stripeException.getStripeError());
    }
}
