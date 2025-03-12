package com.stripe.android.financialconnections.domain

import com.stripe.android.financialconnections.FinancialConnectionsSheet
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitutionSelected
import com.stripe.android.financialconnections.repository.FinancialConnectionsManifestRepository
import javax.inject.Inject

internal class SelectInstitution @Inject constructor(
    private val repository: FinancialConnectionsManifestRepository,
    private val configuration: FinancialConnectionsSheet.Configuration,
) {

    suspend operator fun invoke(
        institution: FinancialConnectionsInstitution,
    ): FinancialConnectionsInstitutionSelected {
        return repository.selectInstitution(
            clientSecret = configuration.financialConnectionsSessionClientSecret,
            institution = institution,
        )
    }
}
