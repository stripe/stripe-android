package com.stripe.android.payments.core.authentication.threeds2

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.LinearRetryDelaySupplier
import com.stripe.android.model.Stripe3ds2AuthResult
import com.stripe.android.model.Stripe3ds2AuthResultFixtures
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.stripe3ds2.transaction.ChallengeResult
import com.stripe.android.stripe3ds2.transaction.IntentData
import com.stripe.android.stripe3ds2.transactions.UiType
import com.stripe.android.testing.AbsFakeStripeRepository
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class Stripe3ds2BetaHeadersTest {
    @Test
    fun `completion retains merchant beta flags with challenge credentials`() = runTest {
        val configuration = ApiConfiguration.State("pk_test_merchant", "acct_merchant", setOf("challenge_beta=v1"))
        val repository = FakeStripeRepository()
        val analytics = FakeAnalyticsRequestExecutor()
        val processor = DefaultStripe3ds2ChallengeResultProcessor(
            stripeRepository = repository,
            analyticsRequestExecutor = analytics,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext<Application>(),
                publishableKeyProvider = { configuration.publishableKey },
            ),
            retryDelaySupplier = LinearRetryDelaySupplier(),
            logger = Logger.noop(),
            workContext = UnconfinedTestDispatcher(testScheduler),
            apiConfiguration = configuration,
        )

        processor.process(
            ChallengeResult.Succeeded(
                UiType.Text.code,
                UiType.Text,
                IntentData("client_secret", "src_challenge", "pk_test_challenge", "acct_challenge"),
            )
        )

        val call = repository.calls.awaitItem()
        assertThat(call.first).isEqualTo("src_challenge")
        assertThat(call.second.apiKey).isEqualTo("pk_test_challenge")
        assertThat(call.second.stripeAccount).isEqualTo("acct_challenge")
        assertThat(call.second.betas).containsExactly("challenge_beta=v1")
        assertThat(analytics.calls.awaitItem().params["event"])
            .isEqualTo("stripe_android.3ds2_challenge_flow_completed")
        assertThat(analytics.calls.awaitItem().params["event"])
            .isEqualTo("stripe_android.3ds2_challenge_flow_presented")
        repository.calls.ensureAllEventsConsumed()
        analytics.calls.ensureAllEventsConsumed()
    }

    private class FakeStripeRepository : AbsFakeStripeRepository() {
        val calls = Turbine<Pair<String, ApiRequest.Options>>()

        override suspend fun complete3ds2Auth(
            sourceId: String,
            requestOptions: ApiRequest.Options,
        ): Result<Stripe3ds2AuthResult> {
            calls.add(sourceId to requestOptions)
            return Result.success(Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION)
        }
    }

    private class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val calls = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            calls.add(request)
        }
    }
}
