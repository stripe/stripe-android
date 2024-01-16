@file:Suppress("LongMethod")

package com.stripe.android.financialconnections.features.success

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized

internal class SuccessPreviewParameterProvider :
    PreviewParameterProvider<SuccessState> {
    override val values = sequenceOf(
        canonical(),
        animationCompleted()
    )

    override val count: Int
        get() = super.count

    private fun canonical() = SuccessState(
        overrideAnimationForPreview = false,
        payload = Success(
            SuccessState.Payload(
                skipSuccessPane = false,
                accountsCount = 1,
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
                businessName = "Stripe",
            )
        ),
        completeSession = Uninitialized,
    )

}
