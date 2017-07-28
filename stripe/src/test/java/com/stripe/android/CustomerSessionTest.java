package com.stripe.android;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.testharness.TestEphemeralKeyProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link CustomerSession}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 25)
public class CustomerSessionTest {

    private static final String FIRST_SAMPLE_KEY_RAW = "{\n" +
            "  \"id\": \"ephkey_123\",\n" +
            "  \"object\": \"ephemeral_key\",\n" +
            "  \"secret\": \"ek_test_123\",\n" +
            "  \"created\": 1501179335,\n" +
            "  \"livemode\": false,\n" +
            "  \"expires\": 1501199335,\n" +
            "  \"associated_objects\": [{\n" +
            "            \"type\": \"customer\",\n" +
            "            \"id\": \"cus_AQsHpvKfKwJDrF\"\n" +
            "            }]\n" +
            "}";

    private static final String SECOND_SAMPLE_KEY_RAW = "{\n" +
            "  \"id\": \"ephkey_ABC\",\n" +
            "  \"object\": \"ephemeral_key\",\n" +
            "  \"secret\": \"ek_test_456\",\n" +
            "  \"created\": 1601189335,\n" +
            "  \"livemode\": false,\n" +
            "  \"expires\": 1601199335,\n" +
            "  \"associated_objects\": [{\n" +
            "            \"type\": \"customer\",\n" +
            "            \"id\": \"cus_abc123\"\n" +
            "            }]\n" +
            "}";

    private static final String FIRST_TEST_CUSTOMER_OBJECT =
            "{\n" +
                    "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"abc123\",\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" +
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 0,\n" +
                    "    \"url\": \"/v1/customers/cus_AQsHpvKfKwJDrF/sources\"\n" +
                    "  }\n" +
                    "}";

    private static final String SECOND_TEST_CUSTOMER_OBJECT =
            "{\n" +
                    "  \"id\": \"cus_ABC123\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"def456\",\n" +
                    "  \"sources\": {\n" +
                    "    \"object\": \"list\",\n" +
                    "    \"data\": [\n" +
                    "\n" +
                    "    ],\n" +
                    "    \"has_more\": false,\n" +
                    "    \"total_count\": 0,\n" +
                    "    \"url\": \"/v1/customers/cus_ABC123/sources\"\n" +
                    "  }\n" +
                    "}";

    @Mock CustomerSession.StripeApiProxy mStripeApiProxy;
    private TestEphemeralKeyProvider mEphemeralKeyProvider;

    private Customer mFirstCustomer;
    private Customer mSecondCustomer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PaymentConfiguration.init("pk_test_abc123");

        mFirstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(mFirstCustomer);
        mSecondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(mSecondCustomer);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        try {
            when(mStripeApiProxy.retrieveCustomerWithKey(anyString(), anyString()))
                    .thenReturn(mFirstCustomer, mSecondCustomer);
        } catch (StripeException exception) {
            fail("Exception when accessing mock api proxy: " + exception.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_withoutInitializing_throwsException() {
        CustomerSession.getInstance();
        fail("Should not be able to get instance of CustomerSession without initializing");
    }

    @Test
    public void create_withoutInvokingFunctions_fetchesKeyAndCustomer() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);
        CustomerSession session = CustomerSession.getInstance();

        assertEquals(firstKey.getId(), session.getEphemeralKey().getId());

        try {
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    firstKey.getCustomerId(), firstKey.getSecret());
            assertNotNull(session.getCustomer());
            assertEquals(mFirstCustomer.getId(), session.getCustomer().getId());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }
    }

    @Test
    public void updateCustomerIfNecessary_withDifferentEphemeralKey_updatesCustomer() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        EphemeralKey secondKey = EphemeralKey.fromString(SECOND_SAMPLE_KEY_RAW);
        assertNotNull(secondKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis - 100L);

        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                proxyCalendar);
        CustomerSession session = CustomerSession.getInstance();

        assertEquals(firstKey.getId(), session.getEphemeralKey().getId());
        assertEquals(firstKey.getCustomerId(), mFirstCustomer.getId());

        mEphemeralKeyProvider.setNextRawEphemeralKey(SECOND_SAMPLE_KEY_RAW);

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        session.getEphemeralKeyManager().updateKeyIfNecessary();
        try {
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    firstKey.getCustomerId(), firstKey.getSecret());
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    secondKey.getCustomerId(), secondKey.getSecret());
            assertNotNull(session.getCustomer());
            assertEquals(mSecondCustomer.getId(), session.getCustomer().getId());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }
    }

    @Test
    public void updateCustomerIfNecessary_withSameEphemeralKey_doesNotUpdateCustomer() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + 100000L);

        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                proxyCalendar);
        CustomerSession session = CustomerSession.getInstance();

        assertEquals(firstKey.getId(), session.getEphemeralKey().getId());
        assertEquals(firstKey.getCustomerId(), mFirstCustomer.getId());

        session.updateCustomerIfNecessary(firstKey);

        try {
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    firstKey.getCustomerId(), firstKey.getSecret());
            verifyNoMoreInteractions(mStripeApiProxy);
            assertNotNull(session.getCustomer());
            assertEquals(mFirstCustomer.getId(), session.getCustomer().getId());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }
    }
}
