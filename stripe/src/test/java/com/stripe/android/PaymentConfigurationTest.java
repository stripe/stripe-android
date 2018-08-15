package com.stripe.android;

import com.stripe.android.model.Address;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for {@link PaymentConfiguration}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class PaymentConfigurationTest {

    @Before
    public void setup() {
        // Make sure we initialize before each test.
        PaymentConfiguration.setInstance(null);
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_beforeInit_throwsRuntimeException() {
        PaymentConfiguration.getInstance();
        fail("Should not be able to get a payment configuration before it has been initialized.");
    }

    @Test
    public void getInstance_withPublicKey_returnsDefaultInstance() {
        PaymentConfiguration.init("pk_test_key");
        PaymentConfiguration payConfig = PaymentConfiguration.getInstance();
        assertEquals("pk_test_key", payConfig.getPublishableKey());
        assertEquals(Address.RequiredBillingAddressFields.NONE, payConfig.getRequiredBillingAddressFields());
    }

    @Test
    public void setValues_setsForSingletonInstance() {
        PaymentConfiguration.init("pk_test_key");
        PaymentConfiguration.getInstance()
                .setRequiredBillingAddressFields(Address.RequiredBillingAddressFields.FULL);

        assertEquals("pk_test_key", PaymentConfiguration.getInstance().getPublishableKey());
        assertEquals(Address.RequiredBillingAddressFields.FULL,
                PaymentConfiguration.getInstance().getRequiredBillingAddressFields());
    }
}
