package com.stripe.android.financialconnections.features.success

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
import com.stripe.android.financialconnections.repository.SuccessContentRepository
import com.stripe.android.financialconnections.ui.TextResource

internal class SuccessPreviewParameterProvider :
    PreviewParameterProvider<SuccessState> {
    override val values = sequenceOf(
        canonical(),
        customMessage()
    )

    private fun canonical() = SuccessState(
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                accountsCount = 1,
                customSuccessContent = null,
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )

    private fun customMessage() = SuccessState(
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                accountsCount = 1,
                customSuccessContent = SuccessContentRepository.State(
                    message = TextResource.Text(
                        "You can expect micro-deposits to account " +
                            "••••1234 in 1-2 days and an email with further instructions."
                    ),
                    heading = TextResource.Text("Success!")
                ),
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )
}
