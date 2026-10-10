package com.stripe.android.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.frauddetection.FraudDetectionData
import com.stripe.android.core.frauddetection.FraudDetectionDataRepository
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.AnalyticsRequestExecutor
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import com.stripe.android.model.PaymentMethodUpdateParams
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class StripeApiRepositoryAnalyticsKeyTest {
    @Test
    fun `payment method update keeps ephemeral key out of analytics`() = runTest {
        val networkClient = FakeStripeNetworkClient()
        val executor = FakeAnalyticsRequestExecutor()
        val fraudRepository = FakeFraudDetectionDataRepository()
        val repository = StripeApiRepository(
            context = ApplicationProvider.getApplicationContext<Context>(),
            publishableKeyProvider = { "pk_test_merchant" },
            requestSurface = StripeRepository.DEFAULT_REQUEST_SURFACE,
            workContext = StandardTestDispatcher(testScheduler),
            stripeNetworkClient = networkClient,
            analyticsRequestExecutor = executor,
            fraudDetectionDataRepository = fraudRepository,
        )
        fraudRepository.refreshCalls.awaitItem()

        val result = repository.updatePaymentMethod(
            paymentMethodId = "pm_test",
            paymentMethodUpdateParams = PaymentMethodUpdateParams.createCard(expiryMonth = 12, expiryYear = 2045),
            options = ApiRequest.Options("ek_test_customer"),
        )

        assertThat(result.isSuccess).isTrue()
        assertThat((networkClient.requests.awaitItem() as ApiRequest).options.apiKey).isEqualTo("ek_test_customer")
        assertThat(executor.requests.awaitItem().params).containsEntry("publishable_key", "pk_test_merchant")
        fraudRepository.refreshCalls.awaitItem()
        networkClient.requests.ensureAllEventsConsumed()
        executor.requests.ensureAllEventsConsumed()
        fraudRepository.refreshCalls.ensureAllEventsConsumed()
    }

    internal class FakeAnalyticsRequestExecutor : AnalyticsRequestExecutor {
        val requests = Turbine<AnalyticsRequest>()

        override fun executeAsync(request: AnalyticsRequest) {
            requests.add(request)
        }
    }

    internal class FakeStripeNetworkClient : StripeNetworkClient {
        val requests = Turbine<StripeRequest>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            requests.add(request)
            return StripeResponse(200, """{"id":"pm_test","type":"card"}""")
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("Unexpected file request")
        }
    }

    internal class FakeFraudDetectionDataRepository : FraudDetectionDataRepository {
        val refreshCalls = Turbine<Unit>()

        override fun refresh() {
            refreshCalls.add(Unit)
        }

        override fun getCached(): FraudDetectionData? = error("Unexpected cached data request")

        override suspend fun getLatest(): FraudDetectionData? = error("Unexpected latest data request")

        override fun save(fraudDetectionData: FraudDetectionData) = error("Unexpected save")
    }
}
