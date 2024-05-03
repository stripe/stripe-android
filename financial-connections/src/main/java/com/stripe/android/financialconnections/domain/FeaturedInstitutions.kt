package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
import javax.inject.Inject

/**
 * Fetches featured institutions.
 *
 */
internal class FeaturedInstitutions @Inject constructor(
    private val repository: FinancialConnectionsInstitutionsRepository
) {
    suspend operator fun invoke(
        clientSecret: String
    ): InstitutionResponse {
        return repository.featuredInstitutions(
            clientSecret = clientSecret,
        )
    }
}
