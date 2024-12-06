package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCallback
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.ConfirmationMediator.Parameters
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
    parameters: ConfirmationDefinition.Parameters
) = runTest {
    val savedStateHandle = SavedStateHandle()
    val mediator = ConfirmationMediator(savedStateHandle, definition)

    DummyActivityResultCaller.test {
        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {}
        )

        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        val action = mediator.action(
            option = confirmationOption,
            parameters = parameters,
        )

        assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

        val launchAction = action.asLaunch()

        launchAction.launch()

        val savedParameters = savedStateHandle
            .get<Parameters<TConfirmationOption>>("${definition.key}Parameters")

        assertThat(savedParameters?.confirmationOption).isEqualTo(confirmationOption)
        assertThat(savedParameters?.confirmationParameters).isEqualTo(parameters)
        assertThat(savedParameters?.deferredIntentConfirmationType).isNull()

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
    parameters: ConfirmationDefinition.Parameters,
    launcherResult: TLauncherResult,
    definitionResult: ConfirmationDefinition.Result,
) = runTest {
    val countDownLatch = CountDownLatch(1)

    val savedStateHandle = SavedStateHandle().apply {
        set(
            "${definition.key}Parameters",
            Parameters(
                confirmationOption = confirmationOption,
                confirmationParameters = parameters,
                deferredIntentConfirmationType = null,
            )
        )
    }

    DummyActivityResultCaller.test {
        val mediator = ConfirmationMediator(savedStateHandle, definition)

        var result: ConfirmationDefinition.Result? = null

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {
                result = it

                countDownLatch.countDown()
            }
        )

        val call = awaitRegisterCall()

        assertThat(awaitNextRegisteredLauncher()).isNotNull()

        call.callback.asCallbackFor<TLauncherResult>().onActivityResult(launcherResult)

        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue()

        assertThat(result).isEqualTo(definitionResult)
    }
}

internal fun ConfirmationHandler.Option.asSaved(): PaymentMethodConfirmationOption.Saved {
    return this as PaymentMethodConfirmationOption.Saved
}

internal fun ConfirmationDefinition.Result?.asSucceeded(): ConfirmationDefinition.Result.Succeeded {
    return this as ConfirmationDefinition.Result.Succeeded
}

internal fun ConfirmationDefinition.Result?.asNextStep(): ConfirmationDefinition.Result.NextStep {
    return this as ConfirmationDefinition.Result.NextStep
}

internal fun ConfirmationDefinition.Result?.asFailed(): ConfirmationDefinition.Result.Failed {
    return this as ConfirmationDefinition.Result.Failed
}

internal fun ConfirmationDefinition.Result?.asCanceled(): ConfirmationDefinition.Result.Canceled {
    return this as ConfirmationDefinition.Result.Canceled
}

internal fun <T> ConfirmationDefinition.Action<T>.asFail(): ConfirmationDefinition.Action.Fail<T> {
    return this as ConfirmationDefinition.Action.Fail<T>
}

internal fun <T> ConfirmationDefinition.Action<T>.asLaunch(): ConfirmationDefinition.Action.Launch<T> {
    return this as ConfirmationDefinition.Action.Launch<T>
}

internal fun ConfirmationMediator.Action.asLaunch(): ConfirmationMediator.Action.Launch {
    return this as ConfirmationMediator.Action.Launch
}

internal fun <T> ActivityResultCallback<*>.asCallbackFor(): ActivityResultCallback<T> {
    @Suppress("UNCHECKED_CAST")
    return this as ActivityResultCallback<T>
}

internal fun ConfirmationHandler.State.assertIdle(): ConfirmationHandler.State.Idle {
    assertThat(this).isInstanceOf<ConfirmationHandler.State.Idle>()

    return this as ConfirmationHandler.State.Idle
}

internal fun ConfirmationHandler.State.assertConfirming(): ConfirmationHandler.State.Confirming {
    assertThat(this).isInstanceOf<ConfirmationHandler.State.Confirming>()

    return this as ConfirmationHandler.State.Confirming
}

internal fun ConfirmationHandler.State.assertComplete(): ConfirmationHandler.State.Complete {
    assertThat(this).isInstanceOf<ConfirmationHandler.State.Complete>()

    return this as ConfirmationHandler.State.Complete
}

internal fun ConfirmationHandler.Result?.assertSucceeded(): ConfirmationHandler.Result.Succeeded {
    assertThat(this).isInstanceOf<ConfirmationHandler.Result.Succeeded>()

    return this as ConfirmationHandler.Result.Succeeded
}

internal fun ConfirmationHandler.Result?.assertFailed(): ConfirmationHandler.Result.Failed {
    assertThat(this).isInstanceOf<ConfirmationHandler.Result.Failed>()

    return this as ConfirmationHandler.Result.Failed
}

internal fun ConfirmationHandler.Result?.assertCanceled(): ConfirmationHandler.Result.Canceled {
    assertThat(this).isInstanceOf<ConfirmationHandler.Result.Canceled>()

    return this as ConfirmationHandler.Result.Canceled
}
