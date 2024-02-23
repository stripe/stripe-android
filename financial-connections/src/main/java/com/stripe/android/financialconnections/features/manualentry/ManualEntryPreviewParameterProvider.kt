package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.R

internal class ManualEntryPreviewParameterProvider : PreviewParameterProvider<ManualEntryState> {
    override val values = sequenceOf(
        canonical(),
        failure(),
        fieldFailure()
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
            APIException(message = "Test bank accounts cannot be used in live mode")
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

    private fun fieldFailure() = ManualEntryState(
        routing = ManualEntryState.InputState(
            maxLength = 9,
            value = "123456789",
            error = R.string.stripe_validation_no_us_routing,
        ),
        account = ManualEntryState.InputState(
            maxLength = 17,
            value = "123456789",
            error = R.string.stripe_validation_no_us_routing,
        ),
        accountConfirm = ManualEntryState.InputState(
            maxLength = 17,
            value = "123456789",
            error = R.string.stripe_validation_no_us_routing,
        ),
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false
            )
        ),
        linkPaymentAccount = Uninitialized,
    )
}
