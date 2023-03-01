package com.stripe.android.paymentsheet.repositories

import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.ElementsSessionParams
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
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
            ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
            )
        )

        val locale = Locale.GERMANY
        val session = withLocale(locale) {
            createRepository().get(
                initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                    clientSecret = "client_secret",
                ),
                configuration = null,
            )
        }

        val argumentCaptor: KArgumentCaptor<ElementsSessionParams> = argumentCaptor()

        verify(stripeRepository).retrieveElementsSession(argumentCaptor.capture(), any())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        assertThat(argumentCaptor.firstValue.locale).isEqualTo(locale.toLanguageTag())
    }

    @Test
    fun `get with locale when element session fails should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(
                stripeRepository.retrieveElementsSession(any(), any())
            ).thenReturn(null)

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val session = withLocale(Locale.ITALY) {
                createRepository().get(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                        clientSecret = "client_secret",
                    ),
                    configuration = null,
                )
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
            ).thenThrow(RuntimeException())

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val session = withLocale(Locale.ITALY) {
                createRepository().get(
                    initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                        clientSecret = "client_secret",
                    ),
                    configuration = null,
                )
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
            ElementsSession(
                linkSettings = null,
                paymentMethodSpecs = null,
                stripeIntent = PaymentIntentFixtures.PI_WITH_SHIPPING,
            )
        )

        val session = ElementsSessionRepository.Api(
            stripeRepository,
            { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
            testDispatcher,
        ).get(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = "client_secret",
            ),
            configuration = null,
        )

        val argumentCaptor: KArgumentCaptor<ElementsSessionParams> = argumentCaptor()

        verify(stripeRepository).retrieveElementsSession(argumentCaptor.capture(), any())
        verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
        assertThat(session.stripeIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)

        val defaultLocale = LocaleListCompat.getAdjustedDefault()[0]?.toLanguageTag()
        assertThat(argumentCaptor.firstValue.locale).isEqualTo(defaultLocale)
    }

    private fun createRepository() = ElementsSessionRepository.Api(
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
