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
        featured(),
        searchInProgress(),
        searchSuccess(),
        searchSuccessNoManualEntry(),
        searchNoResults(),
        searchNoResultsNoManualEntry(),
        searchFailed(),
        searchFailedNoManualEntry(),
        selectedInstitution()
    )

    private fun initialLoading() = InstitutionPickerState(
        previewText = null,
        payload = Loading(),
        searchInstitutions = Uninitialized,
    )

    private fun featured() = InstitutionPickerState(
        previewText = null,
        payload = Success(payload()),
        searchInstitutions = Uninitialized,
    )

    private fun searchInProgress() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Loading(),
    )

    private fun searchSuccess() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse().copy(showManualEntry = true)),
    )

    private fun searchSuccessNoManualEntry() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse().copy(showManualEntry = false)),
    )

    private fun searchNoResults() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(
            InstitutionResponse(
                data = emptyList(),
                showManualEntry = true
            )
        ),
    )

    private fun searchNoResultsNoManualEntry() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(
            InstitutionResponse(
                data = emptyList(),
                showManualEntry = false
            )
        ),
    )

    private fun searchFailed() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload(manualEntry = true)),
        searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
    )

    private fun searchFailedNoManualEntry() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload(manualEntry = false)),
        searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
    )

    private fun selectedInstitution() = InstitutionPickerState(
        previewText = "Some query",
        payload = Success(payload()),
        searchInstitutions = Success(institutionResponse()),
        selectedInstitutionId = "2",
        selectInstitution = Loading(),
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
