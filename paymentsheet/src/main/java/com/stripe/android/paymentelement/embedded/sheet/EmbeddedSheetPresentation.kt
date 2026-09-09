package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult

internal interface EmbeddedSheetPresentation {
    fun register()

    fun canDismiss(): Boolean

    fun onDismissed()

    @Composable
    fun Content()

    fun onDestroy()

    interface Factory {
        fun create(
            activity: EmbeddedSheetActivity,
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation
    }

    companion object : EmbeddedSheetPresentationFactory {
        override fun create(
            activity: EmbeddedSheetActivity,
            args: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            return EmbeddedSheetViewModel.Factory { args }.createReadyPresentation(
                activity = activity,
                args = args,
                activityResultCaller = activityResultCaller,
            )
        }
    }
}

internal interface EmbeddedSheetPresentationFactory {
    fun create(
        activity: EmbeddedSheetActivity,
        args: EmbeddedActivityArgs,
        activityResultCaller: ActivityResultCaller,
    ): EmbeddedSheetPresentation
}

internal fun EmbeddedSheetActivity.finishWithResult(result: EmbeddedActivityResult) {
    setResult(
        Activity.RESULT_OK,
        EmbeddedActivityResult.toIntent(intent, result),
    )
    finish()
}
