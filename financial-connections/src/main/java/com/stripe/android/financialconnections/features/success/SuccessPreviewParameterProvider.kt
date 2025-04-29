package com.stripe.android.financialconnections.features.success

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.stripe.android.financialconnections.R
import com.stripe.android.financialconnections.presentation.Async.Success
import com.stripe.android.financialconnections.presentation.Async.Uninitialized
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
                title = TextResource.StringId(R.string.stripe_success_pane_title),
                content = TextResource.PluralId(
                    singular = R.string.stripe_success_pane_desc_singular,
                    plural = R.string.stripe_success_pane_desc_plural,
                    count = 3
                ),
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )

    private fun customMessage() = SuccessState(
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                title = TextResource.Text("Success"),
                content = TextResource.Text(
                    "You can expect micro-deposits to account " +
                        "••••1234 in 1-2 days and an email with further instructions."
                ),
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )
}
