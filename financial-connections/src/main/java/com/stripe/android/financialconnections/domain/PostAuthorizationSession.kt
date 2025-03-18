package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheetConfiguration
import com.stripe.android.financialconnections.di.APPLICATION_ID
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.features.common.showManualEntryInErrors
import com.stripe.android.financialconnections.model.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.SynchronizeSessionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import javax.inject.Named
import kotlin.time.Duration.Companion.seconds

/**
 * Use case called on institution selection. It notifies backend about selection to initiate
 * an authorization session.
 *
 */
internal class PostAuthorizationSession @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheetConfiguration,
    @Named(APPLICATION_ID) private val applicationId: String
) {

    /**
     * @param institution selected institution to create a [FinancialConnectionsAuthorizationSession]
     * @param sync [SynchronizeSessionResponse]
     */
    suspend operator fun invoke(
        institution: FinancialConnectionsInstitution,
        sync: SynchronizeSessionResponse,
    ): FinancialConnectionsAuthorizationSession {
        return try {
            repository.postAuthorizationSession(
                configuration.financialConnectionsSessionClientSecret,
                institution = institution,
                applicationId = applicationId
            )
        } catch (e: StripeException) {
            throw e.toDomainException(
                showManualEntry = sync.showManualEntryInErrors(),
                institution = institution
            )
        }
    }

    private fun StripeException.toDomainException(
        showManualEntry: Boolean,
        institution: FinancialConnectionsInstitution
    ): StripeException = this.stripeError?.let {
        val institutionUnavailable: String? = it.extraFields?.get("institution_unavailable")
        val availableAt: String? = it.extraFields?.get("expected_to_be_available_at")
        when (institutionUnavailable) {
            "true" -> when {
                availableAt.isNullOrEmpty() -> InstitutionUnplannedDowntimeError(
                    institution = institution,
                    showManualEntry = showManualEntry,
                    stripeException = this
                )

                else -> InstitutionPlannedDowntimeError(
                    institution = institution,
                    showManualEntry = showManualEntry,
                    isToday = true,
                    backUpAt = availableAt.toLong().seconds.inWholeMilliseconds,
                    stripeException = this
                )
            }

            else -> this
        }
    } ?: this
}
