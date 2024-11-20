package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCallback
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator.Parameters
import com.stripe.android.paymentelement.confirmation.epms.ExternalPaymentMethodConfirmationOption
import com.stripe.android.payments.paymentlauncher.PaymentResult
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.test.runTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal fun <
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
    > runLaunchTest(
    definition: ConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>,
    confirmationOption: ConfirmationHandler.Option,
    intent: StripeIntent,
) = runTest {
    val savedStateHandle = SavedStateHandle()
    val mediator = ConfirmationMediator(savedStateHandle, definition)

    DummyActivityResultCaller.test {
        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {}
        )

        val action = mediator.action(
            option = confirmationOption,
            intent = intent,
        )

        assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

        val launchAction = action.asLaunch()

        launchAction.launch()

        val parameters = savedStateHandle
            .get<Parameters<ExternalPaymentMethodConfirmationOption>>("ExternalPaymentMethodParameters")

        assertThat(parameters?.confirmationOption).isEqualTo(confirmationOption)
        assertThat(parameters?.intent).isEqualTo(intent)
        assertThat(parameters?.deferredIntentConfirmationType).isNull()

        assertThat(awaitRegisterCall()).isNotNull()
        assertThat(awaitLaunchCall()).isNotNull()
    }
}

internal fun <
    TConfirmationOption : ConfirmationHandler.Option,
    TLauncher,
    TLauncherArgs,
    TLauncherResult : Parcelable
    > runResultTest(
    definition: ConfirmationDefinition<TConfirmationOption, TLauncher, TLauncherArgs, TLauncherResult>,
    confirmationOption: ConfirmationHandler.Option,
    intent: StripeIntent,
) = runTest {
    val countDownLatch = CountDownLatch(1)

    val savedStateHandle = SavedStateHandle().apply {
        set(
            "${definition.key}Parameters",
            Parameters(
                confirmationOption = confirmationOption,
                intent = intent,
                deferredIntentConfirmationType = null,
            )
        )
    }

    DummyActivityResultCaller.test {
        val mediator = ConfirmationMediator(savedStateHandle, definition)

        var result: ConfirmationHandler.Result? = null

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {
                result = it

                countDownLatch.countDown()
            }
        )

        val call = awaitRegisterCall()

        call.callback.asCallbackFor<PaymentResult>().onActivityResult(PaymentResult.Completed)

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(result).isInstanceOf<ConfirmationHandler.Result.Succeeded>()

        val successResult = result.asSucceeded()

        assertThat(successResult.intent).isEqualTo(intent)
    }
}

internal fun ConfirmationHandler.Result?.asSucceeded(): ConfirmationHandler.Result.Succeeded {
    return this as ConfirmationHandler.Result.Succeeded
}

internal fun ConfirmationHandler.Result?.asFailed(): ConfirmationHandler.Result.Failed {
    return this as ConfirmationHandler.Result.Failed
}

internal fun ConfirmationHandler.Result?.asCanceled(): ConfirmationHandler.Result.Canceled {
    return this as ConfirmationHandler.Result.Canceled
}

internal fun <T> ConfirmationDefinition.ConfirmationAction<T>.asFail():
    ConfirmationDefinition.ConfirmationAction.Fail<T> {
    return this as ConfirmationDefinition.ConfirmationAction.Fail<T>
}

internal fun <T> ConfirmationDefinition.ConfirmationAction<T>.asLaunch():
    ConfirmationDefinition.ConfirmationAction.Launch<T> {
    return this as ConfirmationDefinition.ConfirmationAction.Launch<T>
}

internal fun ConfirmationMediator.Action.asLaunch(): ConfirmationMediator.Action.Launch {
    return this as ConfirmationMediator.Action.Launch
}

internal fun <T> ActivityResultCallback<*>.asCallbackFor(): ActivityResultCallback<T> {
    @Suppress("UNCHECKED_CAST")
    return this as ActivityResultCallback<T>
}
