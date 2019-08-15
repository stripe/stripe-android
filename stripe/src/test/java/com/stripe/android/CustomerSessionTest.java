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
import com.stripe.android.exception.StripeException;
import com.stripe.android.model.Customer;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.ShippingInformation;
import com.stripe.android.model.Source;
import com.stripe.android.testharness.JsonTestUtils;
import com.stripe.android.testharness.TestEphemeralKeyProvider;
import com.stripe.android.view.AddPaymentMethodActivity;
import com.stripe.android.view.BaseViewTest;
import com.stripe.android.view.CardInputTestActivity;
import com.stripe.android.view.PaymentFlowActivity;
import com.stripe.android.view.PaymentFlowActivityStarter;
import com.stripe.android.view.PaymentMethodsActivity;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
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
public class CustomerSessionTest extends BaseViewTest<PaymentFlowActivity> {

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

    static final String FIRST_TEST_CUSTOMER_OBJECT =
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

    private static final String PAYMENT_METHOD_OBJECT = "{\n" +
            "  \"id\": \"pm_abc123\",\n" +
            "  \"object\": \"payment_method\",\n" +
            "  \"created\": 1556220472,\n" +
            "  \"customer\": \"cus_AQsHpvKfKwJDrF\",\n" +
            "  \"livemode\": false,\n" +
            "  \"metadata\": {},\n" +
            "  \"type\": \"card\"\n" +
            "}";

    private static final Customer FIRST_CUSTOMER =
            Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT);
    private static final Customer SECOND_CUSTOMER =
            Customer.fromString(SECOND_TEST_CUSTOMER_OBJECT);

    @Mock private BroadcastReceiver mBroadcastReceiver;
    @Mock private StripeRepository mStripeRepository;
    @Mock private ThreadPoolExecutor mThreadPoolExecutor;

    @Captor private ArgumentCaptor<List<String>> mListArgumentCaptor;
    @Captor private ArgumentCaptor<Source> mSourceArgumentCaptor;
    @Captor private ArgumentCaptor<PaymentMethod> mPaymentMethodArgumentCaptor;
    @Captor private ArgumentCaptor<List<PaymentMethod>> mPaymentMethodsArgumentCaptor;
    @Captor private ArgumentCaptor<Customer> mCustomerArgumentCaptor;
    @Captor private ArgumentCaptor<Intent> mIntentArgumentCaptor;
    @Captor private ArgumentCaptor<ApiRequest.Options> mRequestOptionsArgumentCaptor;

    private TestEphemeralKeyProvider mEphemeralKeyProvider;
    private Source mAddedSource;
    private PaymentMethod mPaymentMethod;

    public CustomerSessionTest() {
        super(PaymentFlowActivity.class);
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @NonNull
    private CustomerEphemeralKey getCustomerEphemeralKey(@NonNull String key) throws JSONException {
        return CustomerEphemeralKey.fromJson(new JSONObject(key));
    }

    @Before
    public void setup() throws StripeException {
        MockitoAnnotations.initMocks(this);
        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
                .registerReceiver(mBroadcastReceiver,
                        new IntentFilter(CustomerSession.ACTION_API_EXCEPTION));

        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(SECOND_CUSTOMER);

        mEphemeralKeyProvider = new TestEphemeralKeyProvider();

        mAddedSource = Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE);
        assertNotNull(mAddedSource);

        mPaymentMethod = PaymentMethod.fromString(PAYMENT_METHOD_OBJECT);
        assertNotNull(mPaymentMethod);

        when(mStripeRepository.retrieveCustomer(anyString(), ArgumentMatchers.<ApiRequest.Options>any()))
                .thenReturn(FIRST_CUSTOMER, SECOND_CUSTOMER);

        when(mStripeRepository.addCustomerSource(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()
        ))
                .thenReturn(mAddedSource);

        when(mStripeRepository.deleteCustomerSource(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()))
                .thenReturn(Source.fromString(CardInputTestActivity.EXAMPLE_JSON_CARD_SOURCE));

        when(mStripeRepository.setDefaultCustomerSource(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()))
                .thenReturn(SECOND_CUSTOMER);

        when(mStripeRepository.attachPaymentMethod(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()
        ))
                .thenReturn(mPaymentMethod);

        when(mStripeRepository.detachPaymentMethod(
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()
        ))
                .thenReturn(mPaymentMethod);

        when(mStripeRepository.getPaymentMethods(
                anyString(),
                eq("card"),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                ArgumentMatchers.<ApiRequest.Options>any()
        ))
                .thenReturn(Collections.singletonList(mPaymentMethod));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }
        }).when(mThreadPoolExecutor).execute(any(Runnable.class));
    }

    @Test
    public void getInstance_withoutInitializing_throwsException() {
        CustomerSession.clearInstance();

        assertThrows(IllegalStateException.class, new ThrowingRunnable() {
            @Override
            public void run() {
                CustomerSession.getInstance();
            }
        });
    }

    @Test
    public void addProductUsageTokenIfValid_whenValid_addsExpectedTokens() {
        final CustomerSession customerSession = createCustomerSession(null);
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);

        final List<String> expectedTokens = new ArrayList<>();
        expectedTokens.add(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);

        JsonTestUtils.assertListEquals(expectedTokens,
                new ArrayList<>(customerSession.getProductUsageTokens()));

        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        expectedTokens.add(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

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
            APIConnectionException, JSONException {
        final CustomerEphemeralKey firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        assertNotNull(firstKey);

        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        final CustomerSession customerSession = createCustomerSession(null);

        verify(mStripeRepository).retrieveCustomer(eq(firstKey.getCustomerId()),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);
        assertNotNull(customerSession.getCustomer());
        assertNotNull(FIRST_CUSTOMER);
        assertEquals(FIRST_CUSTOMER.getId(), customerSession.getCustomer().getId());
    }

    @Test
    public void setCustomerShippingInfo_withValidInfo_callsWithExpectedArgs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException, JSONException {
        final CustomerEphemeralKey firstKey = Objects.requireNonNull(
                getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW));
        mEphemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW);
        Calendar proxyCalendar = Calendar.getInstance();
        final CustomerSession customerSession = createCustomerSession(proxyCalendar);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        Customer customerWithShippingInfo = Objects
                .requireNonNull(Customer.fromString(FIRST_TEST_CUSTOMER_OBJECT_WITH_SHIPPING_INFO));
        ShippingInformation shippingInformation = Objects.requireNonNull(customerWithShippingInfo
                .getShippingInformation());
        customerSession.setCustomerShippingInformation(shippingInformation);

        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).setCustomerShippingInfo(
                eq(FIRST_CUSTOMER.getId()),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                eq(shippingInformation),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);
        assertTrue(mListArgumentCaptor.getValue().contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));
    }

    @Test
    public void retrieveCustomer_withExpiredCache_updatesCustomer()
            throws CardException, APIException, InvalidRequestException,
            AuthenticationException, APIConnectionException, JSONException {
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

        verify(mStripeRepository).retrieveCustomer(eq(firstKey.getCustomerId()),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);
        verify(mStripeRepository).retrieveCustomer(eq(secondKey.getCustomerId()),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(secondKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);
    }

    @Test
    public void retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException, JSONException {
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

        verify(mStripeRepository).retrieveCustomer(eq(firstKey.getCustomerId()),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

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
        verifyNoMoreInteractions(mStripeRepository);
    }

    @Test
    public void addSourceToCustomer_withUnExpiredCustomer_returnsAddedSourceAndEmptiesLogs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        customerSession.addCustomerSource(
                "abc123",
                Source.SourceType.CARD,
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).addCustomerSource(
                eq(FIRST_CUSTOMER.getId()),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                eq("abc123"),
                eq(Source.SourceType.CARD),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

        final List<String> productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY));
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));

        verify(mockListener).onSourceRetrieved(mSourceArgumentCaptor.capture());
        final Source capturedSource = mSourceArgumentCaptor.getValue();
        assertNotNull(capturedSource);
        assertEquals(mAddedSource.getId(), capturedSource.getId());
    }

    @Test
    public void addSourceToCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs()
            throws StripeException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        setupErrorProxy();
        customerSession.addCustomerSource(
                "abc123",
                Source.SourceType.CARD,
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
            APIConnectionException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        customerSession.deleteCustomerSource(
                "abc123",
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).deleteCustomerSource(
                eq(FIRST_CUSTOMER.getId()),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                eq("abc123"),
                mRequestOptionsArgumentCaptor.capture());
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

        final List productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY));
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));

        verify(mockListener).onSourceRetrieved(mSourceArgumentCaptor.capture());
        final Source capturedSource = mSourceArgumentCaptor.getValue();
        assertNotNull(capturedSource);
        assertEquals(mAddedSource.getId(), capturedSource.getId());
    }

    @Test
    public void removeSourceFromCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs()
            throws StripeException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.SourceRetrievalListener mockListener =
                mock(CustomerSession.SourceRetrievalListener.class);

        setupErrorProxy();
        customerSession.deleteCustomerSource("abc123", mockListener);

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
            APIConnectionException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        customerSession.setCustomerDefaultSource(
                "abc123",
                Source.SourceType.CARD,
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).setDefaultCustomerSource(
                eq(FIRST_CUSTOMER.getId()),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                eq("abc123"),
                eq(Source.SourceType.CARD),
                mRequestOptionsArgumentCaptor.capture()
        );
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

        final List<String> productUsage = mListArgumentCaptor.getValue();
        assertEquals(1, productUsage.size());
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));

        verify(mockListener).onCustomerRetrieved(mCustomerArgumentCaptor.capture());
        final Customer capturedCustomer = mCustomerArgumentCaptor.getValue();
        assertNotNull(capturedCustomer);
        assertNotNull(SECOND_CUSTOMER);
        assertEquals(SECOND_CUSTOMER.getId(), capturedCustomer.getId());
    }

    @Test
    public void setDefaultSourceForCustomer_whenApiThrows_tellsListenerBroadcastsAndClearsLogs()
            throws StripeException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.CustomerRetrievalListener mockListener =
                mock(CustomerSession.CustomerRetrievalListener.class);

        setupErrorProxy();
        customerSession.setCustomerDefaultSource("abc123", Source.SourceType.CARD,
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
        createActivity(new PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                        .build())
                .setPaymentSessionData(new PaymentSessionData())
                .build());

        final List<String> actualTokens = new ArrayList<>(customerSession.getProductUsageTokens());
        assertTrue(actualTokens.contains("ShippingInfoScreen"));
    }

    @Test
    public void shippingMethodScreen_whenLaunched_logs() {
        final CustomerSession customerSession = createCustomerSession(null);
        CustomerSession.setInstance(customerSession);

        createActivity(new PaymentFlowActivityStarter.Args.Builder()
                .setPaymentSessionConfig(new PaymentSessionConfig.Builder()
                        .setShippingInfoRequired(false)
                        .build())
                .setPaymentSessionData(new PaymentSessionData())
                .build());

        assertTrue(new ArrayList<>(customerSession.getProductUsageTokens())
                .contains("ShippingMethodScreen"));
    }

    @Test
    public void attachPaymentMethodToCustomer_withUnExpiredCustomer_returnsAddedPaymentMethodAndEmptiesLogs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.PaymentMethodRetrievalListener mockListener =
                mock(CustomerSession.PaymentMethodRetrievalListener.class);

        customerSession.attachPaymentMethod("pm_abc123", mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).attachPaymentMethod(
                eq(FIRST_CUSTOMER.getId()),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                eq("pm_abc123"),
                mRequestOptionsArgumentCaptor.capture()
        );
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

        final List<String> productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY));
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));

        verify(mockListener).onPaymentMethodRetrieved(mPaymentMethodArgumentCaptor.capture());
        final PaymentMethod capturedPaymentMethod = mPaymentMethodArgumentCaptor.getValue();
        assertNotNull(capturedPaymentMethod);
        assertEquals(mPaymentMethod.id, capturedPaymentMethod.id);
    }

    @Test
    public void attachPaymentMethodToCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs()
            throws StripeException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.PaymentMethodRetrievalListener mockListener =
                mock(CustomerSession.PaymentMethodRetrievalListener.class);

        setupErrorProxy();
        customerSession.attachPaymentMethod("pm_abc123", mockListener);

        verify(mBroadcastReceiver).onReceive(any(Context.class), mIntentArgumentCaptor.capture());

        Intent captured = mIntentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);

        verify(mockListener)
                .onError(404, "The payment method is invalid", null);
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }


    @Test
    public void detachPaymentMethodFromCustomer_withUnExpiredCustomer_returnsRemovedPaymentMethodAndEmptiesLogs()
            throws CardException, APIException, InvalidRequestException, AuthenticationException,
            APIConnectionException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.PaymentMethodRetrievalListener mockListener =
                mock(CustomerSession.PaymentMethodRetrievalListener.class);

        customerSession.detachPaymentMethod(
                "pm_abc123",
                mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).detachPaymentMethod(
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                eq("pm_abc123"),
                mRequestOptionsArgumentCaptor.capture()
        );
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

        final List productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY));
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));

        verify(mockListener).onPaymentMethodRetrieved(mPaymentMethodArgumentCaptor.capture());
        final PaymentMethod capturedPaymentMethod = mPaymentMethodArgumentCaptor.getValue();
        assertNotNull(capturedPaymentMethod);
        assertEquals(mPaymentMethod.id, capturedPaymentMethod.id);
    }

    @Test
    public void detachPaymentMethodFromCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs()
            throws StripeException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.class.getSimpleName());
        assertFalse(customerSession.getProductUsageTokens().isEmpty());

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        CustomerSession.PaymentMethodRetrievalListener mockListener =
                mock(CustomerSession.PaymentMethodRetrievalListener.class);

        setupErrorProxy();
        customerSession.detachPaymentMethod("pm_abc123", mockListener);

        verify(mBroadcastReceiver).onReceive(any(Context.class),
                mIntentArgumentCaptor.capture());
        final Intent captured = mIntentArgumentCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION));
        APIException ex = (APIException)
                captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION);
        assertNotNull(ex);

        verify(mockListener)
                .onError(404, "The payment method does not exist", null);
        assertTrue(customerSession.getProductUsageTokens().isEmpty());
    }

    @Test
    public void getPaymentMethods_withUnExpiredCustomer_returnsAddedPaymentMethodAndEmptiesLogs()
            throws StripeException, JSONException {
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
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY);

        long firstCustomerCacheTime = customerSession.getCustomerCacheTime();
        long shortIntervalInMilliseconds = 10L;

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY);
        proxyCalendar.setTimeInMillis(firstCustomerCacheTime + shortIntervalInMilliseconds);
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
                proxyCalendar.getTimeInMillis());
        final CustomerSession.PaymentMethodsRetrievalListener mockListener =
                mock(CustomerSession.PaymentMethodsRetrievalListener.class);

        customerSession.getPaymentMethods(PaymentMethod.Type.Card, mockListener);

        assertTrue(customerSession.getProductUsageTokens().isEmpty());
        assertNotNull(FIRST_CUSTOMER);
        assertNotNull(FIRST_CUSTOMER.getId());
        verify(mStripeRepository).getPaymentMethods(
                eq(FIRST_CUSTOMER.getId()),
                eq("card"),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                mListArgumentCaptor.capture(),
                mRequestOptionsArgumentCaptor.capture()
        );
        assertEquals(firstKey.getSecret(),
                mRequestOptionsArgumentCaptor.getValue().apiKey);

        final List<String> productUsage = mListArgumentCaptor.getValue();
        assertEquals(2, productUsage.size());
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY));
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY));

        verify(mockListener).onPaymentMethodsRetrieved(mPaymentMethodsArgumentCaptor.capture());
        final List<PaymentMethod> paymentMethods = mPaymentMethodsArgumentCaptor.getValue();
        assertNotNull(paymentMethods);
    }

    private void setupErrorProxy()
            throws StripeException {
        when(mStripeRepository.addCustomerSource(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()
        ))
                .thenThrow(new APIException("The card is invalid", "request_123", 404, null,
                        null));

        when(mStripeRepository.deleteCustomerSource(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()))
                .thenThrow(new APIException("The card does not exist", "request_123", 404, null,
                        null));

        when(mStripeRepository.setDefaultCustomerSource(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()))
                .thenThrow(new APIException("auth error", "reqId", 405, null, null));

        when(mStripeRepository.attachPaymentMethod(
                anyString(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()
        ))
                .thenThrow(new APIException("The payment method is invalid", "request_123", 404,
                        null, null));

        when(mStripeRepository.detachPaymentMethod(
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                anyString(),
                ArgumentMatchers.<ApiRequest.Options>any()))
                .thenThrow(new APIException("The payment method does not exist", "request_123",
                        404, null, null));

        when(mStripeRepository.getPaymentMethods(
                anyString(),
                eq("card"),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                ArgumentMatchers.<String>anyList(),
                ArgumentMatchers.<ApiRequest.Options>any()))
                .thenThrow(new APIException("The payment method does not exist", "request_123",
                        404, null, null));
    }

    @NonNull
    private CustomerSession createCustomerSession(@Nullable Calendar calendar) {
        return new CustomerSession(ApplicationProvider.getApplicationContext(),
                mEphemeralKeyProvider, calendar, mThreadPoolExecutor, mStripeRepository,
                ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
                "acct_abc123", true);
    }
}
