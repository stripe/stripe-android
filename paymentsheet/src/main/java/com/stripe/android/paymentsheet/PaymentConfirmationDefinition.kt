package com.stripe.android.paymentsheet

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.model.StripeIntent

internal interface PaymentConfirmationDefinition<
    TConfirmationOption : PaymentConfirmationOption,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
    > {
    val key: String

    fun option(
        confirmationOption: PaymentConfirmationOption,
    ): TConfirmationOption?

    suspend fun action(
        confirmationOption: TConfirmationOption,
        intent: StripeIntent,
    ): ConfirmationAction<TLauncherArgs>

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

    fun toPaymentConfirmationResult(
        confirmationOption: TConfirmationOption,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: TLauncherResult,
    ): PaymentConfirmationResult

    sealed interface ConfirmationAction<TLauncherArgs> {
        data class Complete<TLauncherArgs>(
            val intent: StripeIntent,
            val confirmationOption: PaymentConfirmationOption,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : ConfirmationAction<TLauncherArgs>

        data class Fail<TLauncherArgs>(
            val cause: Throwable,
            val message: ResolvableString,
            val errorType: PaymentConfirmationErrorType,
        ) : ConfirmationAction<TLauncherArgs>

        data class Launch<TLauncherArgs>(
            val launcherArguments: TLauncherArgs,
            val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        ) : ConfirmationAction<TLauncherArgs>
    }
}
