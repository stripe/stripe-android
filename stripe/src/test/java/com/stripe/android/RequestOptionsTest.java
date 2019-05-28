package com.stripe.android;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RequestOptionsTest {

    @Test
    public void testCreateForApi() {
        final RequestOptions opts = RequestOptions.createForApi("key", "account");
        assertEquals(RequestOptions.RequestType.API, opts.getRequestType());
        assertEquals("key", opts.getPublishableApiKey());
        assertEquals("account", opts.getStripeAccount());
        assertNull(opts.getGuid());
    }

    @Test
    public void testCreateForFingerprinting() {
        final RequestOptions opts = RequestOptions.createForFingerprinting("guid");
        assertEquals(RequestOptions.RequestType.FINGERPRINTING, opts.getRequestType());
        assertNull(opts.getPublishableApiKey());
        assertNull(opts.getStripeAccount());
        assertEquals("guid", opts.getGuid());
    }
}