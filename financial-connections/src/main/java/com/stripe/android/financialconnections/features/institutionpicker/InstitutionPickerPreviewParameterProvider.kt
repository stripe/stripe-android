package com.stripe.android.financialconnections.features.institutionpicker

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.model.InstitutionResponse
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized

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
            searchInstitutions = Success(institutionResponse(FEW_INSTITUTIONS).copy(showManualEntry = true)),
        ),
        initialScroll = 0
    )

    private fun searchSuccessNoManualEntry() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse(FEW_INSTITUTIONS).copy(showManualEntry = false)),
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
            searchInstitutions = Fail(Exception("Something went wrong")),
        ),
        initialScroll = 0
    )

    private fun searchFailedNoManualEntry() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload(manualEntry = false)),
            searchInstitutions = Fail(Exception("Something went wrong")),
        ),
        initialScroll = 0
    )

    private fun selectedInstitution() = InstitutionPreviewState(
        state = InstitutionPickerState(
            previewText = "Some query",
            payload = Success(payload()),
            searchInstitutions = Success(institutionResponse(FEW_INSTITUTIONS)),
            selectedInstitutionId = "2",
            createSessionForInstitution = Loading(),
        ),
        initialScroll = 0
    )

    private fun partiallyScrolled(): InstitutionPreviewState {
        return InstitutionPreviewState(
            state = InstitutionPickerState(
                previewText = "Some query",
                payload = Success(payload()),
                searchInstitutions = Success(institutionResponse(MANY_INSTITUTIONS)),
            ),
            initialScroll = 1000
        )
    }

    private fun payload(manualEntry: Boolean = true) = InstitutionPickerState.Payload(
        featuredInstitutions = institutionResponse(institutions = FEW_INSTITUTIONS).copy(showManualEntry = manualEntry),
        searchDisabled = false,
        featuredInstitutionsDuration = 0
    )

    @Suppress("MagicNumber")
    private fun institutionResponse(institutions: Int) = InstitutionResponse(
        showManualEntry = true,
        listOf(
            institution(1).copy(
                name = "Very very long institution content does not fit - 1",
                url = "https://www.institutionUrl.com/1",
            ),
            institution(2),
            institution(3).copy(
                url = "Unparseable URL"
            ),
            institution(4),
            institution(5),
            institution(6),
            institution(7),
            institution(8),
            institution(9),
            institution(10)
        ).take(institutions)
    )

    private fun institution(i: Int): FinancialConnectionsInstitution {
        return FinancialConnectionsInstitution(
            id = i.toString(),
            name = "Institution $i",
            url = "otherUrl.com",
            featured = false,
            featuredOrder = null,
            icon = null,
            logo = null,
            mobileHandoffCapable = false
        )
    }

    data class InstitutionPreviewState(
        val state: InstitutionPickerState,
        val initialScroll: Int
    )

    companion object {
        const val FEW_INSTITUTIONS = 3
        const val MANY_INSTITUTIONS = 10
    }
}
