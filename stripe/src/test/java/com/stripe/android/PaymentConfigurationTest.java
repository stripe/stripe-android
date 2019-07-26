package com.stripe.android;

import androidx.test.core.app.ApplicationProvider;

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
                PaymentConfiguration.getInstance(ApplicationProvider.getApplicationContext());
            }
        });
    }

    @Test
    public void getInstance_whenInstanceIsNull_loadsFromPrefs() {
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(),
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY);

        PaymentConfiguration.clearInstance();

        assertEquals(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                PaymentConfiguration
                        .getInstance(ApplicationProvider.getApplicationContext())
                        .getPublishableKey());
    }
}
