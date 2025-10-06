package com.stripe.android.paymentsheet.repositories

import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.SharedPaymentTokenSessionPreview
import com.stripe.android.core.exception.APIException
import com.stripe.android.core.networking.AnalyticsRequestFactory
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.DeferredIntentParams
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.PaymentMethodOptionsSetupFutureUsagePreview
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentmethodoptions.setupfutureusage.toJsonObjectString
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import java.util.UUID
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
internal class ElementsSessionRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeRepository = mock<StripeRepository>()

    @Test
    fun `get with locale should retrieve with element session`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                    elementsSessionId = "session_1234"
                )
            )
        )

        val locale = Locale.GERMANY
        val session = withLocale(locale) {
            createRepository().get(
                initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret",
                ),
                customer = null,
                externalPaymentMethods = emptyList(),
                customPaymentMethods = emptyList(),
                savedPaymentMethodSelectionId = null,
                countryOverride = null,
            ).getOrThrow()
        }

        val argumentCaptor: KArgumentCaptor<ElementsSessionParams> = argumentCaptor()

        verify(stripeRepository).retrieveElementsSession(argumentCaptor.capture(), any())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        assertThat(session.elementsSessionId).isEqualTo("session_1234")
        assertThat(argumentCaptor.firstValue.appId).isEqualTo(APP_ID)
        assertThat(argumentCaptor.firstValue.locale).isEqualTo(locale.toLanguageTag())
    }

    @Test
    fun `get with locale when element session fails should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(
                stripeRepository.retrieveElementsSession(any(), any())
            ).thenReturn(Result.failure(APIException()))

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(Result.success(PaymentIntentFixtures.PI_WITH_SHIPPING))

            val session = withLocale(Locale.ITALY) {
                createRepository().get(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "client_secret",
                    ),
                    customer = null,
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                    countryOverride = null,
                ).getOrThrow()
            }

            verify(stripeRepository).retrieveElementsSession(any(), any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
            assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        }

    @Test
    fun `get with locale when element session fails with exception should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(
                stripeRepository.retrieveElementsSession(any(), any())
            ).thenReturn(Result.failure(RuntimeException()))

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(Result.success(PaymentIntentFixtures.PI_WITH_SHIPPING))

            val session = withLocale(Locale.ITALY) {
                createRepository().get(
                    initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                        clientSecret = "client_secret",
                    ),
                    customer = null,
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                    countryOverride = null,
                ).getOrThrow()
            }

            verify(stripeRepository).retrieveElementsSession(any(), any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
            assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        }

    @Test
    fun `get without locale should retrieve ordered payment methods in default locale`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val session = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        ).getOrThrow()

        val argumentCaptor: KArgumentCaptor<ElementsSessionParams> = argumentCaptor()

        verify(stripeRepository).retrieveElementsSession(argumentCaptor.capture(), any())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)

        val defaultLocale = LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag()
        assertThat(argumentCaptor.firstValue.locale).isEqualTo(defaultLocale)
        assertThat(argumentCaptor.firstValue.appId)
    }

    @Test
    fun `Handles deferred intent elements session lookup failure gracefully`() = runTest {
        val endpointException = APIException(message = "this didn't work")
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.failure(endpointException)
        )

        val session = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    )
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        )

        assertThat(session.isSuccess).isTrue()
        assertThat(session.getOrNull()?.stripeIntent?.paymentMethodTypes).containsExactly("card")
    }

    @Test
    fun `Deferred intent elements session failure uses payment method types if specified`() = runTest {
        val endpointException = APIException(message = "this didn't work")
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.failure(endpointException)
        )
        val expectedPaymentMethodTypes = listOf("card", "amazon_pay")

        val session = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    paymentMethodTypes = expectedPaymentMethodTypes
                )
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        )

        assertThat(session.isSuccess).isTrue()
        assertThat(session.getOrNull()?.stripeIntent?.paymentMethodTypes).isEqualTo(expectedPaymentMethodTypes)
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `Verify customer session client secret is passed to 'StripeRepository'`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val repository = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                id = "cus_1",
                clientSecret = "customer_session_client_secret"
            ),
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    customerSessionClientSecret = "customer_session_client_secret",
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                    appId = APP_ID,
                    mobileSessionId = MOBILE_SESSION_ID,
                )
            ),
            options = any()
        )
    }

    @OptIn(ExperimentalCustomerSessionApi::class)
    @Test
    fun `Verify legacy customer ephemeral key is passed to 'StripeRepository'`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val repository = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = PaymentSheet.CustomerConfiguration(
                id = "cus_1",
                ephemeralKeySecret = "legacy_customer_ephemeral_key"
            ),
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        )

        val captor = argumentCaptor<ElementsSessionParams.PaymentIntentType>()

        verify(stripeRepository).retrieveElementsSession(
            params = captor.capture(),
            options = any()
        )

        assertThat(captor.firstValue.legacyCustomerEphemeralKey).isEqualTo("legacy_customer_ephemeral_key")
    }

    @Test
    fun `Verify default payment method id is passed to 'StripeRepository'`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val repository = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = "pm_123",
            countryOverride = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = "pm_123",
                    appId = APP_ID,
                    mobileSessionId = MOBILE_SESSION_ID,
                )
            ),
            options = any()
        )
    }

    @Test
    fun `Verify custom payment methods ids are passed to 'StripeRepository'`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val repository = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = listOf(
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_123",
                    subtitle = "Pay now".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_456",
                    subtitle = "Pay later".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_789",
                    subtitle = "Pay never".resolvableString,
                    disableBillingDetailCollection = true,
                ),
            ),
            savedPaymentMethodSelectionId = "pm_123",
            countryOverride = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = listOf("cpmt_123", "cpmt_456", "cpmt_789"),
                    savedPaymentMethodSelectionId = "pm_123",
                    appId = APP_ID,
                    mobileSessionId = MOBILE_SESSION_ID,
                )
            ),
            options = any()
        )
    }

    @OptIn(SharedPaymentTokenSessionPreview::class)
    @Test
    fun `Verify seller details is passed to 'StripeRepository'`() = runTest {
        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(ElementsSession.createFromFallback(PaymentIntentFixtures.PI_WITH_SHIPPING, null))
        )

        val repository = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    sharedPaymentTokenSessionWithMode =
                    PaymentSheet.IntentConfiguration.Mode.Payment(amount = 1234, currency = "cad"),
                    sellerDetails = PaymentSheet.IntentConfiguration.SellerDetails(
                        businessName = "My business, Inc.",
                        networkId = "network_123",
                        externalId = "external_123",
                    ),
                ),
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.DeferredIntentType(
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                    appId = APP_ID,
                    mobileSessionId = MOBILE_SESSION_ID,
                    deferredIntentParams = DeferredIntentParams(
                        mode = DeferredIntentParams.Mode.Payment(
                            amount = 1234,
                            currency = "cad",
                            captureMethod = PaymentIntent.CaptureMethod.Automatic,
                            setupFutureUsage = null,
                            paymentMethodOptionsJsonString = null
                        ),
                        paymentMethodTypes = emptyList(),
                        paymentMethodConfigurationId = null,
                        onBehalfOf = null,
                    ),
                    sellerDetails = ElementsSessionParams.SellerDetails(
                        networkId = "network_123",
                        externalId = "external_123",
                    )
                )
            ),
            options = any()
        )
    }

    @Test
    fun `Verify mobile session ID is passed to 'StripeRepository'`() = runTest {
        AnalyticsRequestFactory.setSessionId(UUID.fromString("537a88ff-a54f-42cc-ba52-c7c5623730b6"))

        whenever(
            stripeRepository.retrieveElementsSession(any(), any())
        ).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val repository = RealElementsSessionRepository(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
            { MOBILE_SESSION_ID },
            appId = APP_ID
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = listOf(
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_123",
                    subtitle = "Pay now".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_456",
                    subtitle = "Pay later".resolvableString,
                    disableBillingDetailCollection = true,
                ),
                PaymentSheet.CustomPaymentMethod(
                    id = "cpmt_789",
                    subtitle = "Pay never".resolvableString,
                    disableBillingDetailCollection = true,
                ),
            ),
            savedPaymentMethodSelectionId = "pm_123",
            countryOverride = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = listOf("cpmt_123", "cpmt_456", "cpmt_789"),
                    savedPaymentMethodSelectionId = "pm_123",
                    mobileSessionId = MOBILE_SESSION_ID,
                    appId = APP_ID
                )
            ),
            options = any()
        )
    }

    @OptIn(PaymentMethodOptionsSetupFutureUsagePreview::class)
    @Test
    fun `Verify PMO SFU params are passed to 'StripeRepository'`() = runTest {
        whenever(stripeRepository.retrieveElementsSession(any(), any())).thenReturn(
            Result.success(
                ElementsSession.createFromFallback(
                    stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
                    sessionsError = null,
                )
            )
        )

        val paymentMethodOptions = PaymentSheet.IntentConfiguration.Mode.Payment.PaymentMethodOptions(
            setupFutureUsageValues = mapOf(
                PaymentMethod.Type.Card to PaymentSheet.IntentConfiguration.SetupFutureUse.OnSession,
                PaymentMethod.Type.Affirm to PaymentSheet.IntentConfiguration.SetupFutureUse.None,
                PaymentMethod.Type.AmazonPay
                    to PaymentSheet.IntentConfiguration.SetupFutureUse.OffSession,
            )
        )

        createRepository().get(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1000,
                        currency = "usd",
                        setupFutureUse = null,
                        paymentMethodOptions = paymentMethodOptions
                    ),
                )
            ),
            customer = null,
            customPaymentMethods = listOf(),
            externalPaymentMethods = listOf(),
            savedPaymentMethodSelectionId = null,
            countryOverride = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.DeferredIntentType(
                    deferredIntentParams = DeferredIntentParams(
                        mode = DeferredIntentParams.Mode.Payment(
                            amount = 1000,
                            currency = "usd",
                            setupFutureUsage = null,
                            captureMethod = PaymentIntent.CaptureMethod.Automatic,
                            paymentMethodOptionsJsonString = paymentMethodOptions.toJsonObjectString()
                        ),
                        paymentMethodTypes = listOf(),
                        paymentMethodConfigurationId = null,
                        onBehalfOf = null
                    ),
                    externalPaymentMethods = emptyList(),
                    customPaymentMethods = listOf(),
                    savedPaymentMethodSelectionId = null,
                    mobileSessionId = MOBILE_SESSION_ID,
                    appId = APP_ID
                )
            ),
            options = any()
        )
    }

    private fun createRepository() = RealElementsSessionRepository(
        stripeRepository,
        { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
        testDispatcher,
        { MOBILE_SESSION_ID },
        appId = APP_ID
    )

    private inline fun <T> withLocale(locale: Locale, block: () -> T): T {
        val original = Locale.getDefault()
        Locale.setDefault(locale)
        val result = block()
        Locale.setDefault(original)
        return result
    }

    companion object {
        private const val APP_ID = "com.app.id"
        private const val MOBILE_SESSION_ID = "session_123"
    }
}
