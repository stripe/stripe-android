package com.stripe.android

import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.Customer
import com.stripe.android.model.CustomerFixtures
import com.stripe.android.model.ListPaymentMethodsParams
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.Source
import com.stripe.android.model.SourceFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.testharness.TestEphemeralKeyProvider
import com.stripe.android.utils.TestUtils.idleLooper
import com.stripe.android.view.AddPaymentMethodActivity
import com.stripe.android.view.PaymentMethodsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
internal class CustomerSessionTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val stripeRepository: StripeRepository = mock()

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
        Dispatchers.setMain(testDispatcher)
        runBlocking {
            whenever(stripeRepository.retrieveCustomer(any(), any(), any()))
                .thenReturn(
                    Result.success(FIRST_CUSTOMER),
                    Result.success(SECOND_CUSTOMER),
                )

            whenever(
                stripeRepository.addCustomerSource(
                    any(),
                    eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(Result.success(SourceFixtures.SOURCE_CARD))

            whenever(
                stripeRepository.deleteCustomerSource(
                    any(),
                    eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(Result.success(SourceFixtures.SOURCE_CARD))

            whenever(
                stripeRepository.setDefaultCustomerSource(
                    any(),
                    eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                    any(),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(Result.success(SECOND_CUSTOMER))

            whenever(
                stripeRepository.attachPaymentMethod(
                    customerId = any(),
                    productUsageTokens = any(),
                    paymentMethodId = any(),
                    requestOptions = any()
                )
            ).thenReturn(Result.success(PAYMENT_METHOD))

            whenever(
                stripeRepository.detachPaymentMethod(
                    productUsageTokens = any(),
                    paymentMethodId = any(),
                    requestOptions = any()
                )
            ).thenReturn(Result.success(PAYMENT_METHOD))

            whenever(
                stripeRepository.getPaymentMethods(
                    listPaymentMethodsParams = any(),
                    productUsageTokens = any(),
                    requestOptions = any()
                )
            ).thenReturn(Result.success(listOf(PAYMENT_METHOD)))

            whenever(
                stripeRepository.setCustomerShippingInfo(
                    any(),
                    eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                    any(),
                    any(),
                    any()
                )
            ).thenReturn(Result.success(FIRST_CUSTOMER))
        }
    }

    @Test
    fun getInstance_withoutInitializing_throwsException() {
        CustomerSession.clearInstance()

        assertFailsWith<IllegalStateException> { CustomerSession.getInstance() }
    }

    @Test
    fun addProductUsageTokenIfValid_whenValid_addsExpectedTokens() = runTest {
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
        idleLooper()

        verify(stripeRepository).attachPaymentMethod(
            customerId = any(),
            productUsageTokens = eq(DEFAULT_PRODUCT_USAGE),
            paymentMethodId = any(),
            requestOptions = any()
        )
    }

    @Test
    fun create_withoutInvokingFunctions_fetchesKeyAndCustomer() = runTest {
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession()
        idleLooper()

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
    fun setCustomerShippingInfo_withValidInfo_callsWithExpectedArgs() = runTest {
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
            }
        )
        idleLooper()

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).setCustomerShippingInfo(
            eq(FIRST_CUSTOMER.id.orEmpty()),
            eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
            eq(DEFAULT_PRODUCT_USAGE),
            eq(shippingInformation),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            EphemeralKeyFixtures.FIRST.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )
    }

    @Test
    fun retrieveCustomer_withExpiredCache_updatesCustomer() = runTest {
        val firstKey = EphemeralKeyFixtures.FIRST
        val secondKey = EphemeralKeyFixtures.SECOND

        val firstExpiryTimeInMillis = TimeUnit.SECONDS.toMillis(firstKey.expires)
        var currentTime = firstExpiryTimeInMillis - 100L

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(
            timeSupplier = { currentTime }
        )
        idleLooper()
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)

        val firstCustomerCacheTime = customerSession.customerCacheTime
        assertEquals(firstExpiryTimeInMillis - 100L, firstCustomerCacheTime)
        val timeForCustomerToExpire = TimeUnit.MINUTES.toMillis(2)

        currentTime = firstCustomerCacheTime + timeForCustomerToExpire

        // We want to make sure that the next ephemeral key will be different.
        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.SECOND_JSON)

        // The key manager should think it is necessary to update the key,
        // because the first one was expired.
        customerSession
            .retrieveCurrentCustomer(DEFAULT_PRODUCT_USAGE, customerRetrievalListener)
        idleLooper()

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
        assertEquals(
            firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )
        verify(stripeRepository).retrieveCustomer(
            eq(secondKey.objectId),
            eq(DEFAULT_PRODUCT_USAGE),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            secondKey.secret,
            requestOptionsArgumentCaptor.allValues[1].apiKey
        )
    }

    @Test
    fun retrieveCustomer_withUnExpiredCache_returnsCustomerWithoutHittingApi() = runTest {
        val firstKey = EphemeralKeyFixtures.FIRST

        var currentTime = DEFAULT_CURRENT_TIME

        ephemeralKeyProvider.setNextRawEphemeralKey(EphemeralKeyFixtures.FIRST_JSON)
        val customerSession = createCustomerSession(timeSupplier = { currentTime })
        idleLooper()

        // Make sure we're in a good state and that we have the expected customer
        assertEquals(firstKey.objectId, FIRST_CUSTOMER.id)
        assertEquals(firstKey.objectId, customerSession.customer?.id)

        verify(stripeRepository).retrieveCustomer(
            eq(firstKey.objectId),
            productUsageArgumentCaptor.capture(),
            requestOptionsArgumentCaptor.capture()
        )
        assertEquals(
            firstKey.secret,
            requestOptionsArgumentCaptor.firstValue.apiKey
        )

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
    fun addSourceToCustomer_withUnExpiredCustomer_returnsAddedSource() = runTest {
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
        idleLooper()

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
    fun addSourceToCustomer_whenApiThrowsError_callsListener() = runTest {
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
        idleLooper()

        verify(sourceRetrievalListener)
            .onError(404, "The card is invalid", null)
    }

    @Test
    fun removeSourceFromCustomer_withUnExpiredCustomer_returnsRemovedSource() = runTest {
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
        idleLooper()

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
    fun removeSourceFromCustomer_whenApiThrowsError_callsListener() = runTest {
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
        idleLooper()

        verify(sourceRetrievalListener)
            .onError(404, "The card does not exist", null)
    }

    @Test
    fun setDefaultSourceForCustomer_withUnExpiredCustomer_returnsCustomerAndClearsLog() = runTest {
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
        idleLooper()

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
    fun setDefaultSourceForCustomer_whenApiThrows_callsListenerAndClearsLogs() = runTest {
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
        idleLooper()

        verify(customerRetrievalListener)
            .onError(405, "auth error", null)
//        assertTrue(customerSession.productUsageTokens.isEmpty())
    }

    @Test
    fun attachPaymentMethodToCustomer_withUnExpiredCustomer_returnsAddedPaymentMethod() = runTest {
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
        idleLooper()

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).attachPaymentMethod(
            customerId = eq(FIRST_CUSTOMER.id.orEmpty()),
            productUsageTokens = eq(expectedProductUsage),
            paymentMethodId = eq("pm_abc123"),
            requestOptions = requestOptionsArgumentCaptor.capture()
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
    fun attachPaymentMethodToCustomer_whenApiThrowsError_callsListenerOnError() = runTest {
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
        idleLooper()

        verify(paymentMethodRetrievalListener)
            .onError(404, "The payment method is invalid", null)
    }

    @Test
    fun detachPaymentMethodFromCustomer_withUnExpiredCustomer_returnsRemovedPaymentMethod() = runTest {
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
        idleLooper()

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).detachPaymentMethod(
            productUsageTokens = eq(expectedProductUsage),
            paymentMethodId = eq("pm_abc123"),
            requestOptions = requestOptionsArgumentCaptor.capture()
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
    fun detachPaymentMethodFromCustomer_whenApiThrowsError_callsListenerOnError() = runTest {
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
        idleLooper()

        verify(paymentMethodRetrievalListener)
            .onError(404, "The payment method does not exist", null)
    }

    @Test
    fun getPaymentMethods_withUnExpiredCustomer_returnsAddedPaymentMethod() = runTest {
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
        idleLooper()

        assertNotNull(FIRST_CUSTOMER.id)
        verify(stripeRepository).getPaymentMethods(
            listPaymentMethodsParams = eq(
                ListPaymentMethodsParams(
                    customerId = FIRST_CUSTOMER.id.orEmpty(),
                    paymentMethodType = PaymentMethod.Type.Card
                )
            ),
            productUsageTokens = eq(expectedProductUsage),
            requestOptions = requestOptionsArgumentCaptor.capture()
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

    private suspend fun setupErrorProxy() {
        whenever(
            stripeRepository.addCustomerSource(
                any(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Result.failure(APIException(statusCode = 404, message = "The card is invalid"))
        )

        whenever(
            stripeRepository.deleteCustomerSource(
                any(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                any(),
                any(),
                any()
            )
        ).thenReturn(
            Result.failure(APIException(statusCode = 404, message = "The card does not exist"))
        )

        whenever(
            stripeRepository.setDefaultCustomerSource(
                any(),
                eq(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(Result.failure(APIException(statusCode = 405, message = "auth error")))

        whenever(
            stripeRepository.attachPaymentMethod(
                customerId = any(),
                productUsageTokens = any(),
                paymentMethodId = any(),
                requestOptions = any()
            )
        ).thenReturn(
            Result.failure(APIException(statusCode = 404, message = "The payment method is invalid"))
        )

        whenever(
            stripeRepository.detachPaymentMethod(
                productUsageTokens = any(),
                paymentMethodId = any(),
                requestOptions = any()
            )
        ).thenReturn(
            Result.failure(APIException(statusCode = 404, message = "The payment method does not exist"))
        )

        whenever(
            stripeRepository.getPaymentMethods(
                listPaymentMethodsParams = any(),
                productUsageTokens = any(),
                requestOptions = any()
            )
        ).thenReturn(
            Result.failure(APIException(statusCode = 404, message = "The payment method does not exist"))
        )
    }

    private fun createCustomerSession(
        timeSupplier: TimeSupplier = { Calendar.getInstance().timeInMillis },
        ephemeralKeyManagerFactory: EphemeralKeyManager.Factory =
            createEphemeralKeyManagerFactory(timeSupplier)
    ): CustomerSession {
        return CustomerSession(
            stripeRepository,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
            "acct_abc123",
            timeSupplier = timeSupplier,
            workContext = testDispatcher,
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
