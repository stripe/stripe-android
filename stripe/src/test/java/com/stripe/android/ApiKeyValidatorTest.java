package com.stripe.android;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ApiKeyValidatorTest {

    @Test
    public void testPublishableKey() {
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                ApiKeyValidator.get().requireValid(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY));
    }

    @Test
    public void testEphemeralKey() {
        assertEquals("ek_test_123",
                ApiKeyValidator.get().requireValid("ek_test_123"));
    }

    @Test
    public void testSecretKey_throwsException() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() {
                        ApiKeyValidator.get().requireValid("sk_test_123");
                    }
                });
    }

    @Test
    public void testEmpty_throwsException() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    @Override
                    public void run() {
                        ApiKeyValidator.get().requireValid("   ");
                    }
                });
    }

    @Test
    public void testNull_throwsException() {
        assertThrows(IllegalArgumentException.class,
                new ThrowingRunnable() {
                    @SuppressWarnings("ConstantConditions")
                    @Override
                    public void run() {
                        ApiKeyValidator.get().requireValid(null);
                    }
                });
    }
}