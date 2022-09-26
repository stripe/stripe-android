package com.stripe.android.financialconnections.domain

import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.exception.InstitutionPlannedException
import com.stripe.android.financialconnections.exception.InstitutionUnplannedException
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.FinancialConnectionsAuthorizationSession
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * Use case called on institution selection. It notifies backend about selection to initiate
 * an authorization session.
 *
 */
internal class PostAuthorizationSession @Inject constructor(
    val repository: FinancialConnectionsManifestRepository,
    val configuration: FinancialConnectionsSheet.Configuration
) {

    /**
     * @param institution selected institution to create a [FinancialConnectionsAuthorizationSession]
     * @param allowManualEntry to build
     */
    suspend operator fun invoke(
        institution: FinancialConnectionsInstitution,
        allowManualEntry: Boolean
    ): FinancialConnectionsAuthorizationSession {
        return try {
            repository.postAuthorizationSession(
                configuration.financialConnectionsSessionClientSecret,
                institution = institution
            )
        } catch (
            @Suppress("SwallowedException") e: StripeException
        ) {
            throw e.toDomainException(allowManualEntry, institution)
        }
    }

    private fun StripeException.toDomainException(
        allowManualEntry: Boolean,
        institution: FinancialConnectionsInstitution
    ): StripeException = this.stripeError?.let {
        val institutionUnavailable: String? = it.extraFields?.get("institution_unavailable")
        val availableAt: String? = it.extraFields?.get("expected_to_be_available_at")
        when (institutionUnavailable) {
            "true" -> when {
                availableAt.isNullOrEmpty() -> InstitutionUnplannedException(
                    institution = institution,
                    allowManualEntry = allowManualEntry,
                    stripeException = this
                )
                else -> InstitutionPlannedException(
                    institution = institution,
                    allowManualEntry = allowManualEntry,
                    isToday = true,
                    backUpAt = availableAt.toLong().seconds.inWholeMilliseconds,
                    stripeException = this
                )
            }
            else -> this
        }
    } ?: this
}
