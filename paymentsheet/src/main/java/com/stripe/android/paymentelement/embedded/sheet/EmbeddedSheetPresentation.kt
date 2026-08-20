package com.stripe.android.paymentelement.embedded.sheet

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultCaller
import androidx.compose.runtime.Composable
import com.stripe.android.paymentelement.embedded.EmbeddedActivityArgs
import com.stripe.android.paymentelement.embedded.EmbeddedActivityResult
import com.stripe.android.paymentsheet.PaymentOptionsActivityResult
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PaymentSheetResult

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
            return when (args.presentationState) {
                EmbeddedActivityArgs.PresentationState.Loading -> LoadingEmbeddedSheetPresentation.Factory.create(
                    activity = activity,
                    args = args,
                    activityResultCaller = activityResultCaller,
                )
                EmbeddedActivityArgs.PresentationState.Ready ->
                    EmbeddedSheetViewModel.Factory { args }.createReadyPresentation(
                        activity = activity,
                        args = args,
                        activityResultCaller = activityResultCaller,
                    )
            }
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

internal fun EmbeddedSheetActivity.finishLoading(args: EmbeddedActivityArgs) {
    when (val configuration = args.activityConfiguration) {
        EmbeddedActivityArgs.ActivityConfiguration.Embedded -> finishWithResult(
            EmbeddedActivityResult.Cancelled(
                customerState = args.customerState,
                launchMode = args.launchMode,
            )
        )
        is EmbeddedActivityArgs.ActivityConfiguration.PaymentSheet -> {
            setResult(
                Activity.RESULT_OK,
                Intent().putExtras(PaymentSheetContract.Result(PaymentSheetResult.Canceled()).toBundle()),
            )
            finish()
        }
        is EmbeddedActivityArgs.ActivityConfiguration.PaymentOptions -> {
            val result = PaymentOptionsActivityResult.Canceled(
                mostRecentError = null,
                paymentSelection = configuration.initialSelection,
                paymentMethods = args.customerState?.paymentMethods,
                linkAccountInfo = configuration.initialLinkAccount,
            )
            setResult(result.resultCode, Intent().putExtras(result.toBundle()))
            finish()
        }
    }
}
