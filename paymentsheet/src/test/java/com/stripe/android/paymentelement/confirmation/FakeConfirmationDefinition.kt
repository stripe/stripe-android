package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import kotlinx.parcelize.Parcelize

internal class FakeConfirmationDefinition(
    private val onAction: (
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        intent: StripeIntent
    ) -> ConfirmationDefinition.Action<LauncherArgs> = { _, _ ->
        val exception = IllegalStateException("Failed!")

        ConfirmationDefinition.Action.Fail(
            cause = exception,
            message = exception.stripeErrorMessage(),
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
        )
    },
    private val result: ConfirmationDefinition.Result = ConfirmationDefinition.Result.Canceled(
        action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
    ),
    private val launcher: Launcher = Launcher(),
) : ConfirmationDefinition<
    PaymentMethodConfirmationOption.Saved,
    FakeConfirmationDefinition.Launcher,
    FakeConfirmationDefinition.LauncherArgs,
    FakeConfirmationDefinition.LauncherResult
    > {
    private val _launchCalls = Turbine<LaunchCall>()
    val launchCalls: ReceiveTurbine<LaunchCall> = _launchCalls

    private val _createLauncherCalls = Turbine<CreateLauncherCall>()
    val createLauncherCalls: ReceiveTurbine<CreateLauncherCall> = _createLauncherCalls

    private val _toResultCalls = Turbine<ToResultCall>()
    val toResultCalls: ReceiveTurbine<ToResultCall> =
        _toResultCalls

    override val key: String = "Test"

    override fun option(
        confirmationOption: ConfirmationHandler.Option
    ): PaymentMethodConfirmationOption.Saved? {
        return confirmationOption as? PaymentMethodConfirmationOption.Saved
    }

    override suspend fun action(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        intent: StripeIntent
    ): ConfirmationDefinition.Action<LauncherArgs> {
        return onAction(confirmationOption, intent)
    }

    override fun launch(
        launcher: Launcher,
        arguments: LauncherArgs,
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        intent: StripeIntent
    ) {
        _launchCalls.add(
            LaunchCall(
                launcher = launcher,
                arguments = arguments,
                confirmationOption = confirmationOption,
                intent = intent,
            )
        )
    }

    override fun createLauncher(
        activityResultCaller: ActivityResultCaller,
        onResult: (LauncherResult) -> Unit
    ): Launcher {
        _createLauncherCalls.add(
            CreateLauncherCall(
                activityResultCaller = activityResultCaller,
                onResult = onResult,
            )
        )

        return launcher
    }

    override fun toResult(
        confirmationOption: PaymentMethodConfirmationOption.Saved,
        deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        intent: StripeIntent,
        result: LauncherResult
    ): ConfirmationDefinition.Result {
        _toResultCalls.add(
            ToResultCall(
                confirmationOption = confirmationOption,
                deferredIntentConfirmationType = deferredIntentConfirmationType,
                intent = intent,
                result = result,
            )
        )

        return this.result
    }

    class LaunchCall(
        val launcher: Launcher,
        val arguments: LauncherArgs,
        val confirmationOption: PaymentMethodConfirmationOption.Saved,
        val intent: StripeIntent
    )

    class CreateLauncherCall(
        val activityResultCaller: ActivityResultCaller,
        val onResult: (LauncherResult) -> Unit
    )

    class ToResultCall(
        val confirmationOption: PaymentMethodConfirmationOption.Saved,
        val deferredIntentConfirmationType: DeferredIntentConfirmationType?,
        val intent: StripeIntent,
        val result: LauncherResult
    )

    class Launcher

    data class LauncherArgs(
        val amount: Long,
    )

    @Parcelize
    data class LauncherResult(
        val amount: Long,
    ) : Parcelable
}
