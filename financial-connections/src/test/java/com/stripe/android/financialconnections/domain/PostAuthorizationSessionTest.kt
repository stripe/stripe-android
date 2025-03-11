package com.stripe.android.financialconnections.domain

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.StripeError
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.ApiKeyFixtures
import com.stripe.android.financialconnections.ApiKeyFixtures.sessionManifest
import com.stripe.android.financialconnections.ApiKeyFixtures.syncResponse
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.networking.FakeFinancialConnectionsManifestRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
internal class PostAuthorizationSessionTest {

    private val repository = FakeFinancialConnectionsManifestRepository()
    private val postAuthorizationSession = PostAuthorizationSession(
        repository = repository,
        configuration = FinancialConnectionsSheetConfiguration(
            ApiKeyFixtures.DEFAULT_FINANCIAL_CONNECTIONS_SESSION_SECRET,
            ApiKeyFixtures.DEFAULT_PUBLISHABLE_KEY,
        ),
        APPLICATION_ID
    )
    private val selectedInstitution = FinancialConnectionsInstitution(
        id = "id",
        name = "name",
        url = "url",
        featured = true,
        featuredOrder = null,
        mobileHandoffCapable = false
    )

    @Test
    fun `invoke - institution unplanned downtime maps to exception`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(allowManualEntry = true)
        )
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

        val result: Throwable = kotlin.runCatching {
            postAuthorizationSession(
                institution = selectedInstitution,
                sync = sync
            )
        }
            .exceptionOrNull()!!

        assertThat(result).isInstanceOf(InstitutionUnplannedDowntimeError::class.java)
        val exception = result as InstitutionUnplannedDowntimeError
        assertThat(exception.institution).isEqualTo(selectedInstitution)
    }

    @Test
    fun `invoke - institution planned downtime maps to exception`() = runTest {
        val upTime = Date().time
        val sync = syncResponse(
            sessionManifest().copy(allowManualEntry = true)
        )
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

        val result: Throwable = kotlin.runCatching {
            postAuthorizationSession(
                institution = selectedInstitution,
                sync = sync
            )
        }
            .exceptionOrNull()!!

        assertThat(result).isInstanceOf(InstitutionPlannedDowntimeError::class.java)
        val exception = result as InstitutionPlannedDowntimeError
        assertThat(exception.institution).isEqualTo(selectedInstitution)
        assertThat(exception.backUpAt).isEqualTo(upTime * 1000)
    }

    @Test
    fun `invoke - unhandled exception does not map`() = runTest {
        val sync = syncResponse(
            sessionManifest().copy(allowManualEntry = true)
        )
        val unhandledException = APIException(
            stripeError = StripeError(
                extraFields = mapOf()
            ),
            statusCode = 405,
            message = "auth error"
        )
        repository.postAuthorizationSessionProvider = { throw unhandledException }

        val result: Throwable = kotlin.runCatching {
            postAuthorizationSession(
                institution = selectedInstitution,
                sync = sync
            )
        }
            .exceptionOrNull()!!

        assertThat(result).isEqualTo(unhandledException)
    }
}
