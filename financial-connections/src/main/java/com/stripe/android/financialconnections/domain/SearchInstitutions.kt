package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsInstitutionsRepository
import javax.inject.Inject

/**
 * Searches for institutions based on a given query.
 *
 */
internal class SearchInstitutions @Inject constructor(
    private val repository: FinancialConnectionsInstitutionsRepository
) {
    suspend operator fun invoke(
        clientSecret: String,
        query: String
    ): InstitutionResponse {
        return repository.searchInstitutions(
            clientSecret = clientSecret,
            query = query,
            limit = SEARCH_INSTITUTIONS_LIMIT
        )
    }

    private companion object {
        private const val SEARCH_INSTITUTIONS_LIMIT = 10
    }
}
