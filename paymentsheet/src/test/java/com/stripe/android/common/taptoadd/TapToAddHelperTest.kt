package com.stripe.android.common.taptoadd

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.FakeCustomerStateHolder
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.asCallbackFor
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TapToAddHelperTest {

    @Test
    fun `hasPreviouslyAttemptedCollection is initially false`() = runScenario {
        assertThat(helper.hasPreviouslyAttemptedCollection).isFalse()
    }

    @Test
    fun `hasPreviouslyAttemptedCollection is true after startPaymentMethodCollection`() = runScenario {
        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        assertThat(helper.hasPreviouslyAttemptedCollection).isTrue()
    }

    @Test
    fun `hasPreviouslyAttemptedCollection remains true after multiple start calls`() = runScenario {
        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        assertThat(helper.hasPreviouslyAttemptedCollection).isTrue()
    }

    @Test
    fun `register uses activity result caller to register callback which updates result state`() = runScenario {
        helper.register(
            activityResultCaller = activityResultCallerScenario.activityResultCaller,
            lifecycleOwner = TestLifecycleOwner(),
        )

        helper.nextStep.test {
            assertThat(activityResultCallerScenario.awaitNextRegisteredLauncher()).isNotNull()

            val registerCall = activityResultCallerScenario.awaitRegisterCall()

            assertThat(registerCall.contract).isEqualTo(TapToAddContract)

            val tapToAddCallback = registerCall.callback.asCallbackFor<TapToAddResult>()

            tapToAddCallback.onActivityResult(TapToAddResult.Complete)

            testScope.advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(TapToAddNextStep.Complete)
        }
    }

    @Test
    fun `when TapToAddActivity result is canceled with selection, updateSelection is called with new selection`() {
        val expectedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val tapToAddResult = TapToAddResult.Canceled(
            paymentSelection = expectedPaymentSelection,
        )

        var selection: PaymentSelection.Saved? = null
        runScenario(
            updateSelection = { selection = it },
        ) {
            helper.register(
                activityResultCaller = activityResultCallerScenario.activityResultCaller,
                lifecycleOwner = TestLifecycleOwner(),
            )

            finishTapToAddActivityWithResult(
                tapToAddResult,
                helper = helper,
                activityResultCallerScenario = activityResultCallerScenario,
                expectedNextStep = null,
                testScope = testScope,
            )

            assertThat(selection).isEqualTo(expectedPaymentSelection)
        }
    }

    @Test
    fun `when TapToAddActivity result is canceled with selection, payment method is added to customer`() {
        val expectedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val tapToAddResult = TapToAddResult.Canceled(
            paymentSelection = expectedPaymentSelection,
        )
        val customerStateHolder = FakeCustomerStateHolder()

        runScenario(
            customerStateHolder = customerStateHolder,
        ) {
            helper.register(
                activityResultCaller = activityResultCallerScenario.activityResultCaller,
                lifecycleOwner = TestLifecycleOwner(),
            )

            finishTapToAddActivityWithResult(
                tapToAddResult,
                helper = helper,
                activityResultCallerScenario = activityResultCallerScenario,
                expectedNextStep = null,
                testScope = testScope,
            )

            assertThat(customerStateHolder.addPaymentMethodTurbine.awaitItem()).isEqualTo(
                expectedPaymentSelection.paymentMethod
            )
        }
    }

    @Test
    fun `when TapToAddActivity result is canceled with selection and link signup available, next step is correct`() {
        val expectedPaymentSelection = PaymentSelection.Saved(CARD_PAYMENT_METHOD)
        val tapToAddResult = TapToAddResult.Canceled(expectedPaymentSelection)

        runScenario(
            linkSignupMode = LinkSignupMode.InsteadOfSaveForFutureUse,
        ) {
            helper.register(
                activityResultCaller = activityResultCallerScenario.activityResultCaller,
                lifecycleOwner = TestLifecycleOwner(),
            )

            finishTapToAddActivityWithResult(
                tapToAddResult,
                helper = helper,
                activityResultCallerScenario = activityResultCallerScenario,
                expectedNextStep = TapToAddNextStep.ConfirmSavedPaymentMethod(
                    expectedPaymentSelection,
                ),
                testScope = testScope,
            )
        }
    }

    private suspend fun finishTapToAddActivityWithResult(
        result: TapToAddResult,
        helper: TapToAddHelper,
        activityResultCallerScenario: DummyActivityResultCaller.Scenario,
        expectedNextStep: TapToAddNextStep?,
        testScope: TestScope,
    ) {
        helper.nextStep.test {
            assertThat(activityResultCallerScenario.awaitNextRegisteredLauncher()).isNotNull()

            val registerCall = activityResultCallerScenario.awaitRegisterCall()

            assertThat(registerCall.contract).isEqualTo(TapToAddContract)

            val tapToAddCallback = registerCall.callback.asCallbackFor<TapToAddResult>()

            tapToAddCallback.onActivityResult(result)

            testScope.advanceUntilIdle()

            if (expectedNextStep == null) {
                ensureAllEventsConsumed()
            } else {
                assertThat(awaitItem()).isEqualTo(expectedNextStep)
            }
        }
    }

    @Test
    fun `launcher is unregistered & removed when lifecycle is destroyed`() = runScenario {
        val lifecycleOwner = TestLifecycleOwner()

        helper.register(
            activityResultCaller = activityResultCallerScenario.activityResultCaller,
            lifecycleOwner = lifecycleOwner,
        )

        val launcher = activityResultCallerScenario.awaitNextRegisteredLauncher()

        assertThat(activityResultCallerScenario.awaitRegisterCall()).isNotNull()

        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        assertThat(activityResultCallerScenario.awaitNextUnregisteredLauncher()).isEqualTo(launcher)
    }

    @Test
    fun `startPaymentMethodCollection when already collecting does not launch again`() = runScenario {
        helper.register(
            activityResultCaller = activityResultCallerScenario.activityResultCaller,
            lifecycleOwner = TestLifecycleOwner()
        )
        assertThat(activityResultCallerScenario.awaitRegisterCall()).isNotNull()
        assertThat(activityResultCallerScenario.awaitNextRegisteredLauncher()).isNotNull()

        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        assertThat(activityResultCallerScenario.awaitLaunchCall()).isNotNull()
    }

    @Test
    fun `startPaymentMethodCollection can launch again after result is received`() = runScenario {
        helper.register(
            activityResultCaller = activityResultCallerScenario.activityResultCaller,
            lifecycleOwner = TestLifecycleOwner()
        )
        val registerCall = activityResultCallerScenario.awaitRegisterCall()
        assertThat(activityResultCallerScenario.awaitNextRegisteredLauncher()).isNotNull()

        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        assertThat(activityResultCallerScenario.awaitLaunchCall()).isNotNull()

        val tapToAddCallback = registerCall.callback.asCallbackFor<TapToAddResult>()
        tapToAddCallback.onActivityResult(TapToAddResult.Complete)

        helper.startPaymentMethodCollection(DEFAULT_METADATA)
        assertThat(activityResultCallerScenario.awaitLaunchCall()).isNotNull()
    }

    @Test
    fun `startPaymentMethodCollection calls launch with expected params`() = runScenario(
        tapToAddMode = TapToAddMode.Continue,
        eventMode = EventReporter.Mode.Embedded,
        paymentElementCallbackIdentifier = "mpe_callback_id",
        productUsage = setOf("PaymentSheet", "FlowController")
    ) {
        helper.register(
            activityResultCaller = activityResultCallerScenario.activityResultCaller,
            lifecycleOwner = TestLifecycleOwner()
        )

        assertThat(activityResultCallerScenario.awaitRegisterCall()).isNotNull()
        assertThat(activityResultCallerScenario.awaitNextRegisteredLauncher()).isNotNull()

        helper.startPaymentMethodCollection(DEFAULT_METADATA)

        val launchCall = activityResultCallerScenario.awaitLaunchCall()

        assertThat(launchCall).isInstanceOf(TapToAddContract.Args::class.java)

        val tapToAddArgs = launchCall as TapToAddContract.Args

        assertThat(tapToAddArgs.paymentMethodMetadata).isEqualTo(DEFAULT_METADATA)
        assertThat(tapToAddArgs.eventMode).isEqualTo(EventReporter.Mode.Embedded)
        assertThat(tapToAddArgs.paymentElementCallbackIdentifier).isEqualTo("mpe_callback_id")
        assertThat(tapToAddArgs.productUsage).containsExactly("PaymentSheet", "FlowController")
        assertThat(tapToAddArgs.mode).isEqualTo(TapToAddMode.Continue)
    }

    @Test
    fun `hasPreviouslyAttemptedCollection persists across instances via SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()

        FakeTapToAddCollectionHandler.test {
            val helper1 = createTapToAddHelper(savedStateHandle = savedStateHandle)

            assertThat(helper1.hasPreviouslyAttemptedCollection).isFalse()
            helper1.startPaymentMethodCollection(DEFAULT_METADATA)
            assertThat(helper1.hasPreviouslyAttemptedCollection).isTrue()

            val helper2 = createTapToAddHelper(savedStateHandle = savedStateHandle)

            assertThat(helper2.hasPreviouslyAttemptedCollection).isTrue()
        }
    }

    private fun runScenario(
        tapToAddMode: TapToAddMode = TapToAddMode.Complete,
        eventMode: EventReporter.Mode = EventReporter.Mode.Complete,
        paymentElementCallbackIdentifier: String = "callback_id",
        productUsage: Set<String> = emptySet(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        updateSelection: (PaymentSelection.Saved) -> Unit = {},
        customerStateHolder: CustomerStateHolder = FakeCustomerStateHolder(),
        linkSignupMode: LinkSignupMode? = null,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val testScope = this
        DummyActivityResultCaller.test {
            block(
                Scenario(
                    helper = createTapToAddHelper(
                        productUsage = productUsage,
                        paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                        tapToAddMode = tapToAddMode,
                        eventMode = eventMode,
                        savedStateHandle = savedStateHandle,
                        updateSelection = updateSelection,
                        customerStateHolder = customerStateHolder,
                        linkSignupMode = linkSignupMode,
                        testScope = testScope,
                    ),
                    activityResultCallerScenario = this,
                    testScope = testScope,
                )
            )
        }
    }

    private suspend fun createTapToAddHelper(
        tapToAddMode: TapToAddMode = TapToAddMode.Complete,
        eventMode: EventReporter.Mode = EventReporter.Mode.Complete,
        paymentElementCallbackIdentifier: String = "callback_id",
        productUsage: Set<String> = emptySet(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        updateSelection: (PaymentSelection.Saved) -> Unit = {},
        customerStateHolder: CustomerStateHolder = FakeCustomerStateHolder(),
        linkSignupMode: LinkSignupMode? = null,
        testScope: TestScope = TestScope(),
    ): TapToAddHelper {
        return DefaultTapToAddHelper(
            coroutineScope = testScope,
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            tapToAddMode = tapToAddMode,
            eventMode = eventMode,
            savedStateHandle = savedStateHandle,
            updateSelection = updateSelection,
            customerStateHolder = customerStateHolder,
            linkSignupMode = stateFlowOf(linkSignupMode),
        )
    }

    private class Scenario(
        val helper: TapToAddHelper,
        val activityResultCallerScenario: DummyActivityResultCaller.Scenario,
        val testScope: TestScope,
    )

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
    }
}
