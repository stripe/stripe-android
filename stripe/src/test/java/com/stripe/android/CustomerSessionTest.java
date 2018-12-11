package com.stripe.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.stripe.android.exception.APIException;
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;
import com.stripe.android.testharness.JsonTestUtils;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.CardInputTestActivity;
import com.stripe.android.view.PaymentFlowActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.stripe.android.PaymentSession.PAYMENT_SESSION_CONFIG;
import static com.stripe.android.PaymentSession.PAYMENT_SESSION_DATA_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link CustomerSession}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants=BuildConfig.class, sdk = 25)
public class CustomerSessionTest {

    public static final String FIRST_SAMPLE_KEY_RAW = "{\n" +
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

    public static final String SECOND_SAMPLE_KEY_RAW = "{\n" +
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
    public static final String FIRST_TEST_CUSTOMER_OBJECT_WITH_SHIPPING_INFO =
            "{\n" +
                    "  \"id\": \"cus_AQsHpvKfKwJDrF\",\n" +
                    "  \"object\": \"customer\",\n" +
                    "  \"default_source\": \"abc123\",\n" +
                    "  \"shipping\": { \n" +
                    "     \"address\": { \n" +
                    "        \"city\": \"San Francisco\", \n" +
                    "            \"country\": \"US\", \n" +
                    "            \"line1\": \"185 Berry St\", \n" +
                    "            \"line2\": null, \n" +
                    "            \"postal_code\": \"94087\", \n" +
                    "            \"state\": \"CA\" \n" +
                    "                  }, \n" +
                    "     \"name\": \"Kathy\", \n" +
                    "     \"phone\": \"1234567890\" }, \n" +
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
    @Mock CustomerSession.StripeApiProxy mErrorProxy;
    private TestEphemeralKeyProvider mEphemeralKeyProvider;

    private Customer mFirstCustomer;
    private Customer mSecondCustomer;

    private Source mAddedSource;
    private Source mDeletedSource;

    @Mock BroadcastReceiver mBroadcastReceiver;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PaymentConfiguration.init("pk_test_abc123");

        LocalBroadcastManager instance =
                LocalBroadcastManager.getInstance(RuntimeEnvironment.application);
        instance.registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(CustomerSession.ACTION_API_EXCEPTION));

        mFirstCustomer = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
        assertNotNull(mFirstCustomer);
        mSecondCustomer = Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);
        assertNotNull(mSecondCustomer);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        mAddedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        mDeletedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(mAddedSource);
        try {
            when(mStripeApiProxy.retrieveCustomerWithKey(anyString(), anyString()))
                    .thenReturn(mFirstCustomer, mSecondCustomer);
            when(mStripeApiProxy.addCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString(),
                    anyString()))
                    .thenReturn(mAddedSource);
            when(mErrorProxy.addCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString(),
                    anyString()))
                    .thenThrow(new APIException(
                            "The card is invalid",
                            "request_123",
                            404,
                            null));
            when(mStripeApiProxy.deleteCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString()))
                    .thenReturn(mDeletedSource);
            when(mErrorProxy.deleteCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString()))
                    .thenThrow(new APIException(
                            "The card does not exist",
                            "request_123",
                            404,
                            null));
            when(mStripeApiProxy.setDefaultCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString(),
                    anyString()))
                    .thenReturn(mSecondCustomer);

            when(mErrorProxy.setDefaultCustomerSourceWithKey(
                    any(Context.class),
                    anyString(),
                    anyString(),
                    ArgumentMatchers.<String>anyList(),
                    anyString(),
                    anyString(),
                    anyString()))
                    .thenThrow(new APIException("auth error", "reqId", 405, null));
        } catch (StripeException exception) {
            fail("Exception when accessing mock api proxy: " + exception.getMessage());
        }
    }

    @After
    public void teardown() {
        LocalBroadcastManager.getInstance(RuntimeEnvironment.application)
                .unregisterReceiver(mBroadcastReceiver);
        CustomerSession.endCustomerSession();
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_withoutInitializing_throwsException() {
        CustomerSession.clearInstance();
        CustomerSession.getInstance();
        fail("Should not be able to get instance of CustomerSession without initializing");
    }

    @Test
    public void addProductUsageTokenIfValid_whenValid_addsExpectedTokens() {
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);
        CustomerSession.getInstance().addProductUsageTokenIfValid("AddSourceActivity");

        List<String> expectedTokens = new ArrayList<>();
        expectedTokens.add("AddSourceActivity");

        List<String> actualTokens =
                new ArrayList<>(CustomerSession.getInstance().getProductUsageTokens());

        JsonTestUtils.assertListEquals(expectedTokens, actualTokens);

        CustomerSession.getInstance().addProductUsageTokenIfValid("PaymentMethodsActivity");
        expectedTokens.add("PaymentMethodsActivity");

        actualTokens = new ArrayList<>(CustomerSession.getInstance().getProductUsageTokens());
        JsonTestUtils.assertListEquals(expectedTokens, actualTokens);
    }

    @Test
    public void addProductUsageTokenIfValid_whenNotValid_addsNoTokens() {
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);
        CustomerSession.getInstance().addProductUsageTokenIfValid("SomeUnknownActivity");
        JsonTestUtils.assertListEquals(Collections.EMPTY_LIST,
                new ArrayList<>(CustomerSession.getInstance().getProductUsageTokens()));
    }

    @Test
    public void create_withoutInvokingFunctions_fetchesKeyAndCustomer() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
    public void setCustomerShippingInfo_withValidInfo_callsWithExpectedArgs(){
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        Calendar proxyCalendar = Calendar.getInstance();
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                proxyCalendar);
        CustomerSession session = CustomerSession.getInstance();
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");
        Customer customerWithShippingInfo = Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT_WITH_SHIPPING_INFO);
        ShippingInformation shippingInformation = customerWithShippingInfo.getShippingInformation();
        session.setCustomerShippingInformation(RuntimeEnvironment.application, shippingInformation);
        ArgumentCaptor<List> listArgumentCaptor =
                ArgumentCaptor.forClass(List.class);
        try {
            verify(mStripeApiProxy, times(1)).setCustomerShippingInfoWithKey(
                    eq(RuntimeEnvironment.application),
                    eq(mFirstCustomer.getId()),
                    eq("pk_test_abc123"),
                    listArgumentCaptor.capture(),
                    eq(shippingInformation),
                    eq(firstKey.getSecret()));
            List productUsage = listArgumentCaptor.getValue();
            assertTrue(productUsage.contains("PaymentMethodsActivity"));
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }
    }

    @Test
    public void retrieveCustomer_withExpiredCache_updatesCustomer() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        CustomerEphemeralKey secondKey = CustomerEphemeralKey.fromString(SECOND_SAMPLE_KEY_RAW);
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
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
    public void addSourceToCustomer_withUnExpiredCustomer_returnsAddedSourceAndEmptiesLogs() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        session.addProductUsageTokenIfValid("AddSourceActivity");
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        CustomerSession.getInstance().addProductUsageTokenIfValid("AddSourceActivity");
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        session.addCustomerSource(RuntimeEnvironment.application,
                "abc123",
                Source.CARD,
                mockListener);

        assertTrue(CustomerSession.getInstance().getProductUsageTokens().isEmpty());
        ArgumentCaptor<List> listArgumentCaptor =
                ArgumentCaptor.forClass(List.class);
        try {
            verify(mStripeApiProxy, times(1)).addCustomerSourceWithKey(
                    eq(RuntimeEnvironment.application),
                    eq(mFirstCustomer.getId()),
                    eq("pk_test_abc123"),
                    listArgumentCaptor.capture(),
                    eq("abc123"),
                    eq(Source.CARD),
                    eq(firstKey.getSecret()));
            List productUsage = listArgumentCaptor.getValue();
            assertEquals(2, productUsage.size());
            assertTrue(productUsage.contains("AddSourceActivity"));
            assertTrue(productUsage.contains("PaymentMethodsActivity"));
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
    public void addSourceToCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        session.addProductUsageTokenIfValid("AddSourceActivity");
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertFalse(session.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        session.setStripeApiProxy(mErrorProxy);
        session.addCustomerSource(RuntimeEnvironment.application,
                "abc123",
                Source.CARD,
                mockListener);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mBroadcastReceiver).onReceive(
                any(Context.class),
                intentArgumentCaptor.capture());

        Intent captured = intentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);

        verify(mockListener).onError(404, "The card is invalid");
        assertTrue(session.getProductUsageTokens().isEmpty());
    }

    @Test
    public void removeSourceFromCustomer_withUnExpiredCustomer_returnsRemovedSourceAndEmptiesLogs() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        session.addProductUsageTokenIfValid("AddSourceActivity");
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        CustomerSession.getInstance().addProductUsageTokenIfValid("AddSourceActivity");
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        session.deleteCustomerSource(RuntimeEnvironment.application,
                "abc123",
                mockListener);

        assertTrue(CustomerSession.getInstance().getProductUsageTokens().isEmpty());
        ArgumentCaptor<List> listArgumentCaptor =
                ArgumentCaptor.forClass(List.class);
        try {
            verify(mStripeApiProxy, times(1)).deleteCustomerSourceWithKey(
                    eq(RuntimeEnvironment.application),
                    eq(mFirstCustomer.getId()),
                    eq("pk_test_abc123"),
                    listArgumentCaptor.capture(),
                    eq("abc123"),
                    eq(firstKey.getSecret()));
            List productUsage = listArgumentCaptor.getValue();
            assertEquals(2, productUsage.size());
            assertTrue(productUsage.contains("AddSourceActivity"));
            assertTrue(productUsage.contains("PaymentMethodsActivity"));
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
    public void removeSourceFromCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        session.addProductUsageTokenIfValid("AddSourceActivity");
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertFalse(session.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        session.setStripeApiProxy(mErrorProxy);
        session.deleteCustomerSource(RuntimeEnvironment.application,
                "abc123",
                mockListener);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mBroadcastReceiver).onReceive(
                any(Context.class),
                intentArgumentCaptor.capture());

        Intent captured = intentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);

        verify(mockListener).onError(404, "The card does not exist");
        assertTrue(session.getProductUsageTokens().isEmpty());
    }

    @Test
    public void setDefaultSourceForCustomer_withUnExpiredCustomer_returnsCustomerAndClearsLog() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertFalse(session.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        session.setCustomerDefaultSource(
                RuntimeEnvironment.application,
                "abc123",
                Source.CARD,
                mockListener);

        assertTrue(session.getProductUsageTokens().isEmpty());
        ArgumentCaptor<List> listArgumentCaptor =
                ArgumentCaptor.forClass(List.class);
        try {
            verify(mStripeApiProxy, times(1)).setDefaultCustomerSourceWithKey(
                    eq(RuntimeEnvironment.application),
                    eq(mFirstCustomer.getId()),
                    eq("pk_test_abc123"),
                    listArgumentCaptor.capture(),
                    eq("abc123"),
                    eq(Source.CARD),
                    eq(firstKey.getSecret()));
            List productUsage = listArgumentCaptor.getValue();
            assertEquals(1, productUsage.size());
            assertTrue(productUsage.contains("PaymentMethodsActivity"));
        } catch (StripeException unexpected) {
            fail(unexpected.getMessage());
        }

        ArgumentCaptor<Customer> customerArgumentCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(mockListener, times(1)).onCustomerRetrieved(customerArgumentCaptor.capture());
        Customer capturedCustomer = customerArgumentCaptor.getValue();
        assertNotNull(capturedCustomer);
        assertEquals(mSecondCustomer.getId(), capturedCustomer.getId());
    }

    @Test
    public void setDefaultSourceForCustomer_whenApiThrows_tellsListenerBroadcastsAndClearsLogs() {
        CustomerEphemeralKey firstKey = CustomerEphemeralKey.fromString(FIRST_SAMPLE_KEY_RAW);
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
        session.addProductUsageTokenIfValid("PaymentMethodsActivity");

        long firstCustomerCacheTime = session.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        session.setStripeApiProxy(mErrorProxy);
        session.setCustomerDefaultSource(
                RuntimeEnvironment.application,
                "abc123",
                Source.CARD,
                mockListener);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mBroadcastReceiver).onReceive(
                any(Context.class),
                intentArgumentCaptor.capture());

        Intent captured = intentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);
        verify(mockListener).onError(405, "auth error");
        assertTrue(session.getProductUsageTokens().isEmpty());
    }

    @Test
    public void shippingInfoScreen_whenLaunched_logs() {
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, new PaymentSessionData());
        Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        List actualTokens = new ArrayList<>(CustomerSession.getInstance().getProductUsageTokens());
        assertTrue(actualTokens.contains("ShippingInfoScreen"));
    }

    @Test
    public void shippingMethodScreen_whenLaunched_logs() {
        CustomerSession.initCustomerSession(
                mEphemeralKeyProvider,
                mStripeApiProxy,
                null);
        Intent intent = new Intent();
        PaymentSessionConfig paymentSessionConfig = new PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .build();
        intent.putExtra(PAYMENT_SESSION_CONFIG, paymentSessionConfig);
        intent.putExtra(PAYMENT_SESSION_DATA_KEY, new PaymentSessionData());
        Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        List actualTokens = new ArrayList<>(CustomerSession.getInstance().getProductUsageTokens());
        assertTrue(actualTokens.contains("ShippingMethodScreen"));
    }
}
