package com.stripe.android.paymentsheet

import android.os.Build
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.paymentsheet.flowcontroller.DefaultFlowController
import com.stripe.android.paymentsheet.flowcontroller.FlowControllerViewModel
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class FlowControllerComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    private val stateRestorer = StateRestorationTester(composeRule)

    @Test
    fun `controllers sharing an owner have distinct identifiers and callbacks`() = runScenario {
        val firstArgs = present(firstController)
        val secondArgs = present(secondController)

        assertThat(firstArgs.paymentElementCallbackIdentifier).isNotEqualTo(secondArgs.paymentElementCallbackIdentifier)
        assertThat(PaymentElementCallbackReferences[firstArgs.paymentElementCallbackIdentifier])
            .isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondArgs.paymentElementCallbackIdentifier])
            .isSameInstanceAs(secondCallbacks)
    }

    @Test
    fun `recomposition updates only that controller's callbacks and preserves identifiers`() = runScenario {
        val firstIdentifier = present(firstController).paymentElementCallbackIdentifier
        val secondIdentifier = present(secondController).paymentElementCallbackIdentifier
        val replacementCallbacks = createCallbacks("replacement")

        composeRule.runOnIdle {
            firstCallbacks = replacementCallbacks
        }
        composeRule.waitForIdle()

        assertThat(present(firstController).paymentElementCallbackIdentifier).isEqualTo(firstIdentifier)
        assertThat(present(secondController).paymentElementCallbackIdentifier).isEqualTo(secondIdentifier)
        assertThat(PaymentElementCallbackReferences[firstIdentifier]).isSameInstanceAs(replacementCallbacks)
        assertThat(PaymentElementCallbackReferences[secondIdentifier]).isSameInstanceAs(secondCallbacks)
    }

    @Test
    fun `saved composition state restores each controller's identifier and registers callbacks`() = runScenario {
        val firstIdentifier = present(firstController).paymentElementCallbackIdentifier
        val secondIdentifier = present(secondController).paymentElementCallbackIdentifier

        composeRule.runOnIdle {
            PaymentElementCallbackReferences.clear()
            seedUnrelatedCallbacks()
        }
        stateRestorer.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(present(firstController).paymentElementCallbackIdentifier).isEqualTo(firstIdentifier)
        assertThat(present(secondController).paymentElementCallbackIdentifier).isEqualTo(secondIdentifier)
        assertThat(PaymentElementCallbackReferences[firstIdentifier]).isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondIdentifier]).isSameInstanceAs(secondCallbacks)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        PaymentConfiguration.init(ApplicationProvider.getApplicationContext(), ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
        seedUnrelatedCallbacks()
        val registry = FakeFlowControllerActivityResultRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        val scenario = Scenario(registry)
        val paymentResultCallback = PaymentSheetResultCallback { error("Should not be called") }
        val optionResultCallback = PaymentOptionResultCallback { error("Should not be called") }

        stateRestorer.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                val viewModelStore = requireNotNull(LocalViewModelStoreOwner.current).viewModelStore
                val firstController = internalRememberPaymentSheetFlowController(
                    scenario.firstCallbacks, optionResultCallback, paymentResultCallback,
                )
                val secondController = internalRememberPaymentSheetFlowController(
                    scenario.secondCallbacks, optionResultCallback, paymentResultCallback,
                )

                SideEffect {
                    scenario.firstController = firstController
                    scenario.secondController = secondController
                    // Supply configured state so launches exercise the real factory and result registry.
                    viewModelStore.keys().forEach { key ->
                        val viewModel = viewModelStore[key] as? FlowControllerViewModel
                        if (viewModel != null && viewModel.state == null) {
                            viewModel.state = DefaultFlowController.State(
                                paymentSheetState = PaymentSheetFixtures.PAYMENT_OPTIONS_CONTRACT_ARGS.state,
                                config = PaymentSheetFixtures.CONFIG_GOOGLEPAY,
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        scenario.block()

        registry.ensureAllEventsConsumed()
    }

    private fun createCallbacks(name: String): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder()
            .createIntentCallback(CreateIntentCallback { _, _ -> error("Unexpected callback: $name") })
            .build()
    }

    private fun seedUnrelatedCallbacks() {
        // Missing registrations must not pass through the registry's fallback lookup.
        PaymentElementCallbackReferences["unrelated-owner"] = createCallbacks("unrelated")
    }

    private inner class Scenario(
        private val registry: FakeFlowControllerActivityResultRegistry,
    ) {
        var firstCallbacks by mutableStateOf(createCallbacks("first"))
        val secondCallbacks = createCallbacks("second")
        lateinit var firstController: PaymentSheet.FlowController
        lateinit var secondController: PaymentSheet.FlowController

        suspend fun present(controller: PaymentSheet.FlowController): PaymentOptionContract.Args {
            composeRule.runOnIdle {
                controller.presentPaymentOptions()
            }
            return registry.launchCalls.awaitItem()
        }
    }

    internal class FakeFlowControllerActivityResultRegistry : ActivityResultRegistry() {
        val launchCalls = Turbine<PaymentOptionContract.Args>()

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            launchCalls.add(input as PaymentOptionContract.Args)
        }

        fun ensureAllEventsConsumed() {
            launchCalls.ensureAllEventsConsumed()
        }
    }
}
