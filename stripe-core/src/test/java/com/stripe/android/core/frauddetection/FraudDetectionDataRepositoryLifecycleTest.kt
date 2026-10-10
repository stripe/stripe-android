package com.stripe.android.core.frauddetection

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiKeyFixtures
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.networking.StripeNetworkClient
import com.stripe.android.core.networking.StripeRequest
import com.stripe.android.core.networking.StripeResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class FraudDetectionDataRepositoryLifecycleTest {
    @Test
    fun `construction does not start collection`() = runScenario {
        runCurrent()
        networkClient.requests.expectNoEvents()
        reportedErrors.expectNoEvents()
        assertThat(repository.getCached()).isNull()
    }

    @Test
    fun `in flight failure retains its initiating key after another refresh`() = runScenario {
        repository.refresh(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        runCurrent()
        val firstResponse = networkClient.requests.awaitItem()

        repository.refresh(ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        runCurrent()
        val secondResponse = networkClient.requests.awaitItem()

        val error = APIConnectionException("Failed to collect fraud data")
        firstResponse.completeExceptionally(error)
        runCurrent()
        assertThat(reportedErrors.awaitItem()).isEqualTo(error to ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)

        secondResponse.complete(successResponse())
        runCurrent()
        reportedErrors.expectNoEvents()
        assertThat(repository.getCached()?.guid).isEqualTo("guid")
    }

    @Test
    fun `cached reads see collection completed by another repository`() = runScenario {
        assertThat(secondRepository.getCached()).isNull()

        repository.refresh(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        runCurrent()
        networkClient.requests.awaitItem().complete(successResponse())
        runCurrent()

        assertThat(secondRepository.getCached()?.guid).isEqualTo("guid")
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `cached reads see later updates from another repository`() = runScenario {
        repository.save(FraudDetectionData("first", "muid", "sid"))
        assertThat(secondRepository.getCached()?.guid).isEqualTo("first")

        repository.save(FraudDetectionData("second", "muid", "sid"))

        assertThat(secondRepository.getCached()?.guid).isEqualTo("second")
        networkClient.requests.expectNoEvents()
    }

    @Test
    fun `cached reads do not wait for an in flight collection`() = runScenario {
        repository.refresh(ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY)
        runCurrent()
        val response = networkClient.requests.awaitItem()

        assertThat(repository.getCached()).isNull()
        networkClient.requests.expectNoEvents()

        response.complete(successResponse())
        runCurrent()
    }

    @Test
    fun `disabled collection hides persisted data`() = runScenario(fraudDetectionEnabled = false) {
        repository.save(FraudDetectionData("guid", "muid", "sid"))

        assertThat(secondRepository.getCached()).isNull()
        networkClient.requests.expectNoEvents()
    }

    private fun runScenario(
        fraudDetectionEnabled: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val networkClient = FakeFraudNetworkClient()
        val reportedErrors = Turbine<Pair<StripeException, String>>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        fun createRepository() = DefaultFraudDetectionDataRepository(
            localStore = DefaultFraudDetectionDataStore(context, dispatcher),
            fraudDetectionDataRequestFactory = DefaultFraudDetectionDataRequestFactory(context),
            stripeNetworkClient = networkClient,
            errorReporter = { error, key -> reportedErrors.add(error to key) },
            workContext = dispatcher,
            fraudDetectionEnabledProvider = { fraudDetectionEnabled },
        )
        Scenario(this, createRepository(), createRepository(), networkClient, reportedErrors).block()
        networkClient.requests.ensureAllEventsConsumed()
        reportedErrors.ensureAllEventsConsumed()
    }

    private class Scenario(
        val scope: TestScope,
        val repository: DefaultFraudDetectionDataRepository,
        val secondRepository: DefaultFraudDetectionDataRepository,
        val networkClient: FakeFraudNetworkClient,
        val reportedErrors: Turbine<Pair<StripeException, String>>,
    ) {
        fun runCurrent() = scope.runCurrent()
    }

    private class FakeFraudNetworkClient : StripeNetworkClient {
        val requests = Turbine<CompletableDeferred<StripeResponse<String>>>()

        override suspend fun executeRequest(request: StripeRequest): StripeResponse<String> {
            val response = CompletableDeferred<StripeResponse<String>>()
            requests.add(response)
            return response.await()
        }

        override suspend fun executeRequestForFile(request: StripeRequest, outputFile: File): StripeResponse<File> {
            error("File requests are not expected")
        }
    }

    private fun successResponse() = StripeResponse(
        code = 200,
        body = """{"guid":"guid","muid":"muid","sid":"sid"}""",
    )
}
