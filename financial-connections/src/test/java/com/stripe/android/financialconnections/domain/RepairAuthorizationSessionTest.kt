package com.stripe.android.financialconnections.domain

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.ThemeMode
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.networking.AbsFinancialConnectionsManifestRepository
import com.stripe.android.financialconnections.repository.CachedConsumerSession
import com.stripe.android.financialconnections.repository.ConsumerSessionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

internal class RepairAuthorizationSessionTest {

    @Test
    fun `repair reads the current consumer secret at invocation time`() = runScenario {
        consumerSession = consumerSession?.copy(clientSecret = "current_consumer_secret")

        val result = repairAuthorizationSession("fcauth_123")

        assertThat(result).isEqualTo(ApiKeyFixtures.authorizationSession())
        assertThat(repository.repairCalls.awaitItem()).isEqualTo(
            RepairCall("fc_secret", "fcauth_123", "com.app.id", "current_consumer_secret")
        )
    }

    @Test
    fun `repair preserves same-holder arguments after the consumer session is cleared`() = runScenario {
        consumerSession = null

        val result = repairAuthorizationSession("fcauth_123")

        assertThat(result).isEqualTo(ApiKeyFixtures.authorizationSession())
        assertThat(repository.repairCalls.awaitItem()).isEqualTo(
            RepairCall("fc_secret", "fcauth_123", "com.app.id", null)
        )
    }

    @Test
    fun `repair propagates an expired credential error without a credentialless retry`() = runScenario {
        val error = InvalidRequestException(
            stripeError = StripeError(message = "Consumer session expired"),
            statusCode = 400,
        )
        repository.result = Result.failure(error)

        val thrown = assertFailsWith<InvalidRequestException> {
            repairAuthorizationSession("fcauth_123")
        }

        assertThat(thrown).isSameInstanceAs(error)
        assertThat(repository.repairCalls.awaitItem()).isEqualTo(
            RepairCall("fc_secret", "fcauth_123", "com.app.id", "cached_consumer_secret")
        )
        repository.repairCalls.expectNoEvents()
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        val scenario = Scenario(FakeRepairRepository())
        scenario.block()
        scenario.repository.repairCalls.ensureAllEventsConsumed()
    }

    private class Scenario(val repository: FakeRepairRepository) {
        var consumerSession: CachedConsumerSession? = CachedConsumerSession(
            emailAddress = "test@example.com",
            phoneNumber = "+1********12",
            clientSecret = "cached_consumer_secret",
            publishableKey = null,
            isVerified = true,
            linkBrand = null,
        )
        val repairAuthorizationSession = RepairAuthorizationSession(
            repository = repository,
            configuration = FinancialConnectionsSheetConfiguration(
                financialConnectionsSessionClientSecret = "fc_secret",
                apiConfiguration = ApiKeyFixtures.DEFAULT_API_CONFIG,
                preCollectedConsent = null,
                themeMode = ThemeMode.Automatic,
            ),
            applicationId = "com.app.id",
            consumerSessionProvider = ConsumerSessionProvider { consumerSession },
        )
    }

    private class FakeRepairRepository : AbsFinancialConnectionsManifestRepository() {
        val repairCalls = Turbine<RepairCall>()
        var result: Result<FinancialConnectionsAuthorizationSession> =
            Result.success(ApiKeyFixtures.authorizationSession())

        override suspend fun repairAuthorizationSession(
            clientSecret: String,
            coreAuthorization: String,
            applicationId: String,
            consumerSessionClientSecret: String?,
        ): FinancialConnectionsAuthorizationSession {
            repairCalls.add(RepairCall(clientSecret, coreAuthorization, applicationId, consumerSessionClientSecret))
            return result.getOrThrow()
        }
    }

    private data class RepairCall(
        val clientSecret: String,
        val coreAuthorization: String,
        val applicationId: String,
        val consumerSessionClientSecret: String?,
    )
}
