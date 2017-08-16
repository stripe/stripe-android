package com.stripe.android;

import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.Source;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.CardInputTestActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.mock;
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

    public static final String FIRST_TEST_CUSTOMER_OBJECT =
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

    public static final String SECOND_TEST_CUSTOMER_OBJECT =
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

    private Source mAddedSource;
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PaymentConfiguration.init("pk_test_abc123");

        mFirstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(mFirstCustomer);
        mSecondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(mSecondCustomer);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        mAddedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(mAddedSource);
        try {
            when(mStripeApiProxy.retrieveCustomerWithKey(anyString(), anyString()))
                    .thenReturn(mFirstCustomer, mSecondCustomer);
            when(mStripeApiProxy.addCustomerSourceWithKey(anyString(), anyString(), anyString()))
                    .thenReturn(mAddedSource);
            when(mStripeApiProxy.setDefaultCustomerSourceWithKey(
                    anyString(), anyString(), anyString()))
                    .thenReturn(mSecondCustomer);
        } catch (StripeException exception) {
            fail("Exception when accessing mock api proxy: " + exception.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_withoutInitializing_throwsException() {
        CustomerSession.clearInstance();
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
    public void retrieveCustomer_withExpiredCache_updatesCustomer() {
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

        assertNotNull(session.getEphemeralKey());
        assertEquals(firstKey.getId(), session.getEphemeralKey().getId());
        assertEquals(firstKey.getCustomerId(), mFirstCustomer.getId());

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        assertEquals(firstExpiryTimeInMillis - 100L, firstCustomerCacheTime);
        long timeForCustomerToExpire = TimeUnit.MINUTES.toMillis(2);

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + timeForCustomerToExpire);
        assertEquals(firstCustomerCacheTime + timeForCustomerToExpire,
                proxyCalendar.getTimeInMillis());

        // We want to make sure that the next ephemeral key will be different.
        mEphemeralKeyProvider.setNextRawEphemeralKey(SECOND_SAMPLE_KEY_RAW);

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);
        session.retrieveCurrentCustomer(mockListener);

        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(mockListener, times(1)).onCustomerRetrieved(customerArgumentCaptor.capture());
        Customer capturedCustomer = customerArgumentCaptor.getValue();
        assertNotNull(capturedCustomer);
        assertEquals(mSecondCustomer.getId(), capturedCustomer.getId());
        assertNotNull(session.getCustomer());
        //  Make sure the value is cached.
        assertEquals(mSecondCustomer.getId(), session.getCustomer().getId());

        try {
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    firstKey.getCustomerId(), firstKey.getSecret());
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    secondKey.getCustomerId(), secondKey.getSecret());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }
    }

    @Test
    public void retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                proxyCalendar);
        CustomerSession session = CustomerSession.getInstance();

        // Make sure we're in a good state and that we have the expected customer
        assertNotNull(session.getCustomer());
        assertEquals(firstKey.getCustomerId(), mFirstCustomer.getId());
        assertEquals(firstKey.getCustomerId(), session.getCustomer().getId());

        try {
            verify(mStripeApiProxy, times(1)).retrieveCustomerWithKey(
                    firstKey.getCustomerId(), firstKey.getSecret());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);
        session.retrieveCurrentCustomer(mockListener);

        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(mockListener, times(1)).onCustomerRetrieved(customerArgumentCaptor.capture());
        Customer capturedCustomer = customerArgumentCaptor.getValue();

        assertNotNull(capturedCustomer);
        assertEquals(mFirstCustomer.getId(), capturedCustomer.getId());
        assertNotNull(session.getCustomer());
        //  Make sure the value is cached.
        assertEquals(mFirstCustomer.getId(), session.getCustomer().getId());
        verifyNoMoreInteractions(mStripeApiProxy);
    }

    @Test
    public void addSourceToCustomer_withUnExpiredCustomer_returnsAddedSource() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                proxyCalendar);
        CustomerSession session = CustomerSession.getInstance();

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        session.addCustomerSource("abc123", mockListener);

        try {
            verify(mStripeApiProxy, times(1)).addCustomerSourceWithKey(
                    mFirstCustomer.getId(), "abc123", firstKey.getSecret());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }

        ArgumentCaptor<Source> sourceArgumentCaptor = ArgumentCaptor.forClass(Source.class);
        verify(mockListener, times(1)).onSourceRetrieved(sourceArgumentCaptor.capture());
        Source capturedSource = sourceArgumentCaptor.getValue();
        assertNotNull(capturedSource);
        assertEquals(mAddedSource.getId(), capturedSource.getId());
    }

    @Test
    public void setDefaultSourceForCustomer_withUnExpiredCustomer_returnsExpectedCustomer() {
        EphemeralKey firstKey = EphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                proxyCalendar);
        CustomerSession session = CustomerSession.getInstance();

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        session.setCustomerDefaultSource("abc123", mockListener);

        try {
            verify(mStripeApiProxy, times(1)).setDefaultCustomerSourceWithKey(
                    mFirstCustomer.getId(), "abc123", firstKey.getSecret());
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }

        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(mockListener, times(1)).onCustomerRetrieved(customerArgumentCaptor.capture());
        Customer capturedCustomer = customerArgumentCaptor.getValue();
        assertNotNull(capturedCustomer);
        assertEquals(mSecondCustomer.getId(), capturedCustomer.getId());
    }
}
