package com.stripe.android.financialconnections.features.error

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.exception.InstitutionPlannedDowntimeError
import com.stripe.android.financialconnections.exception.InstitutionUnplannedDowntimeError
import com.stripe.android.financialconnections.model.FinancialConnectionsInstitution
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success

internal class ErrorPreviewParameterProvider :
    PreviewParameterProvider<ErrorState> {
    override val values = sequenceOf(
        loading(),
        unclassified(),
        unclassifiedWithManualEntry(),
        expectedDowntime(),
        unexpectedDowntime()
    )

    private fun loading() = ErrorState(
        payload = Loading(),
    )

    private fun unclassified() = ErrorState(
        payload = Success(
            ErrorState.Payload(
                error = IllegalArgumentException("An unknown error occurred."),
                allowManualEntry = false,
                disableLinkMoreAccounts = true,
            )
        ),
    )

    private fun unclassifiedWithManualEntry() = ErrorState(
        payload = Success(
            ErrorState.Payload(
                error = IllegalArgumentException("An unknown error occurred."),
                allowManualEntry = true,
                disableLinkMoreAccounts = true,
            )
        ),
    )

    private fun expectedDowntime() = ErrorState(
        payload = Success(
            ErrorState.Payload(
                error = InstitutionPlannedDowntimeError(
                    institution = institution(),
                    showManualEntry = true,
                    isToday = true,
                    backUpAt = 10000L,
                    stripeException = APIException()
                ),
                allowManualEntry = true,
                disableLinkMoreAccounts = true,
            )
        ),
    )

    private fun unexpectedDowntime() = ErrorState(
        payload = Success(
            ErrorState.Payload(
                error = InstitutionUnplannedDowntimeError(
                    institution = institution(),
                    showManualEntry = true,
                    stripeException = APIException()
                ),
                allowManualEntry = true,
                disableLinkMoreAccounts = true,
            )
        ),
    )

    private fun institution() = FinancialConnectionsInstitution(
        id = "3",
        name = "Random Institution",
        url = "Random Institution url",
        featured = false,
        featuredOrder = null,
        icon = null,
        logo = null,
        mobileHandoffCapable = false
    )
}
