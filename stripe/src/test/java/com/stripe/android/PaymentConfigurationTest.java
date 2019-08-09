package com.stripe.android;

import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Test class for {@link PaymentConfiguration}.
 */
@RunWith(RobolectricTestRunner.class)
public class PaymentConfigurationTest {

    @Before
    public void setup() {
        // Make sure we initialize before each test.
        PaymentConfiguration.clearInstance();
    }

    @Test
    public void getInstance_beforeInit_throwsRuntimeException() {
        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                PaymentConfiguration.getInstance();
            }
        });
    }

    @Test
    public void getInstance_withPublicKey_returnsDefaultInstance() {
        PaymentConfiguration.init(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);
        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                PaymentConfiguration.getInstance().getPublishableKey());
    }
}
