package com.stripe.android.financialconnections.features.manualentry

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.core.exception.APIException
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.features.manualentry.ManualEntryPreviewParameterProvider.PreviewState
import com.stripe.android.financialconnections.presentation.Async.Fail
import com.stripe.android.financialconnections.presentation.Async.Loading
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized

internal class ManualEntryPreviewParameterProvider : PreviewParameterProvider<PreviewState> {
    override val values = sequenceOf(
        canonical(),
        loading(),
        failure(),
        fieldFailure(),
        testMode(),
    )

    override val count: Int
        get() = super.count

    private fun loading() = PreviewState(
        state = ManualEntryState(
            payload = Success(
                ManualEntryState.Payload(
                    verifyWithMicrodeposits = true,
                    customManualEntry = false,
                    testMode = false
                )
            ),
            linkPaymentAccount = Loading(),
        )
    )

    private fun failure() = PreviewState(
        state = ManualEntryState(
            payload = Success(
                ManualEntryState.Payload(
                    verifyWithMicrodeposits = true,
                    customManualEntry = false,
                    testMode = false
                )
            ),
            linkPaymentAccount = Fail(
                APIException(
                    message = "Test bank accounts cannot be used in live mode"
                )
            ),
        )
    )

    private fun canonical() = PreviewState(
        state = ManualEntryState(
            payload = Success(
                ManualEntryState.Payload(
                    verifyWithMicrodeposits = true,
                    customManualEntry = false,
                    testMode = false
                )
            ),
            linkPaymentAccount = Uninitialized
        )
    )

    private fun testMode() = PreviewState(
        state = ManualEntryState(
            payload = Success(
                ManualEntryState.Payload(
                    verifyWithMicrodeposits = true,
                    customManualEntry = false,
                    testMode = true
                )
            ),
            linkPaymentAccount = Uninitialized
        )
    )

    private fun fieldFailure() = PreviewState(
        routing = "123456789",
        routingError = R.string.stripe_validation_no_us_routing,
        account = "123456789",
        accountError = R.string.stripe_validation_no_us_routing,
        accountConfirm = "123456789",
        accountConfirmError = R.string.stripe_validation_no_us_routing,
        state = ManualEntryState(
            payload = Success(
                ManualEntryState.Payload(
                    verifyWithMicrodeposits = true,
                    customManualEntry = false,
                    testMode = false
                )
            ),
            linkPaymentAccount = Uninitialized,
        )
    )

    data class PreviewState(
        val state: ManualEntryState,
        val routing: String = "",
        val account: String = "",
        val accountConfirm: String = "",
        val routingError: Int? = null,
        val accountError: Int? = null,
        val accountConfirmError: Int? = null,
    )
}
