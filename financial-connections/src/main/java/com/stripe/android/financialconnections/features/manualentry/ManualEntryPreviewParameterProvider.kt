package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.R

internal class ManualEntryPreviewParameterProvider : PreviewParameterProvider<ManualEntryState> {
    override val values = sequenceOf(
        canonical(),
        loading(),
        failure(),
        fieldFailure(),
        testMode(),
    )

    override val count: Int
        get() = super.count

    private fun loading() = ManualEntryState(
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false,
                testMode = false
            )
        ),
        linkPaymentAccount = Loading(),
    )

    private fun failure() = ManualEntryState(
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false,
                testMode = false
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
                customManualEntry = false,
                testMode = false
            )
        ),
        linkPaymentAccount = Uninitialized
    )

    private fun testMode() = ManualEntryState(
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false,
                testMode = true
            )
        ),
        linkPaymentAccount = Uninitialized
    )

    private fun fieldFailure() = ManualEntryState(
        routing = "123456789",
        routingError = R.string.stripe_validation_no_us_routing,
        account = "123456789",
        accountError = R.string.stripe_validation_no_us_routing,
        accountConfirm = "123456789",
        accountConfirmError = R.string.stripe_validation_no_us_routing,
        payload = Success(
            ManualEntryState.Payload(
                verifyWithMicrodeposits = true,
                customManualEntry = false,
                testMode = false
            )
        ),
        linkPaymentAccount = Uninitialized,
    )
}
