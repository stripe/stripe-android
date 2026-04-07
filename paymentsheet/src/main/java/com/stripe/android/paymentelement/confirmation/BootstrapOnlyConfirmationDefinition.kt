package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import kotlinx.parcelize.Parcelize

/**
 * A [ConfirmationDefinition] that only participates in the bootstrap phase.
 * It never matches any confirmation option, so [action], [launch], and [toResult] are never called.
 */
internal abstract class BootstrapOnlyConfirmationDefinition : ConfirmationDefinition<
    ConfirmationHandler.Option,
    Unit,
    BootstrapOnlyConfirmationDefinition.NoArgs,
    BootstrapOnlyConfirmationDefinition.NoResult
    > {

    final override fun option(confirmationOption: ConfirmationHandler.Option): ConfirmationHandler.Option? = null

    final override fun canConfirm(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ): Boolean = false

    final override suspend fun action(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ): ConfirmationDefinition.Action<NoArgs> {
        error("${this::class.simpleName} should not be used for confirmation")
    }

    final override fun launch(
        launcher: Unit,
        arguments: NoArgs,
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
    ) {
        error("${this::class.simpleName} should not be used for confirmation")
    }

    final override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (NoResult) -> Unit,
    ): Unit = Unit

    final override fun toResult(
        confirmationOption: ConfirmationHandler.Option,
        confirmationArgs: ConfirmationHandler.Args,
        launcherArgs: NoArgs,
        result: NoResult,
    ): ConfirmationDefinition.Result {
        error("${this::class.simpleName} should not be used for confirmation")
    }

    @Parcelize
    internal object NoArgs : Parcelable

    @Parcelize
    internal object NoResult : Parcelable
}
