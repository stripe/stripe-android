package com.stripe.android.payments.core.authentication.threeds2

import android.content.Context
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class Stripe3ds2AnalyticsKeyTest {
    @Test
    fun `challenge analytics use merchant key while completion uses authentication key`() = runTest {
        val executor = FakeAnalyticsRequestExecutor()
        val repository = FakeStripeRepository()
        val processor = DefaultStripe3ds2ChallengeResultProcessor(
            stripeRepository = repository,
            analyticsRequestExecutor = executor,
            paymentAnalyticsRequestFactory = PaymentAnalyticsRequestFactory(
                context = ApplicationProvider.getApplicationContext<Context>(),
                defaultProductUsageTokens = emptySet(),
            ),
            retryDelaySupplier = LinearRetryDelaySupplier(),
            logger = Logger.noop(),
            workContext = StandardTestDispatcher(testScheduler),
            apiConfiguration = ApiConfiguration.State("pk_test_merchant", null),
        )

        processor.process(
            ChallengeResult.Succeeded(
                uiTypeCode = UiType.Text.code,
                initialUiType = UiType.Text,
                intentData = IntentData("client_secret", "src_123", "pk_test_authentication", null),
            )
        )

        val completed = executor.requests.awaitItem()
        val presented = executor.requests.awaitItem()
        assertThat(completed.params).containsEntry("publishable_key", "pk_test_merchant")
        assertThat(presented.params).containsEntry("publishable_key", "pk_test_merchant")
        assertThat(repository.completionCalls.awaitItem().apiKey).isEqualTo("pk_test_authentication")
        executor.requests.ensureAllEventsConsumed()
        repository.completionCalls.ensureAllEventsConsumed()
    }

    internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }
    }

    internal class FakeStripeRepository : AbsFakeStripeRepository() {
        val completionCalls = Turbine<ApiRequest.Options>()

        override suspend fun complete3ds2Auth(
            sourceId: String,
            requestOptions: ApiRequest.Options,
        ): Result<Stripe3ds2AuthResult> {
            completionCalls.add(requestOptions)
            return Result.success(Stripe3ds2AuthResultFixtures.CHALLENGE_COMPLETION)
        }
    }
}
