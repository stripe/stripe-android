package com.stripe.android

import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.exception.APIConnectionException
import com.stripe.android.exception.APIException
import com.stripe.android.exception.AuthenticationException
import com.stripe.android.exception.CardException
import com.stripe.android.exception.InvalidRequestException
import com.stripe.android.exception.StripeException
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.testharness.JsonTestUtils
import com.stripe.android.testharness.TestEphemeralKeyProvider
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.BaseViewTest
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentFlowActivityStarter
import com.stripe.android.view.PaymentMethodsActivity
import java.util.Calendar
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.json.JSONException
import org.json.JSONObject
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CustomerSession].
 */
@RunWith(RobolectricTestRunner::class)
class CustomerSessionTest : BaseViewTest<PaymentFlowActivity>(PaymentFlowActivity::class.java) {

    @Mock
    private lateinit var broadcastReceiver: BroadcastReceiver
    @Mock
    private lateinit var stripeRepository: StripeRepository
    @Mock
    private lateinit var threadPoolExecutor: ThreadPoolExecutor

    private lateinit var productUsageArgumentCaptor: KArgumentCaptor<Set<String>>
    private lateinit var sourceArgumentCaptor: KArgumentCaptor<Source>
    private lateinit var paymentMethodArgumentCaptor: KArgumentCaptor<PaymentMethod>
    private lateinit var paymentMethodsArgumentCaptor: KArgumentCaptor<List<PaymentMethod>>
    private lateinit var customerArgumentCaptor: KArgumentCaptor<Customer>
    private lateinit var intentArgumentCaptor: KArgumentCaptor<Intent>
    private lateinit var requestOptionsArgumentCaptor: KArgumentCaptor<ApiRequest.Options>

    private val ephemeralKeyProvider: TestEphemeralKeyProvider = TestEphemeralKeyProvider()

    @AfterTest
    override fun tearDown() {
        super.tearDown()
    }

    @Throws(JSONException::class)
    private fun getCustomerEphemeralKey(key: String): EphemeralKey {
        return EphemeralKey.fromJson(JSONObject(key))
    }

    @BeforeTest
    @Throws(StripeException::class)
    fun setup() {
        MockitoAnnotations.initMocks(this)

        productUsageArgumentCaptor = argumentCaptor()
        sourceArgumentCaptor = argumentCaptor()
        paymentMethodArgumentCaptor = argumentCaptor()
        paymentMethodsArgumentCaptor = argumentCaptor()
        customerArgumentCaptor = argumentCaptor()
        intentArgumentCaptor = argumentCaptor()
        requestOptionsArgumentCaptor = argumentCaptor()

        LocalBroadcastManager.getInstance(ApplicationProvider.getApplicationContext())
            .registerReceiver(broadcastReceiver,
                IntentFilter(CustomerSession.ACTION_API_EXCEPTION))

        `when`<Customer>(stripeRepository.retrieveCustomer(any(), any()))
            .thenReturn(FIRST_CUSTOMER, SECOND_CUSTOMER)

        `when`<Source>(stripeRepository.addCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any(),
            any()
        ))
            .thenReturn(SourceFixtures.SOURCE_CARD)

        `when`<Source>(stripeRepository.deleteCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()))
            .thenReturn(SourceFixtures.SOURCE_CARD)

        `when`<Customer>(stripeRepository.setDefaultCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any(),
            any()))
            .thenReturn(SECOND_CUSTOMER)

        `when`<PaymentMethod>(stripeRepository.attachPaymentMethod(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()
        ))
            .thenReturn(PAYMENT_METHOD)

        `when`<PaymentMethod>(stripeRepository.detachPaymentMethod(
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()
        ))
            .thenReturn(PAYMENT_METHOD)

        `when`(stripeRepository.getPaymentMethods(
            any(),
            eq("card"),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any()
        ))
            .thenReturn(listOf(PAYMENT_METHOD))

        doAnswer { invocation ->
            invocation.getArgument<Runnable>(0).run()
            null
        }.`when`<ThreadPoolExecutor>(threadPoolExecutor).execute(any())
    }

    @Test
    fun getInstance_withoutInitializing_throwsException() {
        CustomerSession.clearInstance()

        assertFailsWith<IllegalStateException> { CustomerSession.getInstance() }
    }

    @Test
    fun addProductUsageTokenIfValid_whenValid_addsExpectedTokens() {
        val customerSession = createCustomerSession(null)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)

        val expectedTokens = listOf(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)

        JsonTestUtils.assertListEquals(expectedTokens, customerSession.productUsageTokens.toList())

        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        JsonTestUtils.assertListEquals(
            expectedTokens.plus(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY),
            customerSession.productUsageTokens.toList()
        )
    }

    @Test
    fun addProductUsageTokenIfValid_whenNotValid_addsNoTokens() {
        val customerSession = createCustomerSession(null)
        customerSession.addProductUsageTokenIfValid("SomeUnknownActivity")
        JsonTestUtils.assertListEquals(emptyList<String>(),
            customerSession.productUsageTokens.toList())
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class,
        AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun create_withoutInvokingFunctions_fetchesKeyAndCustomer() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(null)

        verify<StripeRepository>(stripeRepository).retrieveCustomer(eq(firstKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)
        val customerId = customerSession.customer?.id
        assertEquals(FIRST_CUSTOMER.id, customerId)
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class,
        AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun setCustomerShippingInfo_withValidInfo_callsWithExpectedArgs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val proxyCalendar = Calendar.getInstance()
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        val shippingInformation =
            requireNotNull(CustomerFixtures.CUSTOMER_WITH_SHIPPING.shippingInformation)
        customerSession.setCustomerShippingInformation(shippingInformation,
            object : CustomerSession.CustomerRetrievalListener {
                override fun onCustomerRetrieved(customer: Customer) {
                }

                override fun onError(
                    errorCode: Int,
                    errorMessage: String,
                    stripeError: StripeError?
                ) {
                }
            })

        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).setCustomerShippingInfo(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq(shippingInformation),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)
        assertTrue(productUsageArgumentCaptor.firstValue.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class,
        AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun retrieveCustomer_withExpiredCache_updatesCustomer() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val secondKey = getCustomerEphemeralKey(SECOND_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis - 100L

        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        assertEquals(firstExpiryTimeInMillis - 100L, firstCustomerCacheTime)
        val timeForCustomerToExpire = TimeUnit.MINUTES.toMillis(2)

        proxyCalendar.timeInMillis = firstCustomerCacheTime + timeForCustomerToExpire
        assertEquals(firstCustomerCacheTime + timeForCustomerToExpire,
            proxyCalendar.timeInMillis)

        // We want to make sure that the next ephemeral key will be different.
        ephemeralKeyProvider.setNextRawEphemeralKey(SECOND_SAMPLE_KEY_RAW)

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)
        customerSession.retrieveCurrentCustomer(mockListener)

        verify<CustomerSession.CustomerRetrievalListener>(mockListener).onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue
        assertEquals(SECOND_CUSTOMER.id, capturedCustomer.id)

        val customerId = customerSession.customer?.id
        //  Make sure the value is cached.
        assertEquals(SECOND_CUSTOMER.id, customerId)

        verify<StripeRepository>(stripeRepository).retrieveCustomer(eq(firstKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)
        verify<StripeRepository>(stripeRepository).retrieveCustomer(eq(secondKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(secondKey.secret,
            requestOptionsArgumentCaptor.allValues[1].apiKey)
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class, AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)

        // Make sure we're in a good state and that we have the expected customer
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)
        assertEquals(firstKey.objectId, customerSession.customer?.id)

        verify<StripeRepository>(stripeRepository).retrieveCustomer(eq(firstKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)
        customerSession.retrieveCurrentCustomer(mockListener)

        verify<CustomerSession.CustomerRetrievalListener>(mockListener).onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue

        assertEquals(FIRST_CUSTOMER.id, capturedCustomer.id)
        //  Make sure the value is cached.
        assertEquals(FIRST_CUSTOMER.id, customerSession.customer?.id)
        verifyNoMoreInteractions(stripeRepository)
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class, AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun addSourceToCustomer_withUnExpiredCustomer_returnsAddedSourceAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        customerSession.addCustomerSource(
            "abc123",
            Source.SourceType.CARD,
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).addCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("abc123"),
            eq(Source.SourceType.CARD),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify<CustomerSession.SourceRetrievalListener>(mockListener)
            .onSourceRetrieved(sourceArgumentCaptor.capture())
        val capturedSource = sourceArgumentCaptor.firstValue
        assertEquals(SourceFixtures.SOURCE_CARD.id, capturedSource.id)
    }

    @Test
    @Throws(StripeException::class, JSONException::class)
    fun addSourceToCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.addCustomerSource(
            "abc123",
            Source.SourceType.CARD,
            mockListener)

        verify<BroadcastReceiver>(broadcastReceiver)
            .onReceive(any(), intentArgumentCaptor.capture())

        val captured = intentArgumentCaptor.firstValue
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION))
        assertTrue(captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION) is APIException)

        verify<CustomerSession.SourceRetrievalListener>(mockListener)
            .onError(404, "The card is invalid", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class,
        AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun removeSourceFromCustomer_withUnExpiredCustomer_returnsRemovedSourceAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        customerSession.deleteCustomerSource(
            "abc123",
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).deleteCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("abc123"),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify<CustomerSession.SourceRetrievalListener>(mockListener)
            .onSourceRetrieved(sourceArgumentCaptor.capture())
        val capturedSource = sourceArgumentCaptor.firstValue
        assertEquals(SourceFixtures.SOURCE_CARD.id, capturedSource.id)
    }

    @Test
    @Throws(StripeException::class, JSONException::class)
    fun removeSourceFromCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.deleteCustomerSource("abc123", mockListener)

        verify<BroadcastReceiver>(broadcastReceiver).onReceive(any(),
            intentArgumentCaptor.capture())
        val captured = intentArgumentCaptor.firstValue
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION))
        assertTrue(captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION) is APIException)

        verify<CustomerSession.SourceRetrievalListener>(mockListener)
            .onError(404, "The card does not exist", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class, AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun setDefaultSourceForCustomer_withUnExpiredCustomer_returnsCustomerAndClearsLog() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)

        customerSession.setCustomerDefaultSource(
            "abc123",
            Source.SourceType.CARD,
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).setDefaultCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("abc123"),
            eq(Source.SourceType.CARD),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(1, productUsage.size)
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify<CustomerSession.CustomerRetrievalListener>(mockListener).onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue
        assertNotNull(SECOND_CUSTOMER)
        assertEquals(SECOND_CUSTOMER.id, capturedCustomer.id)
    }

    @Test
    @Throws(StripeException::class, JSONException::class)
    fun setDefaultSourceForCustomer_whenApiThrows_tellsListenerBroadcastsAndClearsLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.setCustomerDefaultSource("abc123", Source.SourceType.CARD,
            mockListener)

        val intentArgumentCaptor = ArgumentCaptor.forClass(Intent::class.java)
        verify<BroadcastReceiver>(broadcastReceiver).onReceive(any(),
            intentArgumentCaptor.capture())

        val captured = intentArgumentCaptor.firstValue
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION))
        assertTrue(captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION) is APIException)
        verify<CustomerSession.CustomerRetrievalListener>(mockListener)
            .onError(405, "auth error", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun shippingInfoScreen_whenLaunched_logs() {
        val customerSession = createCustomerSession(null)
        CustomerSession.instance = customerSession
        createActivity(PaymentFlowActivityStarter.Args.Builder()
            .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                .build())
            .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
            .build())

        assertTrue(customerSession.productUsageTokens.contains("ShippingInfoScreen"))
    }

    @Test
    fun shippingMethodScreen_whenLaunched_logs() {
        val customerSession = createCustomerSession(null)
        CustomerSession.instance = customerSession

        createActivity(PaymentFlowActivityStarter.Args.Builder()
            .setPaymentSessionConfig(PaymentSessionConfig.Builder()
                .setShippingInfoRequired(false)
                .build())
            .setPaymentSessionData(PaymentSessionFixtures.PAYMENT_SESSION_DATA)
            .build())

        assertTrue(customerSession.productUsageTokens.contains("ShippingMethodScreen"))
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class, AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun attachPaymentMethodToCustomer_withUnExpiredCustomer_returnsAddedPaymentMethodAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        customerSession.attachPaymentMethod("pm_abc123", mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).attachPaymentMethod(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("pm_abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify<CustomerSession.PaymentMethodRetrievalListener>(mockListener)
            .onPaymentMethodRetrieved(paymentMethodArgumentCaptor.capture())
        val capturedPaymentMethod = paymentMethodArgumentCaptor.firstValue
        assertEquals(PAYMENT_METHOD.id, capturedPaymentMethod.id)
    }

    @Test
    @Throws(StripeException::class, JSONException::class)
    fun attachPaymentMethodToCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.attachPaymentMethod("pm_abc123", mockListener)

        verify<BroadcastReceiver>(broadcastReceiver).onReceive(any(), intentArgumentCaptor.capture())

        val captured = intentArgumentCaptor.firstValue
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION))
        assertTrue(captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION) is APIException)

        verify<CustomerSession.PaymentMethodRetrievalListener>(mockListener)
            .onError(404, "The payment method is invalid", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    @Throws(CardException::class, APIException::class, InvalidRequestException::class,
        AuthenticationException::class, APIConnectionException::class, JSONException::class)
    fun detachPaymentMethodFromCustomer_withUnExpiredCustomer_returnsRemovedPaymentMethodAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener =
            mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        customerSession.detachPaymentMethod(
            "pm_abc123",
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).detachPaymentMethod(
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("pm_abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify<CustomerSession.PaymentMethodRetrievalListener>(mockListener)
            .onPaymentMethodRetrieved(paymentMethodArgumentCaptor.capture())
        val capturedPaymentMethod = paymentMethodArgumentCaptor.firstValue
        assertEquals(PAYMENT_METHOD.id, capturedPaymentMethod.id)
    }

    @Test
    @Throws(StripeException::class, JSONException::class)
    fun detachPaymentMethodFromCustomer_whenApiThrowsError_tellsListenerBroadcastsAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity::class.java.simpleName)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.detachPaymentMethod("pm_abc123", mockListener)

        verify<BroadcastReceiver>(broadcastReceiver).onReceive(any(),
            intentArgumentCaptor.capture())
        val captured = intentArgumentCaptor.firstValue
        assertTrue(captured.hasExtra(CustomerSession.EXTRA_EXCEPTION))
        assertTrue(captured.getSerializableExtra(CustomerSession.EXTRA_EXCEPTION) is APIException)

        verify<CustomerSession.PaymentMethodRetrievalListener>(mockListener)
            .onError(404, "The payment method does not exist", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    @Throws(StripeException::class, JSONException::class)
    fun getPaymentMethods_withUnExpiredCustomer_returnsAddedPaymentMethodAndEmptiesLogs() {
        val firstKey = getCustomerEphemeralKey(FIRST_SAMPLE_KEY_RAW)

        val proxyCalendar = Calendar.getInstance()
        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        val enoughTimeNotToBeExpired = TimeUnit.MINUTES.toMillis(2)
        proxyCalendar.timeInMillis = firstExpiryTimeInMillis + enoughTimeNotToBeExpired

        // Make sure the calendar is set before it gets used.
        assertTrue(proxyCalendar.timeInMillis > 0)

        ephemeralKeyProvider.setNextRawEphemeralKey(FIRST_SAMPLE_KEY_RAW)
        val customerSession = createCustomerSession(proxyCalendar)
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        proxyCalendar.timeInMillis = firstCustomerCacheTime + shortIntervalInMilliseconds
        assertEquals(firstCustomerCacheTime + shortIntervalInMilliseconds,
            proxyCalendar.timeInMillis)
        val mockListener = mock(CustomerSession.PaymentMethodsRetrievalListener::class.java)

        customerSession.getPaymentMethods(PaymentMethod.Type.Card, mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify<StripeRepository>(stripeRepository).getPaymentMethods(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq("card"),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify<CustomerSession.PaymentMethodsRetrievalListener>(mockListener)
            .onPaymentMethodsRetrieved(paymentMethodsArgumentCaptor.capture())
        val paymentMethods = paymentMethodsArgumentCaptor.firstValue
        assertNotNull(paymentMethods)
    }

    @Throws(StripeException::class)
    private fun setupErrorProxy() {
        `when`<Source>(stripeRepository.addCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any(),
            any()
        ))
            .thenThrow(APIException("The card is invalid", "request_123", 404, null, null))

        `when`<Source>(stripeRepository.deleteCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()))
            .thenThrow(APIException("The card does not exist", "request_123", 404, null, null))

        `when`<Customer>(stripeRepository.setDefaultCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any(),
            any()))
            .thenThrow(APIException("auth error", "reqId", 405, null, null))

        `when`<PaymentMethod>(stripeRepository.attachPaymentMethod(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()
        ))
            .thenThrow(APIException("The payment method is invalid", "request_123", 404, null, null))

        `when`<PaymentMethod>(stripeRepository.detachPaymentMethod(
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()))
            .thenThrow(APIException("The payment method does not exist", "request_123",
                404, null, null))

        `when`(stripeRepository.getPaymentMethods(
            any(),
            eq("card"),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any()))
            .thenThrow(APIException("The payment method does not exist", "request_123",
                404, null, null))
    }

    private fun createCustomerSession(calendar: Calendar?): CustomerSession {
        return CustomerSession(ApplicationProvider.getApplicationContext(),
            ephemeralKeyProvider, calendar, threadPoolExecutor, stripeRepository,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_abc123", true)
    }

    private companion object {
        private val FIRST_SAMPLE_KEY_RAW = CustomerFixtures.EPHEMERAL_KEY_FIRST.toString()
        private val SECOND_SAMPLE_KEY_RAW = CustomerFixtures.EPHEMERAL_KEY_SECOND.toString()

        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER
        private val SECOND_CUSTOMER = CustomerFixtures.OTHER_CUSTOMER

        private val PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD
    }
}
