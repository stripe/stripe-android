package com.stripe.android.paymentsheet.repositories

import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.exception.APIException
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
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
                savedPaymentMethodSelectionId = null,
            ).getOrThrow()
        }

        val argumentCaptor: KArgumentCaptor<ElementsSessionParams> = argumentCaptor()

        verify(stripeRepository).retrieveElementsSession(argumentCaptor.capture(), any())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        assertThat(session.elementsSessionId).isEqualTo("session_1234")
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
                    savedPaymentMethodSelectionId = null,
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
                    savedPaymentMethodSelectionId = null,
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
        ).get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = null,
        ).getOrThrow()

        val argumentCaptor: KArgumentCaptor<ElementsSessionParams> = argumentCaptor()

        verify(stripeRepository).retrieveElementsSession(argumentCaptor.capture(), any())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)

        val defaultLocale = LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag()
        assertThat(argumentCaptor.firstValue.locale).isEqualTo(defaultLocale)
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
            savedPaymentMethodSelectionId = null,
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
            savedPaymentMethodSelectionId = null,
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
            savedPaymentMethodSelectionId = null,
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    customerSessionClientSecret = "customer_session_client_secret",
                    externalPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = null,
                )
            ),
            options = any()
        )
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
        )

        repository.get(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = "client_secret"
            ),
            customer = null,
            externalPaymentMethods = emptyList(),
            savedPaymentMethodSelectionId = "pm_123",
        )

        verify(stripeRepository).retrieveElementsSession(
            params = eq(
                ElementsSessionParams.PaymentIntentType(
                    clientSecret = "client_secret",
                    externalPaymentMethods = emptyList(),
                    savedPaymentMethodSelectionId = "pm_123",
                )
            ),
            options = any()
        )
    }

    private fun createRepository() = RealElementsSessionRepository(
        stripeRepository,
        { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
        testDispatcher,
    )

    private inline fun <T> withLocale(locale: Locale, block: () -> T): T {
        val original = Locale.getDefault()
        Locale.setDefault(locale)
        val result = block()
        Locale.setDefault(original)
        return result
    }
}
