@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.success

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.stripe.android.financialconnections.ui.TextResource

internal class SuccessPreviewParameterProvider :
    PreviewParameterProvider<SuccessState> {
    override val values = sequenceOf(
        canonical(),
        animationCompleted(),
        animationCompletedCustomMessage()
    )

    override val count: Int
        get() = super.count

    private fun canonical() = SuccessState(
        overrideAnimationForPreview = false,
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                accountsCount = 1,
                customSuccessMessage = null,
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )

    private fun animationCompleted() = SuccessState(
        overrideAnimationForPreview = true,
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                accountsCount = 1,
                customSuccessMessage = null,
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )

    private fun animationCompletedCustomMessage() = SuccessState(
        overrideAnimationForPreview = true,
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                accountsCount = 1,
                customSuccessMessage = TextResource.Text(
                    "You can expect micro-deposits to account " +
                        "••••1234 in 1-2 days and an email with further instructions."
                ),
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )
}
