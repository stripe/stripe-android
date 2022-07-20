package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.InstitutionPlannedException
import com.stripe.android.financialconnections.exception.InstitutionUnplannedException
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date

internal class PostAuthorizationSessionTest {

    private val repository = FakeFinancialConnectionsRepository()
    private val postAuthorizationSession = PostAuthorizationSession(
        repository = repository,
        configuration = FinancialConnectionsSheet.Configuration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY
        )
    )
    private val selectedInstitution = Institution(
        id = "id",
        name = "name",
        url = "url",
        featured = true,
        featuredOrder = null
    )

    @Test
    fun `invoke - institution unplanned downtime maps to exception`() = runTest {
        repository.postAuthorizationSessionProvider = {
            throw APIException(
                stripeError = StripeError(
                    extraFields = mapOf(
                        "institution_unavailable" to "true"
                    )
                ),
                statusCode = 405,
                message = "auth error"
            )
        }

        val result: Throwable = kotlin.runCatching { postAuthorizationSession(selectedInstitution) }
            .exceptionOrNull()!!

        assertThat(result).isInstanceOf(InstitutionUnplannedException::class.java)
        val exception = result as InstitutionUnplannedException
        assertThat(exception.institution).isEqualTo(selectedInstitution)
    }

    @Test
    fun `invoke - institution planned downtime maps to exception`() = runTest {
        val upTime = Date().time
        repository.postAuthorizationSessionProvider = {
            throw APIException(
                stripeError = StripeError(
                    extraFields = mapOf(
                        "institution_unavailable" to "true",
                        "expected_to_be_available_at" to upTime.toString()
                    )
                ),
                statusCode = 405,
                message = "auth error"
            )
        }

        val result: Throwable = kotlin.runCatching { postAuthorizationSession(selectedInstitution) }
            .exceptionOrNull()!!

        assertThat(result).isInstanceOf(InstitutionPlannedException::class.java)
        val exception = result as InstitutionPlannedException
        assertThat(exception.institution).isEqualTo(selectedInstitution)
        assertThat(exception.backUpAt).isEqualTo(upTime * 1000)
    }

    @Test
    fun `invoke - unhandled exception does not map`() = runTest {
        val unhandledException = APIException(
            stripeError = StripeError(
                extraFields = mapOf()
            ),
            statusCode = 405,
            message = "auth error"
        )
        repository.postAuthorizationSessionProvider = { throw unhandledException }

        val result: Throwable = kotlin.runCatching { postAuthorizationSession(selectedInstitution) }
            .exceptionOrNull()!!

        assertThat(result).isEqualTo(unhandledException)
    }
}
