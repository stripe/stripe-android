package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

internal abstract class FakeConfirmationDefinition<
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs : Parcelable,
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
        confirmationArgs: ConfirmationDefinition.Args,
    ): ConfirmationDefinition.Action<TLauncherArgs> {
        return action
    }

    override fun launch(
        launcher: TLauncher,
        arguments: TLauncherArgs,
        confirmationOption: TConfirmationOption,
        confirmationArgs: ConfirmationDefinition.Args,
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
        confirmationArgs: ConfirmationDefinition.Args,
        launcherArgs: TLauncherArgs,
        result: TLauncherResult
    ): ConfirmationDefinition.Result {
        return this.result
    }
}
