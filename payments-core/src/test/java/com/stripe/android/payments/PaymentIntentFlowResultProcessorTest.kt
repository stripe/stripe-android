package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.PaymentIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.networking.AbsFakeStripeRepository
import com.stripe.android.networking.ApiRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.AfterTest

@RunWith(RobolectricTestRunner::class)
@ExperimentalCoroutinesApi
internal class PaymentIntentFlowResultProcessorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val processor = PaymentIntentFlowResultProcessor(
        ApplicationProvider.getApplicationContext(),
        { ApiKeyFixtures.FAKE_PUBLISHABLE_KEY },
        FakeStripeRepository(),
        Logger.noop(),
        testDispatcher
    )

    @AfterTest
    fun after() {
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `processPaymentIntent() when shouldCancelSource=true should return canceled PaymentIntent`() =
        testDispatcher.runBlockingTest {
            val paymentIntentResult = processor.processResult(
                PaymentFlowResult.Unvalidated(
                    clientSecret = "client_secret",
                    flowOutcome = StripeIntentResult.Outcome.CANCELED,
                    canCancelSource = true
                )
            )

            assertThat(paymentIntentResult)
                .isEqualTo(
                    PaymentIntentResult(
                        intent = PaymentIntentFixtures.CANCELLED,
                        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED,
                    )
                )
        }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun retrievePaymentIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = PaymentIntentFixtures.PI_REQUIRES_REDIRECT

        override suspend fun cancelPaymentIntentSource(
            paymentIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ) = PaymentIntentFixtures.CANCELLED
    }
}
