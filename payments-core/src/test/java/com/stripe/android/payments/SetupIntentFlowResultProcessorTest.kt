package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.model.SetupIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.networking.StripeRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class SetupIntentFlowResultProcessorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockStripeRepository: StripeRepository = mock()

    private val processor = SetupIntentFlowResultProcessor(
        ApplicationProvider.getApplicationContext(),
        { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        mockStripeRepository,
        Logger.noop(),
        testDispatcher
    )

    @Test
    fun `processResult() when shouldCancelSource=true should return canceled SetupIntent`() =
        runTest {
            whenever(mockStripeRepository.retrieveSetupIntent(any(), any(), any())).thenReturn(
                SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT
            )
            whenever(mockStripeRepository.cancelSetupIntentSource(any(), any(), any())).thenReturn(
                SetupIntentFixtures.CANCELLED
            )

            val setupIntentResult = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = "client_secret",
                    flowOutcome = StripeIntentResult.Outcome.CANCELED,
                    canCancelSource = true
                )
            )

            assertThat(setupIntentResult)
                .isEqualTo(
                    SetupIntentResult(
                        intent = SetupIntentFixtures.CANCELLED,
                        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED
                    )
                )
        }

    @Test
    fun `3ds2 canceled with processing intent should succeed`() =
        runTest {
            whenever(mockStripeRepository.retrieveSetupIntent(any(), any(), any())).thenReturn(
                SetupIntentFixtures.SI_3DS2_PROCESSING,
                SetupIntentFixtures.SI_3DS2_SUCCEEDED
            )

            val clientSecret = "pi_3L8WOsLu5o3P18Zp191FpRSy_secret_5JIwIT1ooCwRm28AwreUAc6N4"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val result = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            )

            verify(
                mockStripeRepository,
                atLeastOnce()
            ).retrieveSetupIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            assertThat(result)
                .isEqualTo(
                    SetupIntentResult(
                        SetupIntentFixtures.SI_3DS2_SUCCEEDED,
                        StripeIntentResult.Outcome.SUCCEEDED,
                        null
                    )
                )
        }

    @Test
    fun `3ds2 canceled with succeeded intent should succeed`() =
        runTest {
            whenever(mockStripeRepository.retrieveSetupIntent(any(), any(), any())).thenReturn(
                SetupIntentFixtures.SI_3DS2_SUCCEEDED
            )

            val clientSecret = "pi_3L8WOsLu5o3P18Zp191FpRSy_secret_5JIwIT1ooCwRm28AwreUAc6N4"
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val result = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            )

            verify(mockStripeRepository).retrieveSetupIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            assertThat(result)
                .isEqualTo(
                    SetupIntentResult(
                        SetupIntentFixtures.SI_3DS2_SUCCEEDED,
                        StripeIntentResult.Outcome.SUCCEEDED,
                        null
                    )
                )
        }

    @Test
    fun `3ds2 canceled reaches max retry with processing intent should cancel`() =
        runTest(testDispatcher) {
            val intent = SetupIntentFixtures.SI_3DS2_PROCESSING.copy(
                status = StripeIntent.Status.RequiresAction
            )
            whenever(mockStripeRepository.retrieveSetupIntent(any(), any(), any())).thenReturn(
                intent
            )

            val clientSecret = requireNotNull(
                intent.clientSecret
            )
            val requestOptions = ApiRequest.Options(apiKey = ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)

            val result = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = clientSecret,
                    flowOutcome = StripeIntentResult.Outcome.CANCELED
                )
            )

            verify(
                mockStripeRepository,
                times(PaymentFlowResultProcessor.MAX_RETRIES + 1)
            ).retrieveSetupIntent(
                eq(clientSecret),
                eq(requestOptions),
                eq(PaymentFlowResultProcessor.EXPAND_PAYMENT_METHOD)
            )

            assertThat(result)
                .isEqualTo(
                    SetupIntentResult(
                        intent,
                        StripeIntentResult.Outcome.CANCELED,
                        null
                    )
                )
        }
}
