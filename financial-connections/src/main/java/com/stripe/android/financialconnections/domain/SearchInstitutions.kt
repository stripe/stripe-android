package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.repository.FinancialConnectionsRepository
import javax.inject.Inject

internal class SearchInstitutions @Inject constructor(
    private val repository: FinancialConnectionsRepository
) {
    suspend operator fun invoke(
        clientSecret: String,
        query: String? = null
    ): InstitutionResponse {
        return if (query.isNullOrBlank()) {
            repository.featuredInstitutions(clientSecret)
        } else {
            repository.searchInstitutions(clientSecret = clientSecret, query = query)
        }
    }
}
