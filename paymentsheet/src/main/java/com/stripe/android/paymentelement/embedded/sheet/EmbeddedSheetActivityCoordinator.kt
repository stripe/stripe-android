package com.stripe.android.paymentelement.embedded.sheet

import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs

internal class EmbeddedSheetActivityCoordinator(
    activity: EmbeddedSheetActivity,
    args: EmbeddedActivityArgs,
    presentationFactory: EmbeddedSheetPresentationFactory,
) {
    private val presentation = presentationFactory.create(
        activity = activity,
        args = args,
        activityResultCaller = activity,
    )

    fun register() {
        presentation.register()
    }

    fun canDismiss(): Boolean {
        return presentation.canDismiss()
    }

    fun onDismissed() {
        presentation.onDismissed()
    }

    @Composable
    fun Content() {
        presentation.Content()
    }

    fun onDestroy() {
        presentation.onDestroy()
    }
}
