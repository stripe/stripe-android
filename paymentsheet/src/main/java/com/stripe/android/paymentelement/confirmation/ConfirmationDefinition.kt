package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType

internal interface ConfirmationDefinition<
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
    > {
    val key: String

    fun option(
        confirmationOption: ConfirmationHandler.Option,
    ): TConfirmationOption?

    suspend fun action(
        confirmationOption: TConfirmationOption,
        intent: StripeIntent,
    ): Action<TLauncherArgs>

    fun launch(
        launcher: TLauncher,
        arguments: TLauncherArgs,
        confirmationOption: TConfirmationOption,
        intent: StripeIntent,
    )

    fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (TLauncherResult) -> Unit,
    ): TLauncher

    fun toResult(
        confirmationOption: TConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: TLauncherResult,
    ): Result

    sealed interface Result {
        data class Canceled(
            val action: ConfirmationHandler.Result.Canceled.Action,
        ) : Result

        data class Succeeded(
            val intent: StripeIntent,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Result

        data class NextStep(
            val intent: StripeIntent,
            val confirmationOption: ConfirmationHandler.Option,
        ) : Result

        data class Failed(
            val cause: Throwable,
            val message: ResolvableString,
            val type: ConfirmationHandler.Result.Failed.ErrorType,
        ) : Result
    }

    sealed interface Action<TLauncherArgs> {
        data class Complete<TLauncherArgs>(
            val intent: StripeIntent,
            val confirmationOption: ConfirmationHandler.Option,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Action<TLauncherArgs>

        data class Fail<TLauncherArgs>(
            val cause: Throwable,
            val message: ResolvableString,
            val errorType: ConfirmationHandler.Result.Failed.ErrorType,
        ) : Action<TLauncherArgs>

        data class Launch<TLauncherArgs>(
            val launcherArguments: TLauncherArgs,
            val receivesResultInProcess: Boolean,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : Action<TLauncherArgs>
    }
}
