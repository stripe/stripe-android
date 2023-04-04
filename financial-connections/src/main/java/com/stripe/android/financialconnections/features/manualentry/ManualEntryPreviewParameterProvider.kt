package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.core.exception.APIException

internal class ManualEntryPreviewParameterProvider : PreviewParameterProvider<ManualEntryState> {
    override val values = sequenceOf(
        canonical(),
        failure(),
    )

    override val count: Int
        get() = super.count

    private fun failure() = ManualEntryState(
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false
            )
        ),
        linkPaymentAccount = Fail(
            APIException(message = "Error linking accounts")
        ),
    )

    private fun canonical() = ManualEntryState(
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false
            )
        ),
        linkPaymentAccount = Uninitialized
    )
}
