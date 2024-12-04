package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.paymentsheet.R
import kotlinx.coroutines.test.runTest
import kotlinx.parcelize.Parcelize
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ConfirmationMediatorTest {
    @Test
    fun `On get key, should return the definition's key`() = test {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        assertThat(mediator.key).isEqualTo(definition.key)
    }

    @Test
    fun `On can confirm, should return true if definition is the same type`() = test {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val canConfirm = mediator.canConfirm(
            confirmationOption = TestConfirmationDefinition.Option,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(canConfirm).isTrue()
    }

    @Test
    fun `On can confirm, should return false if definition is not the same type`() = test {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val canConfirm = mediator.canConfirm(
            confirmationOption = InvalidTestConfirmationOption,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(InvalidTestConfirmationOption)
        assertThat(canConfirm).isFalse()
    }

    @Test
    fun `On register, should create launcher`() = test {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val activityResultCaller = mock<ActivityResultCaller>()

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = {},
        )

        val createLauncherCall = createLauncherCalls.awaitItem()

        assertThat(createLauncherCall.activityResultCaller).isEqualTo(activityResultCaller)
    }

    @Test
    fun `On incorrect confirmation option provided on action, should return fail action`() = test {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = InvalidTestConfirmationOption,
            intent = PaymentIntentFixtures.PI_SUCCEEDED,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(InvalidTestConfirmationOption)

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(failAction.cause.message).isEqualTo(
            "Parameter type of 'InvalidTestConfirmationOption' cannot be used with " +
                "ConfirmationMediator to read a result"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `On complete confirmation action, should return mediator complete action`() = test(
        action = ConfirmationDefinition.Action.Complete(
            confirmationOption = TestConfirmationDefinition.Option,
            intent = INTENT,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
        ),
    ) {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = TestConfirmationDefinition.Option,
            intent = INTENT,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

        val actionCall = actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(INTENT)

        val completeAction = action.asComplete()

        assertThat(completeAction.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(completeAction.intent).isEqualTo(INTENT)
        assertThat(completeAction.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On failed confirmation action, should return mediator fail action`() = test(
        action = ConfirmationDefinition.Action.Fail(
            cause = IllegalStateException("Failed!"),
            message = R.string.stripe_something_went_wrong.resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Fatal,
        ),
    ) {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = TestConfirmationDefinition.Option,
            intent = INTENT,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

        val actionCall = actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(INTENT)

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo("Failed!")
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
    }

    @Test
    fun `On launch action, should call definition launch and persist parameters`() = test(
        action = ConfirmationDefinition.Action.Launch(
            launcherArguments = TestConfirmationDefinition.LauncherArgs,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
            receivesResultInProcess = false,
        ),
    ) {
        val savedStateHandle = SavedStateHandle()

        val mediator = ConfirmationMediator(
            savedStateHandle = savedStateHandle,
            definition = definition
        ).apply {
            register(
                activityResultCaller = mock(),
                onResult = {}
            )
        }

        assertThat(createLauncherCalls.awaitItem()).isNotNull()

        val action = mediator.action(
            option = TestConfirmationDefinition.Option,
            intent = INTENT,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

        val actionCall = actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(INTENT)

        val launchAction = action.asLaunch()

        assertThat(launchAction.receivesResultInProcess).isFalse()

        launchAction.launch()

        val launchCall = launchCalls.awaitItem()

        assertThat(launchCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(launchCall.arguments).isEqualTo(TestConfirmationDefinition.LauncherArgs)
        assertThat(launchCall.intent).isEqualTo(INTENT)
        assertThat(launchCall.launcher).isEqualTo(TestConfirmationDefinition.Launcher)

        val parameters = savedStateHandle
            .get<ConfirmationMediator.Parameters<TestConfirmationDefinition.Option>>(
                "TestParameters"
            )

        assertThat(parameters?.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(parameters?.intent).isEqualTo(INTENT)
        assertThat(parameters?.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On launch definition action where result is received in process, 'receivesResultInProcess' should be true`() =
        test(
            action = ConfirmationDefinition.Action.Launch(
                launcherArguments = TestConfirmationDefinition.LauncherArgs,
                deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
                receivesResultInProcess = true,
            ),
        ) {
            val mediator = ConfirmationMediator(
                savedStateHandle = SavedStateHandle(),
                definition = definition
            ).apply {
                register(
                    activityResultCaller = mock(),
                    onResult = {}
                )
            }

            assertThat(createLauncherCalls.awaitItem()).isNotNull()

            val action = mediator.action(
                option = TestConfirmationDefinition.Option,
                intent = INTENT,
            )

            assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

            val actionCall = actionCalls.awaitItem()

            assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
            assertThat(actionCall.intent).isEqualTo(INTENT)

            val launchAction = action.asLaunch()

            assertThat(launchAction.receivesResultInProcess).isTrue()
        }

    @Test
    fun `On confirmation action without registering, should return fail action`() = test(
        action = ConfirmationDefinition.Action.Launch(
            launcherArguments = TestConfirmationDefinition.LauncherArgs,
            deferredIntentConfirmationType = null,
            receivesResultInProcess = false,
        ),
    ) {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        val action = mediator.action(
            option = TestConfirmationDefinition.Option,
            intent = INTENT,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

        val actionCall = actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(INTENT)

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo(
            "No launcher for RecordingConfirmationDefinition was found, did you call register?"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
    }

    @Test
    fun `On confirmation action after un-registering, should return fail action`() = test(
        action = ConfirmationDefinition.Action.Launch(
            launcherArguments = TestConfirmationDefinition.LauncherArgs,
            deferredIntentConfirmationType = null,
            receivesResultInProcess = false,
        ),
    ) {
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition
        )

        mediator.register(
            activityResultCaller = mock(),
            onResult = {}
        )
        mediator.unregister()

        assertThat(createLauncherCalls.awaitItem()).isNotNull()

        val action = mediator.action(
            option = TestConfirmationDefinition.Option,
            intent = INTENT,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

        val actionCall = actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(INTENT)

        val failAction = action.asFail()

        assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failAction.cause.message).isEqualTo(
            "No launcher for RecordingConfirmationDefinition was found, did you call register?"
        )
        assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failAction.errorType).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
    }

    @Test
    fun `On result, should attempt to convert launcher result to confirmation result and return it`() = test(
        action = ConfirmationDefinition.Action.Launch(
            launcherArguments = TestConfirmationDefinition.LauncherArgs,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
            receivesResultInProcess = false,
        ),
        result = ConfirmationDefinition.Result.Succeeded(
            intent = INTENT,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
        ),
    ) {
        val waitForResultLatch = CountDownLatch(1)

        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        var receivedResult: ConfirmationDefinition.Result? = null

        mediator.register(
            activityResultCaller = mock(),
            onResult = { result ->
                receivedResult = result

                waitForResultLatch.countDown()
            },
        )

        val createLauncherCall = createLauncherCalls.awaitItem()

        val action = mediator.action(
            intent = INTENT,
            option = TestConfirmationDefinition.Option,
        )

        assertThat(optionCalls.awaitItem().option).isEqualTo(TestConfirmationDefinition.Option)

        val actionCall = actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(INTENT)

        assertThat(action).isInstanceOf<ConfirmationMediator.Action.Launch>()

        val launchAction = action.asLaunch()

        launchAction.launch()

        val launchCall = launchCalls.awaitItem()

        assertThat(launchCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(launchCall.arguments).isEqualTo(TestConfirmationDefinition.LauncherArgs)
        assertThat(launchCall.intent).isEqualTo(INTENT)
        assertThat(launchCall.launcher).isEqualTo(TestConfirmationDefinition.Launcher)

        createLauncherCall.onResult(TestConfirmationDefinition.LauncherResult)

        waitForResultLatch.await(2, TimeUnit.SECONDS)

        val toPaymentConfirmationResultCall = toResultCalls.awaitItem()

        assertThat(toPaymentConfirmationResultCall.confirmationOption).isEqualTo(TestConfirmationDefinition.Option)
        assertThat(toPaymentConfirmationResultCall.intent).isEqualTo(INTENT)
        assertThat(toPaymentConfirmationResultCall.result).isEqualTo(TestConfirmationDefinition.LauncherResult)
        assertThat(toPaymentConfirmationResultCall.deferredIntentConfirmationType)
            .isEqualTo(DeferredIntentConfirmationType.Client)

        assertThat(receivedResult).isInstanceOf<ConfirmationDefinition.Result.Succeeded>()

        val successResult = receivedResult.asSucceeded()

        assertThat(successResult.intent).isEqualTo(INTENT)
        assertThat(successResult.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
    }

    @Test
    fun `On result with no persisted parameters, should return failed result`() = test {
        val countDownLatch = CountDownLatch(1)
        val mediator = ConfirmationMediator(
            savedStateHandle = SavedStateHandle(),
            definition = definition,
        )

        val activityResultCaller = mock<ActivityResultCaller>()

        mediator.register(
            activityResultCaller = activityResultCaller,
            onResult = { result ->
                assertThat(result).isInstanceOf<ConfirmationDefinition.Result.Failed>()

                val failAction = result.asFailed()

                assertThat(failAction.cause).isInstanceOf(IllegalStateException::class.java)
                assertThat(failAction.cause.message).isEqualTo(
                    "Arguments should have been initialized before handling result!"
                )
                assertThat(failAction.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
                assertThat(failAction.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)

                countDownLatch.countDown()
            },
        )

        val createLauncherCall = createLauncherCalls.awaitItem()

        createLauncherCall.onResult(TestConfirmationDefinition.LauncherResult)

        countDownLatch.await(2, TimeUnit.SECONDS)
    }

    private fun test(
        action: ConfirmationDefinition.Action<TestConfirmationDefinition.LauncherArgs> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        result: ConfirmationDefinition.Result = ConfirmationDefinition.Result.Canceled(
            action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
        ),
        scenarioTest: suspend RecordingConfirmationDefinition.Scenario<
            TestConfirmationDefinition.Option,
            TestConfirmationDefinition.Launcher,
            TestConfirmationDefinition.LauncherArgs,
            TestConfirmationDefinition.LauncherResult,
            >.() -> Unit
    ) = runTest {
        RecordingConfirmationDefinition.test(TestConfirmationDefinition(action, result), scenarioTest)
    }

    private fun ConfirmationHandler.Result.asFailed(): ConfirmationHandler.Result.Failed {
        return this as ConfirmationHandler.Result.Failed
    }

    private fun ConfirmationMediator.Action.asFail(): ConfirmationMediator.Action.Fail {
        return this as ConfirmationMediator.Action.Fail
    }

    private fun ConfirmationMediator.Action.asComplete(): ConfirmationMediator.Action.Complete {
        return this as ConfirmationMediator.Action.Complete
    }

    private fun ConfirmationMediator.Action.asLaunch(): ConfirmationMediator.Action.Launch {
        return this as ConfirmationMediator.Action.Launch
    }

    private class TestConfirmationDefinition(
        action: ConfirmationDefinition.Action<LauncherArgs>,
        result: ConfirmationDefinition.Result,
    ) : FakeConfirmationDefinition<
        TestConfirmationDefinition.Option,
        TestConfirmationDefinition.Launcher,
        TestConfirmationDefinition.LauncherArgs,
        TestConfirmationDefinition.LauncherResult,
        >(
        launcher = Launcher,
        action = action,
        result = result,
    ) {
        override val key: String = "Test"

        override fun option(confirmationOption: ConfirmationHandler.Option): Option? {
            return confirmationOption as? Option
        }

        @Parcelize
        data object Option : ConfirmationHandler.Option

        object Launcher

        data object LauncherArgs

        @Parcelize
        data object LauncherResult : Parcelable
    }

    @Parcelize
    private object InvalidTestConfirmationOption : ConfirmationHandler.Option

    private companion object {
        private val INTENT = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
    }
}
