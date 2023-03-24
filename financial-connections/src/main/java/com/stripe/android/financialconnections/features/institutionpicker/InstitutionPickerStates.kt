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
        noSearchMode()
    )

    // TODO@carlosmuvi migrate to PreviewParameterProvider when showkase adds support.
    companion object {
        fun initialLoading() = InstitutionPickerState(
            payload = Loading(),
            searchInstitutions = Uninitialized,
            searchMode = false,
        )

        fun searchModeSearchingInstitutions() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Loading(),
            searchMode = true,
        )

        fun searchModeWithResults() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse().copy(showManualEntry = true)),
            searchMode = true,
        )

        fun searchModeWithResultsNoManualEntry() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse().copy(showManualEntry = false)),
            searchMode = true,
        )

        // Search mode: No results
        fun searchModeNoResults() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = true
                )
            ),
            searchMode = true,
        )

        fun searchModeNoResultsNoManualEntry() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = false
                )
            ),
            searchMode = true,
        )

        fun searchModeFailed() = InstitutionPickerState(
            payload = Success(payload().copy(allowManualEntry = true)),
            searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
            searchMode = true,
        )

        fun searchModeFailedNoManualEntry() = InstitutionPickerState(
            payload = Success(payload().copy(allowManualEntry = false)),
            searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
            searchMode = true,
        )

        fun noSearchMode() = InstitutionPickerState(
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse()),
            searchMode = false,
        )

        private fun payload() = InstitutionPickerState.Payload(
            featuredInstitutions = institutionResponse().data,
            searchDisabled = false,
            allowManualEntry = false
        )

        private fun institutionResponse() = InstitutionResponse(
            showManualEntry = true,
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
