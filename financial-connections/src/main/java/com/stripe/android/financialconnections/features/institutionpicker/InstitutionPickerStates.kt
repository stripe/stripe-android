package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.Institution
import com.stripe.android.financialconnections.model.InstitutionResponse

internal class InstitutionPickerStates :
    PreviewParameterProvider<InstitutionPickerState> {
    override val values = sequenceOf(
        searchModeSearchingInstitutions(),
        searchModeWithResults(),
        searchModeSelectingInstitutions(),
        searchModeNoResults(),
        searchModeNoQuery(),
        noSearchMode(),
    )

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        // Search mode - searching institutions
        fun searchModeSearchingInstitutions() = InstitutionPickerState(
            selectInstitution = Uninitialized,
            featuredInstitutions = Success(institutionResponse()),
            searchInstitutions = Loading(),
            searchMode = true,
            query = "query",
        )

        // Search mode: with results
        fun searchModeWithResults() = InstitutionPickerState(
            selectInstitution = Uninitialized,
            featuredInstitutions = Success(institutionResponse()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = true,
            query = "query",
        )

        // Search mode: selecting institution
        fun searchModeSelectingInstitutions() = InstitutionPickerState(
            selectInstitution = Loading(),
            featuredInstitutions = Success(institutionResponse()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = true,
            query = "query",
        )

        // Search mode: No results
        fun searchModeNoResults() = InstitutionPickerState(
            selectInstitution = Uninitialized,
            featuredInstitutions = Success(institutionResponse()),
            searchInstitutions = Success(InstitutionResponse(emptyList())),
            searchMode = false,
            query = "query",
        )

        // Search mode: no query
        fun searchModeNoQuery() = InstitutionPickerState(
            selectInstitution = Uninitialized,
            featuredInstitutions = Success(institutionResponse()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = true,
            query = "",
        )

        // No search mode
        fun noSearchMode() = InstitutionPickerState(
            selectInstitution = Uninitialized,
            featuredInstitutions = Success(institutionResponse()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = false,
            query = "",
        )

        private fun institutionResponse() = InstitutionResponse(
            listOf(
                Institution(
                    id = "1",
                    name = "Very very long institution 1",
                    url = "institution 1 url",
                    featured = false,
                    featuredOrder = null
                ),
                Institution(
                    id = "2",
                    name = "Institution 2",
                    url = "Institution 2 url",
                    featured = false,
                    featuredOrder = null
                ),
                Institution(
                    id = "3",
                    name = "Institution 3",
                    url = "Institution 3 url",
                    featured = false,
                    featuredOrder = null
                )
            )
        )
    }
}
