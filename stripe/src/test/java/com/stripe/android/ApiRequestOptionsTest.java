package com.stripe.android;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ApiRequestOptionsTest {

    @Test
    public void testCreate() {
        final ApiRequest.Options opts = ApiRequest.Options.create(
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, "account");
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY, opts.apiKey);
        assertEquals("account", opts.stripeAccount);
    }

    @Test
    public void testCreate_withSecretKey_throwsException() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() {
                        ApiRequest.Options.create(ApiKeyFixtures.FAKE_SECRET_KEY);
                    }
                });
    }
}