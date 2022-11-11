package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse

internal class InstitutionPickerStates :
    PreviewParameterProvider<InstitutionPickerState> {
    override val values = sequenceOf(
        searchModeSearchingInstitutions(),
        searchModeWithResults(),
        searchModeNoResults(),
        searchModeNoQuery(),
        noSearchMode()
    )

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun initialLoading() = InstitutionPickerState(
            payload = Loading(),
            searchInstitutions = Uninitialized,
            searchMode = false,
            allowManualEntry = true,
        )
        // Search mode - searching institutions
        fun searchModeSearchingInstitutions() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Loading(),
            searchMode = true,
        )

        // Search mode: with results
        fun searchModeWithResults() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = true,
        )

        // Search mode: No results
        fun searchModeNoResults() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(InstitutionResponse(emptyList())),
            searchMode = false,
        )

        // Search mode: failed
        fun searchModeFailed() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
            searchMode = true,
        )

        // Search mode: no query
        fun searchModeNoQuery() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = true,
        )

        // No search mode
        fun noSearchMode() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = false,
        )

        private fun payload() = InstitutionPickerState.Payload(
            featuredInstitutions = institutionResponse().data,
            allowManualEntry = true,
            searchDisabled = false
        )
        private fun institutionResponse() = InstitutionResponse(
            listOf(
                FinancialConnectionsInstitution(
                    id = "1",
                    name = "Very very long institution 1",
                    url = "institution 1 url",
                    featured = false,
                    featuredOrder = null,
                    icon = null,
                    logo = null,
                    mobileHandoffCapable = false
                ),
                FinancialConnectionsInstitution(
                    id = "2",
                    name = "Institution 2",
                    url = "Institution 2 url",
                    featured = false,
                    featuredOrder = null,
                    icon = null,
                    logo = null,
                    mobileHandoffCapable = false
                ),
                FinancialConnectionsInstitution(
                    id = "3",
                    name = "Institution 3",
                    url = "Institution 3 url",
                    featured = false,
                    featuredOrder = null,
                    icon = null,
                    logo = null,
                    mobileHandoffCapable = false
                )
            )
        )
    }
}
