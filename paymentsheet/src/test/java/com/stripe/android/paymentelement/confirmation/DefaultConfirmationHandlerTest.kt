package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentelement.confirmation.intent.DeferredIntentConfirmationType
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentIntentFactory
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.utils.DummyActivityResultCaller
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class DefaultConfirmationHandlerTest {
    @Test
    fun `On initial register, should create launchers for each definition`() = test(shouldRegister = false) {
        assertThat(confirmationHandler.hasReloadedFromProcessDeath).isFalse()

        val activityResultCaller = mock<ActivityResultCaller>()

        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = TestLifecycleOwner(),
        )

        val someDefinitionCreateLauncherCall = someDefinitionScenario.createLauncherCalls.awaitItem()
        val someOtherDefinitionCreateLauncherCall = someOtherDefinitionScenario.createLauncherCalls.awaitItem()

        assertThat(someDefinitionCreateLauncherCall.activityResultCaller).isEqualTo(activityResultCaller)
        assertThat(someOtherDefinitionCreateLauncherCall.activityResultCaller).isEqualTo(activityResultCaller)
    }

    @Test
    fun `On lifecycle destroyed, should unregister all launchers`() = test(
        shouldRegister = false,
        someDefinitionAction = ConfirmationDefinition.Action.Launch(
            launcherArguments = SomeConfirmationDefinition.LauncherArgs,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
        ),
        someOtherDefinitionAction = ConfirmationDefinition.Action.Launch(
            launcherArguments = SomeOtherConfirmationDefinition.LauncherArgs,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
        ),
    ) {
        val activityResultCaller = mock<ActivityResultCaller>()
        val lifecycleOwner = TestLifecycleOwner(
            coroutineDispatcher = UnconfinedTestDispatcher(),
        )

        confirmationHandler.register(
            activityResultCaller = activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        assertThat(someDefinitionScenario.createLauncherCalls.awaitItem()).isNotNull()
        assertThat(someOtherDefinitionScenario.createLauncherCalls.awaitItem()).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(someDefinitionScenario.unregisterCalls.awaitItem()).isNotNull()
        assertThat(someOtherDefinitionScenario.unregisterCalls.awaitItem()).isNotNull()

        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionConfirmingState()

            val someDefinitionFailedResult = awaitCompleteState().result.assertFailed()

            assertThat(someDefinitionFailedResult.cause).isInstanceOf(IllegalStateException::class.java)
            assertThat(someDefinitionFailedResult.cause.message).isEqualTo(
                "No launcher for RecordingConfirmationDefinition was found, did you call register?"
            )
            assertThat(someDefinitionFailedResult.message)
                .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(someDefinitionFailedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeOtherConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeOtherDefinitionActionCalled()
            assertSomeOtherDefinitionConfirmingState()

            val someOtherDefinitionFailedResult = awaitCompleteState().result.assertFailed()

            assertThat(someOtherDefinitionFailedResult.cause).isInstanceOf(IllegalStateException::class.java)
            assertThat(someOtherDefinitionFailedResult.cause.message).isEqualTo(
                "No launcher for RecordingConfirmationDefinition was found, did you call register?"
            )
            assertThat(someOtherDefinitionFailedResult.message)
                .isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(someOtherDefinitionFailedResult.type)
                .isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Fatal)
        }
    }

    @Test
    fun `On 'start' with unknown option, should fail and report unexpected error`() = test {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = InvalidConfirmationOption,
                    intent = PAYMENT_INTENT,
                )
            )

            // Should check all passed definitions
            assertThat(someDefinitionScenario.optionCalls.awaitItem().option)
                .isEqualTo(InvalidConfirmationOption)
            assertThat(someOtherDefinitionScenario.optionCalls.awaitItem().option)
                .isEqualTo(InvalidConfirmationOption)

            val confirmingState = awaitItem().assertConfirming()

            assertThat(confirmingState.option).isEqualTo(InvalidConfirmationOption)

            val completeState = awaitCompleteState()

            val failedResult = completeState.result.assertFailed()

            assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedResult.cause.message).isEqualTo(
                "Attempted to confirm invalid ${InvalidConfirmationOption::class.qualifiedName} " +
                    "confirmation type"
            )
            assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)

            assertThat(errorReporter.getLoggedErrors()).containsExactly(
                ErrorReporter
                    .UnexpectedErrorEvent
                    .INTENT_CONFIRMATION_HANDLER_INVALID_PAYMENT_CONFIRMATION_OPTION
                    .eventName,
            )
        }
    }

    @Test
    fun `On 'start' with 'SomeDefinition' option, should complete with failed result`() = test(
        someDefinitionAction = ConfirmationDefinition.Action.Fail(
            cause = IllegalStateException("Failed!"),
            message = R.string.stripe_something_went_wrong.resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
        )
    ) {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionConfirmingState()

            val completeState = awaitCompleteState()
            val failedResult = completeState.result.assertFailed()

            assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedResult.cause.message).isEqualTo("Failed!")
            assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)

            confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
        }
    }

    @Test
    fun `On 'start' with 'SomeOtherDefinition' option, should complete with failed result`() = test(
        someOtherDefinitionAction = ConfirmationDefinition.Action.Fail(
            cause = IllegalStateException("Failed!"),
            message = R.string.stripe_something_went_wrong.resolvableString,
            errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
        )
    ) {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeOtherConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeOtherDefinitionActionCalled()
            assertSomeOtherDefinitionConfirmingState()

            val completeState = awaitCompleteState()
            val failedResult = completeState.result.assertFailed()

            assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
            assertThat(failedResult.cause.message).isEqualTo("Failed!")
            assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
            assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)

            confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
        }
    }

    @Test
    fun `On complete action, should complete with success result`() = test(
        someDefinitionAction = ConfirmationDefinition.Action.Complete(
            intent = UPDATED_PAYMENT_INTENT,
            confirmationOption = SomeConfirmationDefinition.Option,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
        ),
    ) {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionConfirmingState()

            val completeState = awaitCompleteState()
            val successResult = completeState.result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(UPDATED_PAYMENT_INTENT)
            assertThat(successResult.deferredIntentConfirmationType)
                .isEqualTo(DeferredIntentConfirmationType.Client)

            confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
        }
    }

    @Test
    fun `On launch action with receives result in process as true, should launch & persist as expected`() =
        persistedParametersTest(receivesResultInProcess = true)

    @Test
    fun `On launch action with receives result in process as false, should launch & persist as expected`() =
        persistedParametersTest(receivesResultInProcess = false)

    @Test
    fun `On success result from launched action, should complete with success result`() = launcherResultTest(
        result = ConfirmationDefinition.Result.Succeeded(
            intent = UPDATED_PAYMENT_INTENT,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Server,
        ),
    ) { completeState ->
        val successResult = completeState.result.assertSucceeded()

        assertThat(successResult.intent).isEqualTo(UPDATED_PAYMENT_INTENT)
        assertThat(successResult.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Server)
    }

    @Test
    fun `On failed result from launched action, should complete with failed result`() = launcherResultTest(
        result = ConfirmationDefinition.Result.Failed(
            cause = IllegalStateException("Failed!"),
            message = R.string.stripe_something_went_wrong.resolvableString,
            type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
        ),
    ) { completeState ->
        val failedResult = completeState.result.assertFailed()

        assertThat(failedResult.cause).isInstanceOf<IllegalStateException>()
        assertThat(failedResult.cause.message).isEqualTo("Failed!")
        assertThat(failedResult.message).isEqualTo(R.string.stripe_something_went_wrong.resolvableString)
        assertThat(failedResult.type).isEqualTo(ConfirmationHandler.Result.Failed.ErrorType.Internal)
    }

    @Test
    fun `On canceled result from launched action, should complete with canceled result`() = launcherResultTest(
        result = ConfirmationDefinition.Result.Canceled(
            action = ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails,
        ),
    ) { completeState ->
        val canceledResult = completeState.result.assertCanceled()

        assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.ModifyPaymentDetails)
    }

    @Test
    fun `On next step result, should move to next definition and complete from after its result`() = test(
        someDefinitionAction = ConfirmationDefinition.Action.Launch(
            launcherArguments = SomeConfirmationDefinition.LauncherArgs,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
        ),
        someDefinitionResult = ConfirmationDefinition.Result.NextStep(
            intent = PAYMENT_INTENT,
            confirmationOption = SomeOtherConfirmationDefinition.Option,
        ),
        someOtherDefinitionAction = ConfirmationDefinition.Action.Launch(
            launcherArguments = SomeOtherConfirmationDefinition.LauncherArgs,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = null,
        ),
        someOtherDefinitionResult = ConfirmationDefinition.Result.Succeeded(
            intent = PAYMENT_INTENT,
            deferredIntentConfirmationType = null,
        ),
    ) {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionConfirmingState()
            assertSomeDefinitionLaunchCalled()
            sendSomeDefinitionLauncherResult()
            assertSomeDefinitionToResultCalled()

            assertSomeOtherDefinitionActionCalled()
            assertSomeOtherDefinitionConfirmingState()
            assertSomeOtherDefinitionLaunchCalled()
            sendSomeOtherDefinitionLauncherResult()
            assertSomeOtherDefinitionToResultCalled()

            val completeState = awaitCompleteState()
            val successResult = completeState.result.assertSucceeded()

            assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
            assertThat(successResult.deferredIntentConfirmationType).isNull()

            confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
        }
    }

    @Test
    fun `On return from process death, should wait & complete when result is handled in process`() {
        val dispatcher = StandardTestDispatcher()

        test(
            savedStateHandle = createPrepopulatedSavedStateHandle(receivesResultInProcess = true),
            someDefinitionResult = ConfirmationDefinition.Result.Succeeded(
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
            ),
            dispatcher = dispatcher,
        ) {
            assertThat(confirmationHandler.hasReloadedFromProcessDeath).isTrue()

            confirmationHandler.state.test {
                assertSomeDefinitionConfirmingState()

                dispatcher.scheduler.advanceTimeBy(2.minutes)

                sendSomeDefinitionLauncherResult()
                assertSomeDefinitionToResultCalled()

                val completeState = awaitCompleteState()
                val successResult = completeState.result.assertSucceeded()

                assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
                assertThat(successResult.deferredIntentConfirmationType).isNull()

                confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
            }
        }
    }

    @Test
    fun `On return from process death, should complete if no result when received result not in process`() {
        val dispatcher = StandardTestDispatcher()

        test(
            savedStateHandle = createPrepopulatedSavedStateHandle(receivesResultInProcess = false),
            dispatcher = dispatcher,
        ) {
            assertThat(confirmationHandler.hasReloadedFromProcessDeath).isTrue()

            confirmationHandler.state.test {
                assertSomeDefinitionConfirmingState()

                dispatcher.scheduler.advanceTimeBy(1.seconds)

                expectNoEvents()

                dispatcher.scheduler.advanceTimeBy(1.seconds)

                val completeState = awaitCompleteState()

                val canceledResult = completeState.result.assertCanceled()

                assertThat(canceledResult.action).isEqualTo(ConfirmationHandler.Result.Canceled.Action.None)

                confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
            }
        }
    }

    @Test
    fun `On return from process death, should complete with success result if received in time when not in process`() {
        val dispatcher = StandardTestDispatcher()

        test(
            savedStateHandle = createPrepopulatedSavedStateHandle(receivesResultInProcess = false),
            someDefinitionResult = ConfirmationDefinition.Result.Succeeded(
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
            ),
            dispatcher = dispatcher,
        ) {
            assertThat(confirmationHandler.hasReloadedFromProcessDeath).isTrue()

            confirmationHandler.state.test {
                assertSomeDefinitionConfirmingState()

                dispatcher.scheduler.advanceTimeBy(1.seconds)

                sendSomeDefinitionLauncherResult()
                assertSomeDefinitionToResultCalled()

                val completeState = awaitCompleteState()
                val successResult = completeState.result.assertSucceeded()

                assertThat(successResult.intent).isEqualTo(PAYMENT_INTENT)
                assertThat(successResult.deferredIntentConfirmationType).isNull()

                confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
            }
        }
    }

    @Test
    fun `On return from process death, should continue to confirm is next step result is received`() {
        val dispatcher = StandardTestDispatcher()

        test(
            savedStateHandle = createPrepopulatedSavedStateHandle(receivesResultInProcess = false),
            someDefinitionResult = ConfirmationDefinition.Result.NextStep(
                intent = PAYMENT_INTENT,
                confirmationOption = SomeOtherConfirmationDefinition.Option,
            ),
            someOtherDefinitionAction = ConfirmationDefinition.Action.Launch(
                launcherArguments = SomeOtherConfirmationDefinition.LauncherArgs,
                receivesResultInProcess = false,
                deferredIntentConfirmationType = null,
            ),
            someOtherDefinitionResult = ConfirmationDefinition.Result.Succeeded(
                intent = UPDATED_PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
            ),
            dispatcher = dispatcher,
        ) {
            assertThat(confirmationHandler.hasReloadedFromProcessDeath).isTrue()

            confirmationHandler.state.test {
                assertSomeDefinitionConfirmingState()

                sendSomeDefinitionLauncherResult()
                assertSomeDefinitionToResultCalled()

                dispatcher.scheduler.advanceUntilIdle()

                assertSomeOtherDefinitionConfirmingState()
                assertSomeOtherDefinitionActionCalled()
                assertSomeOtherDefinitionLaunchCalled()
                sendSomeOtherDefinitionLauncherResult()
                assertSomeOtherDefinitionToResultCalled()

                val completeState = awaitCompleteState()
                val successResult = completeState.result.assertSucceeded()

                assertThat(successResult.intent).isEqualTo(UPDATED_PAYMENT_INTENT)
                assertThat(successResult.deferredIntentConfirmationType).isNull()

                confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
            }
        }
    }

    @Test
    fun `On 'awaitResult', should return null if no confirmation process was started`() = test {
        val result = withTimeout(5.seconds) {
            confirmationHandler.awaitResult()
        }

        assertThat(result).isNull()
    }

    @Test
    fun `On 'awaitResult', should wait until result is received if confirmation process was started`() {
        val dispatcher = StandardTestDispatcher()

        test(
            someDefinitionAction = ConfirmationDefinition.Action.Launch(
                launcherArguments = SomeConfirmationDefinition.LauncherArgs,
                receivesResultInProcess = true,
                deferredIntentConfirmationType = null,
            ),
            someDefinitionResult = ConfirmationDefinition.Result.Succeeded(
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
            ),
            dispatcher = dispatcher,
        ) {
            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            val job = CoroutineScope(dispatcher).launch {
                val result = confirmationHandler.awaitResult().assertSucceeded()

                assertThat(result.intent).isEqualTo(PAYMENT_INTENT)
                assertThat(result.deferredIntentConfirmationType).isNull()
            }

            dispatcher.scheduler.advanceUntilIdle()

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionLaunchCalled()

            dispatcher.scheduler.advanceTimeBy(3.minutes)

            assertThat(job.isActive).isTrue()

            sendSomeDefinitionLauncherResult()

            dispatcher.scheduler.advanceUntilIdle()

            assertSomeDefinitionToResultCalled()

            // Wait for job to complete
            withTimeout(5.seconds) {
                job.join()

                assertThat(job.isCompleted).isTrue()
            }
        }
    }

    private fun launcherResultTest(
        result: ConfirmationDefinition.Result,
        test: (ConfirmationHandler.State.Complete) -> Unit,
    ) = test(
        someDefinitionAction = ConfirmationDefinition.Action.Launch(
            launcherArguments = SomeConfirmationDefinition.LauncherArgs,
            receivesResultInProcess = true,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
        ),
        someDefinitionResult = result,
    ) {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionConfirmingState()
            assertSomeDefinitionLaunchCalled()
            sendSomeDefinitionLauncherResult()
            assertSomeDefinitionToResultCalled()

            val completeState = awaitCompleteState()

            test(completeState)

            confirmationHandler.assertAwaitResultCallReceivesSameResult(completeState)
        }
    }

    private fun persistedParametersTest(receivesResultInProcess: Boolean) = test(
        someDefinitionAction = ConfirmationDefinition.Action.Launch(
            launcherArguments = SomeConfirmationDefinition.LauncherArgs,
            receivesResultInProcess = receivesResultInProcess,
            deferredIntentConfirmationType = DeferredIntentConfirmationType.Client,
        ),
    ) {
        confirmationHandler.state.test {
            assertIdleState()

            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = SomeConfirmationDefinition.Option,
                    intent = PAYMENT_INTENT,
                )
            )

            assertSomeDefinitionActionCalled()
            assertSomeDefinitionConfirmingState()
            assertSomeDefinitionLaunchCalled()

            assertThat(savedStateHandle.contains(AWAITING_CONFIRMATION_RESULT_KEY)).isTrue()
            assertThat(savedStateHandle.contains(SOME_DEFINITION_PERSISTED_KEY)).isTrue()

            val resultData = savedStateHandle.get<DefaultConfirmationHandler.AwaitingConfirmationResultData>(
                AWAITING_CONFIRMATION_RESULT_KEY
            )

            assertThat(resultData?.confirmationOption).isEqualTo(SomeConfirmationDefinition.Option)
            assertThat(resultData?.receivesResultInProcess).isEqualTo(receivesResultInProcess)

            val parameters = savedStateHandle.get<ConfirmationMediator.Parameters<SomeConfirmationDefinition.Option>>(
                SOME_DEFINITION_PERSISTED_KEY
            )

            assertThat(parameters?.intent).isEqualTo(PAYMENT_INTENT)
            assertThat(parameters?.confirmationOption).isEqualTo(SomeConfirmationDefinition.Option)
            assertThat(parameters?.deferredIntentConfirmationType).isEqualTo(DeferredIntentConfirmationType.Client)
        }
    }

    private fun test(
        someDefinitionAction: ConfirmationDefinition.Action<SomeConfirmationDefinition.LauncherArgs> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        someDefinitionResult: ConfirmationDefinition.Result =
            ConfirmationDefinition.Result.Failed(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        someOtherDefinitionAction: ConfirmationDefinition.Action<SomeOtherConfirmationDefinition.LauncherArgs> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        someOtherDefinitionResult: ConfirmationDefinition.Result =
            ConfirmationDefinition.Result.Failed(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                type = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        shouldRegister: Boolean = true,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(),
        scenarioTest: suspend Scenario.() -> Unit
    ) = runTest {
        RecordingConfirmationDefinition.test(
            SomeConfirmationDefinition(
                action = someDefinitionAction,
                result = someDefinitionResult,
            )
        ) {
            val someDefinitionScenario = this

            RecordingConfirmationDefinition.test(
                SomeOtherConfirmationDefinition(
                    action = someOtherDefinitionAction,
                    result = someOtherDefinitionResult,
                )
            ) {
                val someOtherDefinitionScenario = this

                var someDefinitionOnResult: ((SomeConfirmationDefinition.LauncherResult) -> Unit)? = null
                var someOtherDefinitionOnResult: ((SomeOtherConfirmationDefinition.LauncherResult) -> Unit)? = null

                val errorReporter = FakeErrorReporter()
                val confirmationHandler = createDefaultConfirmationHandler(
                    mediators = listOf(
                        ConfirmationMediator(
                            savedStateHandle = savedStateHandle,
                            definition = someDefinitionScenario.definition,
                        ),
                        ConfirmationMediator(
                            savedStateHandle = savedStateHandle,
                            definition = someOtherDefinitionScenario.definition,
                        ),
                    ),
                    errorReporter = errorReporter,
                    dispatcher = dispatcher,
                    savedStateHandle = savedStateHandle,
                ).apply {
                    if (shouldRegister) {
                        val activityResultCaller = DummyActivityResultCaller.noOp()

                        register(
                            activityResultCaller = activityResultCaller,
                            lifecycleOwner = TestLifecycleOwner(),
                        )

                        val someDefinitionCreateLauncherCall = someDefinitionScenario
                            .createLauncherCalls
                            .awaitItem()
                        val someOtherDefinitionCreateLauncherCall = someOtherDefinitionScenario
                            .createLauncherCalls
                            .awaitItem()

                        assertThat(someDefinitionCreateLauncherCall.activityResultCaller)
                            .isEqualTo(activityResultCaller)
                        assertThat(someOtherDefinitionCreateLauncherCall.activityResultCaller)
                            .isEqualTo(activityResultCaller)

                        someDefinitionOnResult = someDefinitionCreateLauncherCall.onResult
                        someOtherDefinitionOnResult = someOtherDefinitionCreateLauncherCall.onResult
                    }
                }

                scenarioTest(
                    Scenario(
                        confirmationHandler = confirmationHandler,
                        someDefinitionScenario = someDefinitionScenario,
                        someDefinitionOnResult = someDefinitionOnResult,
                        someOtherDefinitionScenario = someOtherDefinitionScenario,
                        someOtherDefinitionOnResult = someOtherDefinitionOnResult,
                        savedStateHandle = savedStateHandle,
                        errorReporter = errorReporter,
                    )
                )
            }
        }
    }

    private fun createDefaultConfirmationHandler(
        dispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        mediators: List<ConfirmationMediator<*, *, *, *>> = listOf(
            ConfirmationMediator(
                savedStateHandle = savedStateHandle,
                definition = SomeConfirmationDefinition()
            )
        ),
        errorReporter: ErrorReporter = FakeErrorReporter(),
    ): DefaultConfirmationHandler {
        return DefaultConfirmationHandler(
            mediators = mediators,
            coroutineScope = CoroutineScope(dispatcher),
            errorReporter = errorReporter,
            savedStateHandle = savedStateHandle,
        )
    }

    private fun createPrepopulatedSavedStateHandle(
        receivesResultInProcess: Boolean,
    ) = SavedStateHandle().apply {
        set(
            "AwaitingConfirmationResult",
            DefaultConfirmationHandler.AwaitingConfirmationResultData(
                key = "Some",
                confirmationOption = SomeConfirmationDefinition.Option,
                receivesResultInProcess = receivesResultInProcess,
            ),
        )
        set(
            "SomeParameters",
            ConfirmationMediator.Parameters(
                confirmationOption = SomeConfirmationDefinition.Option,
                intent = PAYMENT_INTENT,
                deferredIntentConfirmationType = null,
            )
        )
    }

    private suspend fun ConfirmationHandler.assertAwaitResultCallReceivesSameResult(
        state: ConfirmationHandler.State.Complete
    ) {
        assertThat(awaitResult()).isEqualTo(state.result)
    }

    private fun Scenario.sendSomeDefinitionLauncherResult() {
        someDefinitionOnResult?.invoke(SomeConfirmationDefinition.LauncherResult)
    }

    private suspend fun Scenario.assertSomeDefinitionActionCalled() {
        /*
         * Should first only check some definition since it is first in the ordered list of mediators
         * then make another option call when fetching action.
         */
        assertThat(someDefinitionScenario.optionCalls.awaitItem().option)
            .isEqualTo(SomeConfirmationDefinition.Option)
        assertThat(someDefinitionScenario.optionCalls.awaitItem().option)
            .isEqualTo(SomeConfirmationDefinition.Option)

        val actionCall = someDefinitionScenario.actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(SomeConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(PAYMENT_INTENT)
    }

    private suspend fun Scenario.assertSomeDefinitionLaunchCalled() {
        val launchCall = someDefinitionScenario.launchCalls.awaitItem()

        assertThat(launchCall.launcher).isEqualTo(SomeConfirmationDefinition.Launcher)
        assertThat(launchCall.confirmationOption).isEqualTo(SomeConfirmationDefinition.Option)
        assertThat(launchCall.arguments).isEqualTo(SomeConfirmationDefinition.LauncherArgs)
        assertThat(launchCall.intent).isEqualTo(PAYMENT_INTENT)
    }

    private suspend fun Scenario.assertSomeDefinitionToResultCalled() {
        val toResultCall = someDefinitionScenario.toResultCalls.awaitItem()

        assertThat(toResultCall.confirmationOption).isEqualTo(SomeConfirmationDefinition.Option)
        assertThat(toResultCall.result).isEqualTo(SomeConfirmationDefinition.LauncherResult)
        assertThat(toResultCall.intent).isEqualTo(PAYMENT_INTENT)
    }

    private fun Scenario.sendSomeOtherDefinitionLauncherResult() {
        someOtherDefinitionOnResult?.invoke(SomeOtherConfirmationDefinition.LauncherResult)
    }

    private suspend fun Scenario.assertSomeOtherDefinitionActionCalled() {
        /*
         * Should first check against both definitions since it is last in the ordered list of mediators
         * then make another option call when fetching action.
         */
        assertThat(someDefinitionScenario.optionCalls.awaitItem().option)
            .isEqualTo(SomeOtherConfirmationDefinition.Option)
        assertThat(someOtherDefinitionScenario.optionCalls.awaitItem().option)
            .isEqualTo(SomeOtherConfirmationDefinition.Option)
        assertThat(someOtherDefinitionScenario.optionCalls.awaitItem().option)
            .isEqualTo(SomeOtherConfirmationDefinition.Option)

        val actionCall = someOtherDefinitionScenario.actionCalls.awaitItem()

        assertThat(actionCall.confirmationOption).isEqualTo(SomeOtherConfirmationDefinition.Option)
        assertThat(actionCall.intent).isEqualTo(PAYMENT_INTENT)
    }

    private suspend fun Scenario.assertSomeOtherDefinitionLaunchCalled() {
        val launchCall = someOtherDefinitionScenario.launchCalls.awaitItem()

        assertThat(launchCall.launcher).isEqualTo(SomeOtherConfirmationDefinition.Launcher)
        assertThat(launchCall.confirmationOption).isEqualTo(SomeOtherConfirmationDefinition.Option)
        assertThat(launchCall.arguments).isEqualTo(SomeOtherConfirmationDefinition.LauncherArgs)
        assertThat(launchCall.intent).isEqualTo(PAYMENT_INTENT)
    }

    private suspend fun Scenario.assertSomeOtherDefinitionToResultCalled() {
        val toResultCall = someOtherDefinitionScenario.toResultCalls.awaitItem()

        assertThat(toResultCall.confirmationOption).isEqualTo(SomeOtherConfirmationDefinition.Option)
        assertThat(toResultCall.result).isEqualTo(SomeOtherConfirmationDefinition.LauncherResult)
        assertThat(toResultCall.intent).isEqualTo(PAYMENT_INTENT)
    }

    private suspend fun TurbineTestContext<ConfirmationHandler.State>.assertIdleState() {
        assertThat(awaitItem()).isEqualTo(ConfirmationHandler.State.Idle)
    }

    private suspend fun TurbineTestContext<ConfirmationHandler.State>.assertSomeDefinitionConfirmingState() {
        val confirmingState = awaitItem().assertConfirming()

        assertThat(confirmingState.option).isEqualTo(SomeConfirmationDefinition.Option)
    }

    private suspend fun TurbineTestContext<ConfirmationHandler.State>.assertSomeOtherDefinitionConfirmingState() {
        val confirmingState = awaitItem().assertConfirming()

        assertThat(confirmingState.option).isEqualTo(SomeOtherConfirmationDefinition.Option)
    }

    private suspend fun TurbineTestContext<ConfirmationHandler.State>.awaitCompleteState():
        ConfirmationHandler.State.Complete {
        return awaitItem().assertComplete()
    }

    private fun ConfirmationHandler.State.assertConfirming(): ConfirmationHandler.State.Confirming {
        assertThat(this).isInstanceOf<ConfirmationHandler.State.Confirming>()

        return this as ConfirmationHandler.State.Confirming
    }

    private fun ConfirmationHandler.State.assertComplete(): ConfirmationHandler.State.Complete {
        assertThat(this).isInstanceOf<ConfirmationHandler.State.Complete>()

        return this as ConfirmationHandler.State.Complete
    }

    private fun ConfirmationHandler.Result?.assertSucceeded(): ConfirmationHandler.Result.Succeeded {
        assertThat(this).isInstanceOf<ConfirmationHandler.Result.Succeeded>()

        return this as ConfirmationHandler.Result.Succeeded
    }

    private fun ConfirmationHandler.Result?.assertFailed(): ConfirmationHandler.Result.Failed {
        assertThat(this).isInstanceOf<ConfirmationHandler.Result.Failed>()

        return this as ConfirmationHandler.Result.Failed
    }

    private fun ConfirmationHandler.Result?.assertCanceled(): ConfirmationHandler.Result.Canceled {
        assertThat(this).isInstanceOf<ConfirmationHandler.Result.Canceled>()

        return this as ConfirmationHandler.Result.Canceled
    }

    private class Scenario(
        val confirmationHandler: DefaultConfirmationHandler,
        val errorReporter: FakeErrorReporter,
        val savedStateHandle: SavedStateHandle,
        val someDefinitionScenario: RecordingConfirmationDefinition.Scenario<
            SomeConfirmationDefinition.Option,
            SomeConfirmationDefinition.Launcher,
            SomeConfirmationDefinition.LauncherArgs,
            SomeConfirmationDefinition.LauncherResult,
            >,
        val someDefinitionOnResult: ((SomeConfirmationDefinition.LauncherResult) -> Unit)?,
        val someOtherDefinitionScenario: RecordingConfirmationDefinition.Scenario<
            SomeOtherConfirmationDefinition.Option,
            SomeOtherConfirmationDefinition.Launcher,
            SomeOtherConfirmationDefinition.LauncherArgs,
            SomeOtherConfirmationDefinition.LauncherResult,
            >,
        val someOtherDefinitionOnResult: ((SomeOtherConfirmationDefinition.LauncherResult) -> Unit)?,
    )

    private class SomeConfirmationDefinition(
        action: ConfirmationDefinition.Action<LauncherArgs> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        result: ConfirmationDefinition.Result = ConfirmationDefinition.Result.Canceled(
            action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
        ),
    ) : FakeConfirmationDefinition<
        SomeConfirmationDefinition.Option,
        SomeConfirmationDefinition.Launcher,
        SomeConfirmationDefinition.LauncherArgs,
        SomeConfirmationDefinition.LauncherResult,
        >(
        launcher = Launcher,
        action = action,
        result = result,
    ) {
        override val key: String = "Some"

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

    private class SomeOtherConfirmationDefinition(
        action: ConfirmationDefinition.Action<LauncherArgs> =
            ConfirmationDefinition.Action.Fail(
                cause = IllegalStateException("Failed!"),
                message = R.string.stripe_something_went_wrong.resolvableString,
                errorType = ConfirmationHandler.Result.Failed.ErrorType.Internal,
            ),
        result: ConfirmationDefinition.Result = ConfirmationDefinition.Result.Canceled(
            action = ConfirmationHandler.Result.Canceled.Action.InformCancellation,
        ),
    ) : FakeConfirmationDefinition<
        SomeOtherConfirmationDefinition.Option,
        SomeOtherConfirmationDefinition.Launcher,
        SomeOtherConfirmationDefinition.LauncherArgs,
        SomeOtherConfirmationDefinition.LauncherResult,
        >(
        launcher = Launcher,
        action = action,
        result = result,
    ) {
        override val key: String = "SomeOther"

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
    private data object InvalidConfirmationOption : ConfirmationHandler.Option

    private companion object {
        const val SOME_DEFINITION_PERSISTED_KEY = "SomeParameters"
        const val AWAITING_CONFIRMATION_RESULT_KEY = "AwaitingConfirmationResult"

        val PAYMENT_INTENT = PaymentIntentFactory.create()

        val UPDATED_PAYMENT_INTENT = PAYMENT_INTENT.copy(
            paymentMethod = PaymentMethodFactory.card(),
        )
    }
}
