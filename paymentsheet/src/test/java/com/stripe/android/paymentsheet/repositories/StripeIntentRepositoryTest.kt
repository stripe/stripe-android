package com.stripe.android.paymentsheet.repositories

import androidx.core.os.LocaleListCompat
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.lang.RuntimeException
import java.util.Locale
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
internal class StripeIntentRepositoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeRepository = mock<StripeRepository>()

    @Test
    fun `get with locale should retrieve with ordered payment methods`() =
        runTest {
            whenever(
                stripeRepository
                    .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            ).thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val locale = Locale.GERMANY
            val paymentIntent =
                createRepository(locale).get(PaymentIntentClientSecret("client_secret"))

            val localeArgumentCaptor: KArgumentCaptor<Locale> = argumentCaptor()

            verify(stripeRepository)
                .retrievePaymentIntentWithOrderedPaymentMethods(
                    any(), any(), localeArgumentCaptor.capture()
                )
            verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
            assertThat(paymentIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
            assertThat(localeArgumentCaptor.firstValue).isEqualTo(locale)
        }

    @Test
    fun `get with locale when ordered payment methods fails should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(
                stripeRepository
                    .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            ).thenReturn(null)

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val paymentIntent =
                createRepository(Locale.ITALY).get(PaymentIntentClientSecret("client_secret"))

            verify(stripeRepository)
                .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
            assertThat(paymentIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        }

    @Test
    fun `get with locale when ordered payment methods fails with exception should fallback to retrievePaymentIntent()`() =
        runTest {
            whenever(
                stripeRepository
                    .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            ).thenThrow(RuntimeException())

            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val paymentIntent =
                createRepository(Locale.ITALY).get(PaymentIntentClientSecret("client_secret"))

            verify(stripeRepository)
                .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
            assertThat(paymentIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)
        }

    @Test
    fun `get with null locale should call retrievePaymentIntent()`() =
        runTest {
            whenever(stripeRepository.retrievePaymentIntent(any(), any(), any()))
                .thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            createRepository().get(PaymentIntentClientSecret("client_secret"))

            verify(stripeRepository, never())
                .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            verify(stripeRepository).retrievePaymentIntent(any(), any(), any())
        }

    @Test
    fun `get without locale should retrieve ordered payment methods in default locale`() =
        runTest {
            whenever(
                stripeRepository
                    .retrievePaymentIntentWithOrderedPaymentMethods(any(), any(), any())
            ).thenReturn(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val paymentIntent = StripeIntentRepository.Api(
                stripeRepository,
                { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
                testDispatcher,
                Locale.US
            ).get(PaymentIntentClientSecret("client_secret"))

            val localeArgumentCaptor: KArgumentCaptor<Locale> = argumentCaptor()

            verify(stripeRepository)
                .retrievePaymentIntentWithOrderedPaymentMethods(
                    any(), any(), localeArgumentCaptor.capture()
                )
            verify(stripeRepository, never()).retrievePaymentIntent(any(), any(), any())
            assertThat(paymentIntent).isEqualTo(PaymentIntentFixtures.PI_WITH_SHIPPING)

            val defaultLocale = LocaleListCompat.getAdjustedDefault()[0]
            assertThat(localeArgumentCaptor.firstValue).isEqualTo(defaultLocale)
        }

    private fun createRepository(locale: Locale? = null) = StripeIntentRepository.Api(
        stripeRepository,
        { PaymentConfiguration(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY) },
        testDispatcher,
        locale
    )
}
