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
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.testharness.TestEphemeralKeyProvider
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.PaymentMethodsActivity
import java.util.Calendar
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.robolectric.RobolectricTestRunner

/**
 * Test class for [CustomerSession].
 */
@RunWith(RobolectricTestRunner::class)
class CustomerSessionTest {

    private val stripeRepository: StripeRepository = mock()
    private val threadPoolExecutor: ThreadPoolExecutor = mock()

    private val paymentMethodRetrievalListener: CustomerSession.PaymentMethodRetrievalListener = mock()
    private val paymentMethodsRetrievalListener: CustomerSession.PaymentMethodsRetrievalListener = mock()
    private val customerRetrievalListener: CustomerSession.CustomerRetrievalListener = mock()
    private val sourceRetrievalListener: CustomerSession.SourceRetrievalListener = mock()

    private val productUsageArgumentCaptor: KArgumentCaptor<Set<String>> = argumentCaptor()
    private val sourceArgumentCaptor: KArgumentCaptor<Source> = argumentCaptor()
    private val paymentMethodArgumentCaptor: KArgumentCaptor<PaymentMethod> = argumentCaptor()
    private val paymentMethodsArgumentCaptor: KArgumentCaptor<List<PaymentMethod>> = argumentCaptor()
    private val customerArgumentCaptor: KArgumentCaptor<Customer> = argumentCaptor()
    private val requestOptionsArgumentCaptor: KArgumentCaptor<ApiRequest.Options> = argumentCaptor()

    private val ephemeralKeyProvider: TestEphemeralKeyProvider = TestEphemeralKeyProvider()

    @BeforeTest
    fun setup() {
        `when`<Customer>(stripeRepository.retrieveCustomer(any(), any(), any()))
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
        val customerSession = createCustomerSession(
            ephemeralKeyManagerFactory = FakeEphemeralKeyManagerFactory(
                ephemeralKeyProvider,
                EphemeralKeyFixtures.create(2528128663000L)
            )
        )

        customerSession.attachPaymentMethod(
            "pm_12345",
            DEFAULT_PRODUCT_USAGE,
            paymentMethodRetrievalListener
        )

        verify(stripeRepository).attachPaymentMethod(
            any(),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(DEFAULT_PRODUCT_USAGE),
            any(),
            any()
        )
    }

    @Test
    fun create_withoutInvokingFunctions_fetchesKeyAndCustomer() {
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession()

        verify(stripeRepository).retrieveCustomer(
            eq(EphemeralKeyFixtures.FIRST.objectId),
            productUsageArgumentCaptor.capture(),
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
        val shippingInformation =
            requireNotNull(CustomerFixtures.CUSTOMER_WITH_SHIPPING.shippingInformation)
        customerSession.setCustomerShippingInformation(
            shippingInformation = shippingInformation,
            productUsage = DEFAULT_PRODUCT_USAGE,
            listener = object : CustomerSession.CustomerRetrievalListener {
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
            eq(DEFAULT_PRODUCT_USAGE),
            eq(shippingInformation),
            requestOptionsArgumentCaptor.capture())
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )
    }

    @Test
    fun retrieveCustomer_withExpiredCache_updatesCustomer() {
        val firstKey = EphemeralKeyFixtures.FIRST
        val secondKey = EphemeralKeyFixtures.SECOND

        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        var currentTime = firstExpiryTimeInMillis - 100L

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        assertEquals(firstExpiryTimeInMillis - 100L, firstCustomerCacheTime)
        val timeForCustomerToExpire = TimeUnit.MINUTES.toMillis(2)

        currentTime = firstCustomerCacheTime + timeForCustomerToExpire

        // We want to make sure that the next ephemeral key will be different.
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.SECOND_JSON)

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        customerSession.retrieveCurrentCustomer(DEFAULT_PRODUCT_USAGE, customerRetrievalListener)

        verify(customerRetrievalListener)
            .onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue
        assertEquals(SECOND_CUSTOMER.id, capturedCustomer.id)

        val customerId = customerSession.customer?.id
        //  Make sure the value is cached.
        assertEquals(SECOND_CUSTOMER.id, customerId)

        verify(stripeRepository).retrieveCustomer(
            eq(firstKey.objectId),
            eq(emptySet()),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)
        verify(stripeRepository).retrieveCustomer(
            eq(secondKey.objectId),
            eq(DEFAULT_PRODUCT_USAGE),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(secondKey.secret,
            requestOptionsArgumentCaptor.allValues[1].apiKey)
    }

    @Test
    fun retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi() {
        val firstKey = EphemeralKeyFixtures.FIRST

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        // Make sure we're in a good state and that we have the expected customer
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)
        assertEquals(firstKey.objectId, customerSession.customer?.id)

        verify(stripeRepository).retrieveCustomer(
            eq(firstKey.objectId),
            productUsageArgumentCaptor.capture(),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        customerSession.retrieveCurrentCustomer(customerRetrievalListener)

        verify(customerRetrievalListener)
            .onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue

        assertEquals(FIRST_CUSTOMER.id, capturedCustomer.id)
        //  Make sure the value is cached.
        assertEquals(FIRST_CUSTOMER.id, customerSession.customer?.id)
        verifyNoMoreInteractions(stripeRepository)
    }

    @Test
    fun addSourceToCustomer_withUnExpiredCustomer_returnsAddedSource() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)

        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        customerSession.addCustomerSource(
            sourceId = "abc123",
            sourceType = Source.SourceType.CARD,
            productUsage = expectedProductUsage,
            listener = sourceRetrievalListener
        )

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).addCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(expectedProductUsage),
            eq("abc123"),
            eq(Source.SourceType.CARD),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        verify(sourceRetrievalListener)
            .onSourceRetrieved(sourceArgumentCaptor.capture())
        val capturedSource = sourceArgumentCaptor.firstValue
        assertEquals(SourceFixtures.SOURCE_CARD.id, capturedSource.id)
    }

    @Test
    fun addSourceToCustomer_whenApiThrowsError_callsListener() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        setupErrorProxy()
        customerSession.addCustomerSource(
            sourceId = "abc123",
            sourceType = Source.SourceType.CARD,
            productUsage = expectedProductUsage,
            listener = sourceRetrievalListener
        )

        verify(sourceRetrievalListener)
            .onError(404, "The card is invalid", null)
    }

    @Test
    fun removeSourceFromCustomer_withUnExpiredCustomer_returnsRemovedSource() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        customerSession.deleteCustomerSource(
            sourceId = "abc123",
            productUsage = expectedProductUsage,
            listener = sourceRetrievalListener
        )

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).deleteCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(expectedProductUsage),
            eq("abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        verify(sourceRetrievalListener)
            .onSourceRetrieved(sourceArgumentCaptor.capture())
        val capturedSource = sourceArgumentCaptor.firstValue
        assertEquals(SourceFixtures.SOURCE_CARD.id, capturedSource.id)
    }

    @Test
    fun removeSourceFromCustomer_whenApiThrowsError_callsListener() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        setupErrorProxy()
        customerSession.deleteCustomerSource(
            sourceId = "abc123",
            productUsage = expectedProductUsage,
            listener = sourceRetrievalListener
        )

        verify(sourceRetrievalListener)
            .onError(404, "The card does not exist", null)
    }

    @Test
    fun setDefaultSourceForCustomer_withUnExpiredCustomer_returnsCustomerAndClearsLog() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        customerSession.setCustomerDefaultSource(
            "abc123",
            Source.SourceType.CARD,
            setOf(PaymentMethodsActivity.PRODUCT_TOKEN),
            customerRetrievalListener
        )

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).setDefaultCustomerSource(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(setOf(PaymentMethodsActivity.PRODUCT_TOKEN)),
            eq("abc123"),
            eq(Source.SourceType.CARD),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        verify(customerRetrievalListener)
            .onCustomerRetrieved(customerArgumentCaptor.capture())
        val capturedCustomer = customerArgumentCaptor.firstValue
        assertNotNull(SECOND_CUSTOMER)
        assertEquals(SECOND_CUSTOMER.id, capturedCustomer.id)
    }

    @Test
    fun setDefaultSourceForCustomer_whenApiThrows_callsListenerAndClearsLogs() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        setupErrorProxy()
        customerSession.setCustomerDefaultSource(
            "abc123",
            Source.SourceType.CARD,
            customerRetrievalListener
        )

        verify(customerRetrievalListener)
            .onError(405, "auth error", null)
//        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun attachPaymentMethodToCustomer_withUnExpiredCustomer_returnsAddedPaymentMethod() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        customerSession.attachPaymentMethod(
            paymentMethodId = "pm_abc123",
            productUsage = expectedProductUsage,
            listener = paymentMethodRetrievalListener
        )

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).attachPaymentMethod(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(expectedProductUsage),
            eq("pm_abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        verify(paymentMethodRetrievalListener)
            .onPaymentMethodRetrieved(paymentMethodArgumentCaptor.capture())
        val capturedPaymentMethod = paymentMethodArgumentCaptor.firstValue
        assertEquals(PAYMENT_METHOD.id, capturedPaymentMethod.id)
    }

    @Test
    fun attachPaymentMethodToCustomer_whenApiThrowsError_callsListenerOnError() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L
        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        setupErrorProxy()
        customerSession.attachPaymentMethod(
            paymentMethodId = "pm_abc123",
            productUsage = expectedProductUsage,
            listener = paymentMethodRetrievalListener
        )

        verify(paymentMethodRetrievalListener)
            .onError(404, "The payment method is invalid", null)
    }

    @Test
    fun detachPaymentMethodFromCustomer_withUnExpiredCustomer_returnsRemovedPaymentMethod() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        customerSession.detachPaymentMethod(
            paymentMethodId = "pm_abc123",
            productUsage = expectedProductUsage,
            listener = paymentMethodRetrievalListener
        )

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).detachPaymentMethod(
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(expectedProductUsage),
            eq("pm_abc123"),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        verify(paymentMethodRetrievalListener)
            .onPaymentMethodRetrieved(paymentMethodArgumentCaptor.capture())
        val capturedPaymentMethod = paymentMethodArgumentCaptor.firstValue
        assertEquals(PAYMENT_METHOD.id, capturedPaymentMethod.id)
    }

    @Test
    fun detachPaymentMethodFromCustomer_whenApiThrowsError_callsListenerOnError() {
        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        setupErrorProxy()
        customerSession.detachPaymentMethod(
            paymentMethodId = "pm_abc123",
            listener = paymentMethodRetrievalListener
        )

        verify(paymentMethodRetrievalListener)
            .onError(404, "The payment method does not exist", null)
    }

    @Test
    fun getPaymentMethods_withUnExpiredCustomer_returnsAddedPaymentMethod() {
        val expectedProductUsage = setOf(
            AddPaymentMethodActivity.PRODUCT_TOKEN,
            PaymentMethodsActivity.PRODUCT_TOKEN
        )

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })

        val firstCustomerCacheTime = customerSession.customerCacheTime
        val shortIntervalInMilliseconds = 10L

        currentTime = firstCustomerCacheTime + shortIntervalInMilliseconds

        customerSession.getPaymentMethods(
            paymentMethodType = PaymentMethod.Type.Card,
            productUsage = expectedProductUsage,
            listener = paymentMethodsRetrievalListener
        )

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).getPaymentMethods(
            eq(ListPaymentMethodsParams(
                customerId = FIRST_CUSTOMER.id.orEmpty(),
                paymentMethodType = PaymentMethod.Type.Card
            )),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(expectedProductUsage),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

        verify(paymentMethodsRetrievalListener)
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
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            any(),
            any()))
            .thenThrow(APIException(statusCode = 404, message = "The payment method does not exist"))
    }

    private fun createCustomerSession(
        timeSupplier: TimeSupplier = { Calendar.getInstance().timeInMillis },
        ephemeralKeyManagerFactory: EphemeralKeyManager.Factory =
            createEphemeralKeyManagerFactory(timeSupplier)
    ): CustomerSession {
        return CustomerSession(
            ApplicationProvider.getApplicationContext(),
            stripeRepository,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_abc123",
            timeSupplier = timeSupplier,
            threadPoolExecutor = threadPoolExecutor,
            ephemeralKeyManagerFactory = ephemeralKeyManagerFactory
        )
    }

    private fun createEphemeralKeyManagerFactory(
        timeSupplier: TimeSupplier
    ): EphemeralKeyManager.Factory {
        return EphemeralKeyManager.Factory.Default(
            keyProvider = ephemeralKeyProvider,
            shouldPrefetchEphemeralKey = true,
            timeSupplier = timeSupplier
        )
    }

    private class FakeEphemeralKeyManagerFactory(
        private val ephemeralKeyProvider: EphemeralKeyProvider,
        private val ephemeralKey: EphemeralKey
    ) : EphemeralKeyManager.Factory {
        override fun create(
            arg: EphemeralKeyManager.KeyManagerListener
        ): EphemeralKeyManager {
            return EphemeralKeyManager(
                ephemeralKeyProvider,
                arg
            ).also {
                it.ephemeralKey = ephemeralKey
            }
        }
    }

    private companion object {
        private val FIRST_CUSTOMER = CustomerFixtures.CUSTOMER
        private val SECOND_CUSTOMER = CustomerFixtures.OTHER_CUSTOMER

        private val PAYMENT_METHOD = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        private val DEFAULT_CURRENT_TIME =
            TimeUnit.SECONDS.toMillis(EphemeralKeyFixtures.FIRST.expires) +
                TimeUnit.MINUTES.toMillis(2)

        private val DEFAULT_PRODUCT_USAGE = setOf(PaymentMethodsActivity.PRODUCT_TOKEN)
    }
}
