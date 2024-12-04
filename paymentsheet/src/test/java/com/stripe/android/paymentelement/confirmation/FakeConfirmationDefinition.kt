package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.R

internal abstract class FakeConfirmationDefinition<
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable,
    >(
    private val launcher: TLauncher,
    private val action: ConfirmationDefinition.Action<TLauncherArgs> = ConfirmationDefinition.Action.Fail(
        cause = IllegalStateException("Failed!"),
        message = R.string.stripe_something_went_wrong.resolvableString,
        errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
    ),
    private val result: ConfirmationDefinition.Result = ConfirmationDefinition.Result.Canceled(
        action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
    ),
) : ConfirmationDefinition<
    TConfirmationOption,
    TLauncher,
    TLauncherArgs,
    TLauncherResult
    > {
    override suspend fun action(
        confirmationOption: TConfirmationOption,
        intent: StripeIntent
    ): ConfirmationDefinition.Action<TLauncherArgs> {
        return action
    }

    override fun launch(
        launcher: TLauncher,
        arguments: TLauncherArgs,
        confirmationOption: TConfirmationOption,
        intent: StripeIntent
    ) {
        // Do nothing
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (TLauncherResult) -> Unit
    ): TLauncher {
        return launcher
    }

    override fun toResult(
        confirmationOption: TConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: TLauncherResult
    ): ConfirmationDefinition.Result {
        return this.result
    }
}
