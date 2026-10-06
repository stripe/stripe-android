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

    companion object : EmbeddedSheetPresentationFactory {
        override fun create(
            activity: EmbeddedSheetActivity,
            state: EmbeddedActivityArgs,
            activityResultCaller: ActivityResultCaller,
        ): EmbeddedSheetPresentation {
            return when (state) {
                is EmbeddedActivityArgs.LoadingPaymentOptions -> LoadingEmbeddedSheetPresentation.Factory.create(
                    activity = activity,
                    state = state,
                )
                is EmbeddedActivityArgs.Ready ->
                    EmbeddedSheetViewModel.Factory { state }.createReadyPresentation(
                        activity = activity,
                        state = state,
                        activityResultCaller = activityResultCaller,
                    )
            }
        }
    }
}

internal interface EmbeddedSheetPresentationFactory {
    fun create(
        activity: EmbeddedSheetActivity,
        state: EmbeddedActivityArgs,
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
