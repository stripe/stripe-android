package com.stripe.android;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class RequestOptionsTest {

    @Test
    public void testCreateForApi() {
        final RequestOptions opts = RequestOptions.createForApi("key", "account");
        assertEquals(RequestOptions.RequestType.API, opts.getRequestType());
        assertEquals("key", opts.getApiKey());
        assertEquals("account", opts.getStripeAccount());
        assertNull(opts.getGuid());
    }

    @Test
    public void testCreateForFingerprinting() {
        final RequestOptions opts = RequestOptions.createForFingerprinting("guid");
        assertEquals(RequestOptions.RequestType.FINGERPRINTING, opts.getRequestType());
        assertNull(opts.getApiKey());
        assertNull(opts.getStripeAccount());
        assertEquals("guid", opts.getGuid());
    }

    @Test
    public void testCreateForApi_withSecretKey_throwsException() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() {
                        RequestOptions.createForApi("sk_test");
                    }
                });
    }
}