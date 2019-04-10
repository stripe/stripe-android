package com.stripe.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import androidx.test.core.app.ApplicationProvider;

import com.stripe.android.exception.APIConnectionException;
import com.stripe.android.exception.APIException;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.exception.CardException;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;
import com.stripe.android.testharness.JsonTestUtils;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.CardInputTestActivity;
import com.stripe.android.view.PaymentFlowActivity;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link CustomerSession}.
 */
@RunWith(RobolectricTestRunner.class)
public class CustomerSessionTest {

    static final String FIRST_SAMPLE_KEY_RAW = "{\n" +
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
    private static final String FIRST_TEST_CUSTOMER_OBJECT_WITH_SHIPPING_INFO =
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

    static final String SECOND_TEST_CUSTOMER_OBJECT =
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

    private static final Customer FIRST_CUSTOMER =
            Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
    private static final Customer SECOND_CUSTOMER =
            Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);

    @Mock private BroadcastReceiver mBroadcastReceiver;
    @Mock private StripeApiHandler mApiHandler;
    @Mock private ThreadPoolExecutor mThreadPoolExecutor;

    @Captor private ArgumentCaptor<List<String>> mListArgumentCaptor;
    @Captor private ArgumentCaptor<Source> mSourceArgumentCaptor;
    @Captor private ArgumentCaptor<Customer> mCustomerArgumentCaptor;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;

    private TestEphemeralKeyProvider mEphemeralKeyProvider;

    private Source mAddedSource;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @NonNull
    private CustomerEphemeralKey getCustomerEphemeralKey(@NonNull String key) {
        try {
            return CustomerEphemeralKey.fromString(key);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setup()
            throws APIException, APIConnectionException, InvalidRequestException,
            AuthenticationException, CardException {
        MockitoAnnotations.initMocks(this);
        PaymentConfiguration.init("pk_test_abc123");

        LocalBroadcastManager.getInstance(mContext).registerReceiver(
                mBroadcastReceiver,
                new IntentFilter(CustomerSession.ACTION_API_EXCEPTION));

        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(SECOND_CUSTOMER);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        mAddedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(mAddedSource);

        when(mApiHandler.retrieveCustomer(anyString(), anyString()))
                .thenReturn(FIRST_CUSTOMER, SECOND_CUSTOMER);
        when(mApiHandler.addCustomerSource(
                eq(mContext),
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenReturn(mAddedSource);
        when(mApiHandler.deleteCustomerSource(
                eq(mContext),
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenReturn(Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE));
        when(mApiHandler.setDefaultCustomerSource(
                eq(mContext),
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenReturn(SECOND_CUSTOMER);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }
        }).when(mThreadPoolExecutor).execute(any(Runnable.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_withoutInitializing_throwsException() {
        CustomerSession.clearInstance();
        CustomerSession.getInstance();
        fail("Should not be able to get instance of CustomerSession without initializing");
    }

    @Test
    public void addProductUsageTokenIfValid_whenValid_addsExpectedTokens() {
        final CustomerSession customerSession = createCustomerSession(null);
        customerSession.addProductUsageTokenIfValid("AddSourceActivity");

        List<String> expectedTokens = new ArrayList<>();
        expectedTokens.add("AddSourceActivity");

        JsonTestUtils.assertListEquals(expectedTokens,
                new ArrayList<>(customerSession.getProductUsageTokens()));

        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        expectedTokens.add("PaymentMethodsActivity");

        JsonTestUtils.assertListEquals(expectedTokens,
                new ArrayList<>(customerSession.getProductUsageTokens()));
    }

    @Test
    public void addProductUsageTokenIfValid_whenNotValid_addsNoTokens() {
        final CustomerSession customerSession = createCustomerSession(null);
        customerSession.addProductUsageTokenIfValid("SomeUnknownActivity");
        JsonTestUtils.assertListEquals(Collections.EMPTY_LIST,
                new ArrayList<>(customerSession.getProductUsageTokens()));
    }

    @Test
    public void create_withoutInvokingFunctions_fetchesKeyAndCustomer()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        final CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(null);

        verify(mApiHandler).retrieveCustomer(firstKey.getCustomerId(), firstKey.getSecret());
        assertNotNull(customerSession.getCustomer());
        assertNotNull(FIRST_CUSTOMER);
        assertEquals(FIRST_CUSTOMER.getId(), customerSession.getCustomer().getId());
    }

    @Test
    public void setCustomerShippingInfo_withValidInfo_callsWithExpectedArgs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        final CustomerEphemeralKey firstKey = Objects.requireNonNull(
                getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW));
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        Calendar proxyCalendar = Calendar.getInstance();
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        Customer customerWithShippingInfo = Objects
                .requireNonNull(Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT_WITH_SHIPPING_INFO));
        ShippingInformation shippingInformation = Objects.requireNonNull(customerWithShippingInfo
                .getShippingInformation());
        customerSession.setCustomerShippingInformation(mContext, shippingInformation);

        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mApiHandler).setCustomerShippingInfo(
                eq(mContext),
                eq(FIRST_CUSTOMER.getId()),
                eq("pk_test_abc123"),
                mListArgumentCaptor.capture(),
                eq(shippingInformation),
                eq(firstKey.getSecret()),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull());
        assertTrue(mListArgumentCaptor.getValue().contains("PaymentMethodsActivity"));
    }

    @Test
    public void retrieveCustomer_withExpiredCache_updatesCustomer()
            throws CardException, APIException, InvalidRequestException,
            AuthenticationException, APIConnectionException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        CustomerEphemeralKey secondKey = getCustomerEphemeralKey(SECOND_SAMPLE_KEY_RAW);
        assertNotNull(secondKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis - 100L);

        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        assertNotNull(FIRST_CUSTOMER);
        assertEquals(firstKey.getCustomerId(), FIRST_CUSTOMER.getId());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
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
        customerSession.retrieveCurrentCustomer(mockListener);

        verify(mockListener).onCustomerRetrieved(mCustomerArgumentCaptor.capture());
        final Customer capturedCustomer = mCustomerArgumentCaptor.getValue();
        assertNotNull(capturedCustomer);
        assertNotNull(SECOND_CUSTOMER);
        assertEquals(SECOND_CUSTOMER.getId(), capturedCustomer.getId());
        assertNotNull(customerSession.getCustomer());
        //  Make sure the value is cached.
        assertEquals(SECOND_CUSTOMER.getId(), customerSession.getCustomer().getId());

        verify(mApiHandler).retrieveCustomer(firstKey.getCustomerId(), firstKey.getSecret());
        verify(mApiHandler).retrieveCustomer(secondKey.getCustomerId(), secondKey.getSecret());
    }

    @Test
    public void retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        final CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);

        // Make sure we're in a good state and that we have the expected customer
        assertNotNull(customerSession.getCustomer());
        assertNotNull(FIRST_CUSTOMER);
        assertEquals(firstKey.getCustomerId(), FIRST_CUSTOMER.getId());
        assertEquals(firstKey.getCustomerId(), customerSession.getCustomer().getId());

        verify(mApiHandler).retrieveCustomer(firstKey.getCustomerId(), firstKey.getSecret());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);
        customerSession.retrieveCurrentCustomer(mockListener);

        verify(mockListener).onCustomerRetrieved(mCustomerArgumentCaptor.capture());
        Customer capturedCustomer = mCustomerArgumentCaptor.getValue();

        assertNotNull(capturedCustomer);
        assertEquals(FIRST_CUSTOMER.getId(), capturedCustomer.getId());
        assertNotNull(customerSession.getCustomer());
        //  Make sure the value is cached.
        assertEquals(FIRST_CUSTOMER.getId(), customerSession.getCustomer().getId());
        verifyNoMoreInteractions(mApiHandler);
    }

    @Test
    public void addSourceToCustomer_withUnExpiredCustomer_returnsAddedSourceAndEmptiesLogs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("AddSourceActivity");
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid("AddSourceActivity");
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        customerSession.addCustomerSource(mContext,
                "abc123",
                Source.CARD,
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mApiHandler).addCustomerSource(
                eq(mContext),
                eq(FIRST_CUSTOMER.getId()),
                eq("pk_test_abc123"),
                mListArgumentCaptor.capture(),
                eq("abc123"),
                eq(Source.CARD),
                eq(firstKey.getSecret()),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull());
        final List<String> productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains("AddSourceActivity"));
        assertTrue(productUsage.contains("PaymentMethodsActivity"));

        verify(mockListener).onSourceRetrieved(mSourceArgumentCaptor.capture());
        final Source capturedSource = mSourceArgumentCaptor.getValue();
        assertNotNull(capturedSource);
        assertEquals(mAddedSource.getId(), capturedSource.getId());
    }

    @Test
    public void addSourceToCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs()
            throws APIException, APIConnectionException, InvalidRequestException,
            AuthenticationException, CardException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("AddSourceActivity");
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        setupErrorProxy();
        customerSession.addCustomerSource(mContext,
                "abc123",
                Source.CARD,
                mockListener);

        verify(mBroadcastReceiver).onReceive(any(Context.class), mIntentArgumentCaptor.capture());

        Intent captured = mIntentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);

        verify(mockListener)
                .onError(404, "The card is invalid", null);
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }

    @Test
    public void removeSourceFromCustomer_withUnExpiredCustomer_returnsRemovedSourceAndEmptiesLogs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("AddSourceActivity");
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid("AddSourceActivity");
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        customerSession.deleteCustomerSource(mContext,
                "abc123",
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mApiHandler).deleteCustomerSource(
                eq(mContext),
                eq(FIRST_CUSTOMER.getId()),
                eq("pk_test_abc123"),
                mListArgumentCaptor.capture(),
                eq("abc123"),
                eq(firstKey.getSecret()),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull());
        final List productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains("AddSourceActivity"));
        assertTrue(productUsage.contains("PaymentMethodsActivity"));

        verify(mockListener).onSourceRetrieved(mSourceArgumentCaptor.capture());
        final Source capturedSource = mSourceArgumentCaptor.getValue();
        assertNotNull(capturedSource);
        assertEquals(mAddedSource.getId(), capturedSource.getId());
    }

    @Test
    public void removeSourceFromCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs()
            throws APIException, APIConnectionException, InvalidRequestException,
            AuthenticationException, CardException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("AddSourceActivity");
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        setupErrorProxy();
        customerSession.deleteCustomerSource(mContext, "abc123", mockListener);

        verify(mBroadcastReceiver).onReceive(any(Context.class),
                mIntentArgumentCaptor.capture());
        final Intent captured = mIntentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);

        verify(mockListener)
                .onError(404,"The card does not exist", null);
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }

    @Test
    public void setDefaultSourceForCustomer_withUnExpiredCustomer_returnsCustomerAndClearsLog()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        customerSession.setCustomerDefaultSource(mContext,
                "abc123",
                Source.CARD,
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mApiHandler).setDefaultCustomerSource(
                eq(mContext),
                eq(FIRST_CUSTOMER.getId()),
                eq("pk_test_abc123"),
                mListArgumentCaptor.capture(),
                eq("abc123"),
                eq(Source.CARD),
                eq(firstKey.getSecret()),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull());

        final List<String> productUsage = mListArgumentCaptor.getValue();
        assertEquals(1, productUsage.size());
        assertTrue(productUsage.contains("PaymentMethodsActivity"));

        verify(mockListener).onCustomerRetrieved(mCustomerArgumentCaptor.capture());
        final Customer capturedCustomer = mCustomerArgumentCaptor.getValue();
        assertNotNull(capturedCustomer);
        assertNotNull(SECOND_CUSTOMER);
        assertEquals(SECOND_CUSTOMER.getId(), capturedCustomer.getId());
    }

    @Test
    public void setDefaultSourceForCustomer_whenApiThrows_tellsListenerBroadcastsAndClearsLogs()
            throws APIException, APIConnectionException, InvalidRequestException,
            AuthenticationException, CardException {
        CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        Calendar proxyCalendar = Calendar.getInstance();
        long firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.getExpires());
        long enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2);
        proxyCalendar.setTimeInMillis(firstExpiryTimeInMillis + enoughTimeNotToBeExpired);

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.getTimeInMillis() > 0);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid("PaymentMethodsActivity");

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        setupErrorProxy();
        customerSession.setCustomerDefaultSource(mContext, "abc123", Source.CARD,
                mockListener);

        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mBroadcastReceiver).onReceive(any(Context.class),
                intentArgumentCaptor.capture());

        Intent captured = intentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);
        verify(mockListener).onError(405, "auth error", null);
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }

    @Test
    public void shippingInfoScreen_whenLaunched_logs() {
        final CustomerSession customerSession = createCustomerSession(null);
        CustomerSession.setInstance(customerSession);
        final Intent intent = new Intent()
                .putExtra(PAYMENT_SESSION_CONFIG, new PaymentSessionConfig.Builder()
                        .build())
                .putExtra(PAYMENT_SESSION_DATA_KEY, new PaymentSessionData());
        Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        List<String> actualTokens = new ArrayList<>(customerSession.getProductUsageTokens());
        assertTrue(actualTokens.contains("ShippingInfoScreen"));
    }

    @Test
    public void shippingMethodScreen_whenLaunched_logs() {
        final CustomerSession customerSession = createCustomerSession(null);
        CustomerSession.setInstance(customerSession);

        final Intent intent = new Intent()
                .putExtra(PAYMENT_SESSION_CONFIG, new PaymentSessionConfig.Builder()
                        .setShippingInfoRequired(false)
                        .build())
                .putExtra(PAYMENT_SESSION_DATA_KEY, new PaymentSessionData());

        Robolectric.buildActivity(PaymentFlowActivity.class, intent)
                .create().start().resume().visible();
        assertTrue(new ArrayList<>(customerSession.getProductUsageTokens())
                .contains("ShippingMethodScreen"));
    }

    private void setupErrorProxy()
            throws APIException, APIConnectionException, InvalidRequestException,
            AuthenticationException, CardException {
        when(mApiHandler.addCustomerSource(
                eq(mContext),
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenThrow(new APIException("The card is invalid", "request_123", 404, null,
                        null));

        when(mApiHandler.deleteCustomerSource(
                eq(mContext),
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenThrow(new APIException("The card does not exist", "request_123", 404, null,
                        null));

        when(mApiHandler.setDefaultCustomerSource(
                eq(mContext),
                anyString(),
                anyString(),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                anyString(),
                ArgumentMatchers.<StripeApiHandler.LoggingResponseListener>isNull()))
                .thenThrow(new APIException("auth error", "reqId", 405, null, null));
    }

    @NonNull
    private CustomerSession createCustomerSession(@Nullable Calendar calendar) {
        return new CustomerSession(mEphemeralKeyProvider, calendar, mThreadPoolExecutor,
                mApiHandler);
    }
}
