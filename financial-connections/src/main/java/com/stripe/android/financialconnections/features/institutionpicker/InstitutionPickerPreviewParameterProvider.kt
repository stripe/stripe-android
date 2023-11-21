package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse

internal class InstitutionPickerPreviewParameterProvider :
    PreviewParameterProvider<InstitutionPickerState> {
    override val values = sequenceOf(
        initialLoading(),
        searchModeSearchingInstitutions(),
        searchModeWithResults(),
        searchModeWithResultsNoManualEntry(),
        searchModeNoResults(),
        searchModeNoResultsNoManualEntry(),
        searchModeFailed(),
        searchModeFailedNoManualEntry(),
        noSearchMode(),
        noSearchModeSelectedInstitution()
    )

    private fun initialLoading() = InstitutionPickerState(
        previewText = null,
        payload = Loading(),
        searchInstitutions = Uninitialized,
        searchMode = false,
    )

    private fun searchModeSearchingInstitutions() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Loading(),
        searchMode = true,
    )

    private fun searchModeWithResults() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse().copy(showManualEntry = true)),
        searchMode = true,
    )

    private fun searchModeWithResultsNoManualEntry() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse().copy(showManualEntry = false)),
        searchMode = true,
    )

    private fun searchModeNoResults() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(
            InstitutionResponse(
                data = emptyList(),
                showManualEntry = true
            )
        ),
        searchMode = true,
    )

    private fun searchModeNoResultsNoManualEntry() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(
            InstitutionResponse(
                data = emptyList(),
                showManualEntry = false
            )
        ),
        searchMode = true,
    )

    private fun searchModeFailed() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload(manualEntry = true)),
        searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
        searchMode = true,
    )

    private fun searchModeFailedNoManualEntry() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload(manualEntry = false)),
        searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
        searchMode = true,
    )

    private fun noSearchMode() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse()),
        searchMode = false,
    )

    private fun noSearchModeSelectedInstitution() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse()),
        selectedInstitutionId = "2",
        selectInstitution = Loading(),
        searchMode = false,
    )

    private fun payload(manualEntry: Boolean = true) = InstitutionPickerState.Payload(
        featuredInstitutions = institutionResponse().copy(showManualEntry = manualEntry),
        searchDisabled = false,
        featuredInstitutionsDuration = 0
    )

    private fun institutionResponse() = InstitutionResponse(
        showManualEntry = true,
        listOf(
            FinancialConnectionsInstitution(
                id = "1",
                name = "Very very long institution content does not fit - 1",
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
