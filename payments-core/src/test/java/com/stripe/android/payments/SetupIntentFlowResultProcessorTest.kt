package com.stripe.android.payments

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.Logger
import com.stripe.android.SetupIntentResult
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.SetupIntentFixtures
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
internal class SetupIntentFlowResultProcessorTest {
    private val testDispatcher = TestCoroutineDispatcher()

    private val processor = SetupIntentFlowResultProcessor(
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
    fun `processResult() when shouldCancelSource=true should return canceled SetupIntent`() =
        testDispatcher.runBlockingTest {
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
                        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED,
                    )
                )
        }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        override suspend fun retrieveSetupIntent(
            clientSecret: String,
            options: ApiRequest.Options,
            expandFields: List<String>
        ) = SetupIntentFixtures.SI_NEXT_ACTION_REDIRECT

        override suspend fun cancelSetupIntentSource(
            setupIntentId: String,
            sourceId: String,
            options: ApiRequest.Options
        ) = SetupIntentFixtures.CANCELLED
    }
}
