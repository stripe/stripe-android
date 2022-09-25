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
        clientSecret: String,
        testMode: Boolean,
    ): InstitutionResponse {
        val limit = when (testMode) {
            true -> SEARCH_INSTITUTIONS_LIMIT_TEST_MODE
            false -> SEARCH_INSTITUTIONS_LIMIT
        }
        return repository.featuredInstitutions(
            clientSecret = clientSecret,
            limit = limit
        )
    }

    private companion object {
        private const val SEARCH_INSTITUTIONS_LIMIT = 8
        private const val SEARCH_INSTITUTIONS_LIMIT_TEST_MODE = 10
    }
}
