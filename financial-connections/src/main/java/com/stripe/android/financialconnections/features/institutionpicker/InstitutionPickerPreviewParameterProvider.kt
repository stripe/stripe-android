package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse

internal class InstitutionPickerPreviewParameterProvider :
    PreviewParameterProvider<InstitutionPickerPreviewParameterProvider.InstitutionPreviewState> {
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
        selectedInstitution(),
        partiallyScrolled()
    )

    private fun initialLoading() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = null,
            payload = Loading(),
            searchInstitutions = Uninitialized,
        ),
        initialScroll = 0
    )

    private fun featured() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = null,
            payload = Success(payload()),
            searchInstitutions = Uninitialized,
        ),
        initialScroll = 0
    )

    private fun searchInProgress() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Loading(),
        ),
        initialScroll = 0
    )

    private fun searchSuccess() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse(3).copy(showManualEntry = true)),
        ),
        initialScroll = 0
    )

    private fun searchSuccessNoManualEntry() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse(3).copy(showManualEntry = false)),
        ),
        initialScroll = 0
    )

    private fun searchNoResults() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = true
                )
            ),
        ),
        initialScroll = 0
    )

    private fun searchNoResultsNoManualEntry() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(
                InstitutionResponse(
                    data = emptyList(),
                    showManualEntry = false
                )
            ),
        ),
        initialScroll = 0
    )

    private fun searchFailed() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload(manualEntry = true)),
            searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
        ),
        initialScroll = 0
    )

    private fun searchFailedNoManualEntry() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload(manualEntry = false)),
            searchInstitutions = Fail(java.lang.Exception("Something went wrong")),
        ),
        initialScroll = 0
    )

    private fun selectedInstitution() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse(3)),
            selectedInstitutionId = "2",
            createSessionForInstitution = Loading(),
        ),
        initialScroll = 0
    )

    private fun partiallyScrolled() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse(10)),
        ),
        initialScroll = 1000
    )

    private fun payload(manualEntry: Boolean = true) = InstitutionPickerState.Payload(
        featuredInstitutions = institutionResponse(institutions = 3).copy(showManualEntry = manualEntry),
        searchDisabled = false,
        featuredInstitutionsDuration = 0
    )

    private fun institutionResponse(institutions: Int) = InstitutionResponse(
        showManualEntry = true,
        listOf(
            FinancialConnectionsInstitution(
                id = "1",
                name = "Very very long institution content does not fit - 1",
                url = "https://www.institutionUrl.com/1",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "2",
                name = "Institution 2",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "3",
                name = "Institution 3",
                url = "Unparseable URL",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "4",
                name = "Institution 4",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "5",
                name = "Institution 5",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "6",
                name = "Institution 6",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "7",
                name = "Institution 7",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "8",
                name = "Institution 8",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "9",
                name = "Institution 9",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            ),
            FinancialConnectionsInstitution(
                id = "10",
                name = "Institution 10",
                url = "otherUrl.com",
                featured = false,
                featuredOrder = null,
                icon = null,
                logo = null,
                mobileHandoffCapable = false
            )
        ).take(institutions)
    )

    data class InstitutionPreviewState(
        val state: InstitutionPickerState,
        val initialScroll: Int
    )
}
