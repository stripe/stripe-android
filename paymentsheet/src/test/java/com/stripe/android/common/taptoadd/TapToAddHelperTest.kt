package com.stripe.android.common.taptoadd

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.testing.TestLifecycleOwner
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.testing.DummyActivityResultCaller
import com.stripe.android.testing.asCallbackFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
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

        helper.result.test {
            assertThat(activityResultCallerScenario.awaitNextRegisteredLauncher()).isNotNull()

            val registerCall = activityResultCallerScenario.awaitRegisterCall()

            assertThat(registerCall.contract).isEqualTo(TapToAddContract)

            val tapToAddCallback = registerCall.callback.asCallbackFor<TapToAddResult>()

            tapToAddCallback.onActivityResult(TapToAddResult.Complete)

            assertThat(awaitItem()).isEqualTo(TapToAddResult.Complete)
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
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        DummyActivityResultCaller.test {
            block(
                Scenario(
                    helper = createTapToAddHelper(
                        productUsage = productUsage,
                        paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
                        tapToAddMode = tapToAddMode,
                        eventMode = eventMode,
                        savedStateHandle = savedStateHandle,
                    ),
                    activityResultCallerScenario = this,
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
    ): TapToAddHelper {
        return DefaultTapToAddHelper(
            coroutineScope = CoroutineScope(currentCoroutineContext()),
            savedStateHandle = savedStateHandle,
            productUsage = productUsage,
            paymentElementCallbackIdentifier = paymentElementCallbackIdentifier,
            tapToAddMode = tapToAddMode,
            eventMode = eventMode,
        )
    }

    private class Scenario(
        val helper: TapToAddHelper,
        val activityResultCallerScenario: DummyActivityResultCaller.Scenario,
    )

    private companion object {
        val DEFAULT_METADATA = PaymentMethodMetadataFactory.create(isTapToAddSupported = true)
    }
}
