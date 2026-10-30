package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode

internal class FailedEmbeddedSheetPresentation(
    private val activity: EmbeddedSheetActivity,
) : EmbeddedSheetPresentation {
    override fun register() {
        activity.finishWithResult(
            EmbeddedActivityResult.Error(launchMode = EmbeddedLaunchMode.PaymentOptions)
        )
    }

    override fun canDismiss(): Boolean = false

    override fun onDismissed() = Unit

    @Composable
    override fun Content() = Unit

    override fun onDestroy() = Unit
}
