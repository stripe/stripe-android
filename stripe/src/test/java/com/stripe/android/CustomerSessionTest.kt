package com.stripe.android

import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.exception.APIException
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.testharness.TestEphemeralKeyProvider
import com.stripe.android.view.ActivityScenarioFactory
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.PaymentFlowActivity
import com.stripe.android.view.PaymentFlowActivityStarter
import com.stripe.android.view.PaymentMethodsActivity
import java.util.Calendar
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CustomerSession].
 */
@RunWith(RobolectricTestRunner::class)
class CustomerSessionTest {

    private val stripeRepository: StripeRepository = mock()
    private val threadPoolExecutor: ThreadPoolExecutor = mock()

    private val productUsageArgumentCaptor: KArgumentCaptor<Set<String>> by lazy {
        argumentCaptor<Set<String>>()
    }
    private val sourceArgumentCaptor: KArgumentCaptor<Source> by lazy {
        argumentCaptor<Source>()
    }
    private val paymentMethodArgumentCaptor: KArgumentCaptor<PaymentMethod> by lazy {
        argumentCaptor<PaymentMethod>()
    }
    private val paymentMethodsArgumentCaptor: KArgumentCaptor<List<PaymentMethod>> by lazy {
        argumentCaptor<List<PaymentMethod>>()
    }
    private val customerArgumentCaptor: KArgumentCaptor<Customer> by lazy {
        argumentCaptor<Customer>()
    }
    private val requestOptionsArgumentCaptor: KArgumentCaptor<ApiRequest.Options> by lazy {
        argumentCaptor<ApiRequest.Options>()
    }

    private val ephemeralKeyProvider: TestEphemeralKeyProvider = TestEphemeralKeyProvider()

    private val activityScenarioFactory: ActivityScenarioFactory by lazy {
        ActivityScenarioFactory(ApplicationProvider.getApplicationContext())
    }

    @BeforeTest
    fun setup() {
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
            eq(PaymentMethod.Type.Card),
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
        val customerSession = createCustomerSession()
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)

        val expectedTokens = listOf(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)

        assertEquals(expectedTokens, customerSession.productUsageTokens.toList())

        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        assertEquals(
            expectedTokens.plus(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY),
            customerSession.productUsageTokens.toList()
        )
    }

    @Test
    fun addProductUsageTokenIfValid_whenNotValid_addsNoTokens() {
        val customerSession = createCustomerSession()
        customerSession.addProductUsageTokenIfValid("SomeUnknownActivity")
        assertTrue(customerSession.productUsageTokens.toList().isEmpty())
    }

    @Test
    fun create_withoutInvokingFunctions_fetchesKeyAndCustomer() {
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession()

        verify(stripeRepository).retrieveCustomer(
            eq(EphemeralKeyFixtures.FIRST.objectId),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )
        val customerId = customerSession.customer?.id
        assertEquals(FIRST_CUSTOMER.id, customerId)
    }

    @Test
    fun setCustomerShippingInfo_withValidInfo_callsWithExpectedArgs() {
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession()
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
        verify(stripeRepository).setCustomerShippingInfo(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq(shippingInformation),
            requestOptionsArgumentCaptor.capture())
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )
        assertTrue(productUsageArgumentCaptor.firstValue.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))
    }

    @Test
    fun retrieveCustomer_withExpiredCache_updatesCustomer() {
        val firstKey = EphemeralKeyFixtures.FIRST
        val secondKey = EphemeralKeyFixtures.SECOND

        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        var currentTime = firstExpiryTimeInMillis - 100L

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        assertEquals(firstExpiryTimeInMillis - 100L, firstCustomerCacheTime)
        val timeForCustomerToExpire = TimeUnit.MINUTES.toMillis(2)

        currentTime = firstCustomerCacheTime + timeForCustomerToExpire

        // We want to make sure that the next ephemeral key will be different.
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.SECOND_JSON)

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)
        customerSession.retrieveCurrentCustomer(mockListener)

        verify(mockListener)
            .onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue
        assertEquals(SECOND_CUSTOMER.id, capturedCustomer.id)

        val customerId = customerSession.customer?.id
        //  Make sure the value is cached.
        assertEquals(SECOND_CUSTOMER.id, customerId)

        verify(stripeRepository).retrieveCustomer(eq(firstKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)
        verify(stripeRepository).retrieveCustomer(eq(secondKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(secondKey.secret,
            requestOptionsArgumentCaptor.allValues[1].apiKey)
    }

    @Test
    fun retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi() {
        val firstKey = EphemeralKeyFixtures.FIRST

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }

        // Make sure we're in a good state and that we have the expected customer
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)
        assertEquals(firstKey.objectId, customerSession.customer?.id)

        verify(stripeRepository).retrieveCustomer(eq(firstKey.objectId),
            requestOptionsArgumentCaptor.capture())
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)
        customerSession.retrieveCurrentCustomer(mockListener)

        verify(mockListener).onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue

        assertEquals(FIRST_CUSTOMER.id, capturedCustomer.id)
        //  Make sure the value is cached.
        assertEquals(FIRST_CUSTOMER.id, customerSession.customer?.id)
        verifyNoMoreInteractions(stripeRepository)
    }

    @Test
    fun addSourceToCustomer_withUnExpiredCustomer_returnsAddedSourceAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)

        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        customerSession.addCustomerSource(
            "abc123",
            Source.SourceType.CARD,
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).addCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("abc123"),
            eq(Source.SourceType.CARD),
            requestOptionsArgumentCaptor.capture())
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify(mockListener)
            .onSourceRetrieved(sourceArgumentCaptor.capture())
        val capturedSource = sourceArgumentCaptor.firstValue
        assertEquals(SourceFixtures.SOURCE_CARD.id, capturedSource.id)
    }

    @Test
    fun addSourceToCustomer_whenApiThrowsError_callsListenerAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.addCustomerSource("abc123", Source.SourceType.CARD, mockListener)

        verify(mockListener)
            .onError(404, "The card is invalid", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun removeSourceFromCustomer_withUnExpiredCustomer_returnsRemovedSourceAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        customerSession.deleteCustomerSource(
            "abc123",
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).deleteCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("abc123"),
            requestOptionsArgumentCaptor.capture())
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify(mockListener)
            .onSourceRetrieved(sourceArgumentCaptor.capture())
        val capturedSource = sourceArgumentCaptor.firstValue
        assertEquals(SourceFixtures.SOURCE_CARD.id, capturedSource.id)
    }

    @Test
    fun removeSourceFromCustomer_whenApiThrowsError_callsListenerAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.SourceRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.deleteCustomerSource("abc123", mockListener)

        verify(mockListener)
            .onError(404, "The card does not exist", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun setDefaultSourceForCustomer_withUnExpiredCustomer_returnsCustomerAndClearsLog() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)

        customerSession.setCustomerDefaultSource(
            "abc123",
            Source.SourceType.CARD,
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).setDefaultCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("abc123"),
            eq(Source.SourceType.CARD),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(1, productUsage.size)
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify(mockListener).onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue
        assertNotNull(SECOND_CUSTOMER)
        assertEquals(SECOND_CUSTOMER.id, capturedCustomer.id)
    }

    @Test
    fun setDefaultSourceForCustomer_whenApiThrows_callsListenerAndClearsLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.CustomerRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.setCustomerDefaultSource("abc123", Source.SourceType.CARD,
            mockListener)

        verify(mockListener)
            .onError(405, "auth error", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun shippingInfoScreen_whenLaunched_logs() {
        val customerSession = createCustomerSession()
        CustomerSession.instance = customerSession

        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentSessionFixtures.PAYMENT_FLOW_ARGS
        ).use { activityScenario ->
            activityScenario.onActivity {
                assertTrue(customerSession.productUsageTokens.contains("ShippingInfoScreen"))
            }
        }
    }

    @Test
    fun shippingMethodScreen_whenLaunched_logs() {
        val customerSession = createCustomerSession()
        CustomerSession.instance = customerSession

        val config = PaymentSessionFixtures.CONFIG.copy(
            isShippingInfoRequired = false
        )
        activityScenarioFactory.create<PaymentFlowActivity>(
            PaymentFlowActivityStarter.Args(
                paymentSessionConfig = config,
                paymentSessionData = PaymentSessionData(config)
            )
        ).use { activityScenario ->
            activityScenario.onActivity {
                assertTrue(customerSession.productUsageTokens.contains("ShippingMethodScreen"))
            }
        }
    }

    @Test
    fun attachPaymentMethodToCustomer_withUnExpiredCustomer_returnsAddedPaymentMethodAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        customerSession.attachPaymentMethod("pm_abc123", mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).attachPaymentMethod(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("pm_abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify(mockListener)
            .onPaymentMethodRetrieved(paymentMethodArgumentCaptor.capture())
        val capturedPaymentMethod = paymentMethodArgumentCaptor.firstValue
        assertEquals(PAYMENT_METHOD.id, capturedPaymentMethod.id)
    }

    @Test
    fun attachPaymentMethodToCustomer_whenApiThrowsError_callsListenerAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        val mockListener = mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.attachPaymentMethod("pm_abc123", mockListener)

        verify(mockListener)
            .onError(404, "The payment method is invalid", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun detachPaymentMethodFromCustomer_withUnExpiredCustomer_returnsRemovedPaymentMethodAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener =
            mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        customerSession.detachPaymentMethod(
            "pm_abc123",
            mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).detachPaymentMethod(
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            eq("pm_abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify(mockListener)
            .onPaymentMethodRetrieved(paymentMethodArgumentCaptor.capture())
        val capturedPaymentMethod = paymentMethodArgumentCaptor.firstValue
        assertEquals(PAYMENT_METHOD.id, capturedPaymentMethod.id)
    }

    @Test
    fun detachPaymentMethodFromCustomer_whenApiThrowsError_callsListenerAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity::class.java.simpleName)
        assertFalse(customerSession.productUsageTokens.isEmpty())

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.PaymentMethodRetrievalListener::class.java)

        setupErrorProxy()
        customerSession.detachPaymentMethod("pm_abc123", mockListener)

        verify(mockListener)
            .onError(404, "The payment method does not exist", null)
        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun getPaymentMethods_withUnExpiredCustomer_returnsAddedPaymentMethodAndEmptiesLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession { currentTime }
        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        customerSession.addProductUsageTokenIfValid(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        customerSession.addProductUsageTokenIfValid(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY)
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds
        val mockListener = mock(CustomerSession.PaymentMethodsRetrievalListener::class.java)

        customerSession.getPaymentMethods(PaymentMethod.Type.Card, mockListener)

        assertTrue(customerSession.productUsageTokens.isEmpty())
        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).getPaymentMethods(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(PaymentMethod.Type.Card),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            productUsageArgumentCaptor.capture(),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        val productUsage = productUsageArgumentCaptor.firstValue
        assertEquals(2, productUsage.size)
        assertTrue(productUsage.contains(AddPaymentMethodActivity.TOKEN_ADD_PAYMENT_METHOD_ACTIVITY))
        assertTrue(productUsage.contains(PaymentMethodsActivity.TOKEN_PAYMENT_METHODS_ACTIVITY))

        verify(mockListener)
            .onPaymentMethodsRetrieved(paymentMethodsArgumentCaptor.capture())
        val paymentMethods = paymentMethodsArgumentCaptor.firstValue
        assertNotNull(paymentMethods)
    }

    private fun setupErrorProxy() {
        `when`<Source>(stripeRepository.addCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any(),
            any()
        ))
            .thenThrow(APIException(statusCode = 404, message = "The card is invalid"))

        `when`<Source>(stripeRepository.deleteCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()))
            .thenThrow(APIException(statusCode = 404, message = "The card does not exist"))

        `when`<Customer>(stripeRepository.setDefaultCustomerSource(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any(),
            any()))
            .thenThrow(APIException(statusCode = 405, message = "auth error"))

        `when`<PaymentMethod>(stripeRepository.attachPaymentMethod(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()
        ))
            .thenThrow(APIException(statusCode = 404, message = "The payment method is invalid"))

        `when`<PaymentMethod>(stripeRepository.detachPaymentMethod(
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any(),
            any()))
            .thenThrow(APIException(statusCode = 404, message = "The payment method does not exist"))

        `when`(stripeRepository.getPaymentMethods(
            any(),
            eq(PaymentMethod.Type.Card),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any()))
            .thenThrow(APIException(statusCode = 404, message = "The payment method does not exist"))
    }

    private fun createCustomerSession(
        timeSupplier: TimeSupplier = { Calendar.getInstance().timeInMillis }
    ): CustomerSession {
        return CustomerSession(
            ApplicationProvider.getApplicationContext(),
            stripeRepository,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_abc123",
            timeSupplier = timeSupplier,
            threadPoolExecutor = threadPoolExecutor,
            ephemeralKeyManagerFactory = EphemeralKeyManager.Factory(
                keyProvider = ephemeralKeyProvider,
                shouldPrefetchEphemeralKey = true,
                timeSupplier = timeSupplier
            )
        )
    }

    private companion object {
        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER
        private val SECOND_CUSTOMER = CustomerFixtures.OTHER_CUSTOMER

        private val PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        private val DEFAULT_CURRENT_TIME =
            TimeUnit.SECONDS.toMillis(EphemeralKeyFixtures.FIRST.expires) +
                TimeUnit.MINUTES.toMillis(2)
    }
}
