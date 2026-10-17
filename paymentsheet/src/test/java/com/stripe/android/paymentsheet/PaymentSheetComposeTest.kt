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
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
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
internal class PaymentSheetComposeTest {
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
    fun `sheets sharing an owner have distinct identifiers and callbacks`() = runScenario {
        val firstArgs = present(firstSheet)
        val secondArgs = present(secondSheet)

        assertThat(firstArgs.paymentElementCallbackIdentifier).isNotEqualTo(secondArgs.paymentElementCallbackIdentifier)
        assertThat(firstArgs.initializedViaCompose).isTrue()
        assertThat(secondArgs.initializedViaCompose).isTrue()
        assertThat(PaymentElementCallbackReferences[firstArgs.paymentElementCallbackIdentifier])
            .isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondArgs.paymentElementCallbackIdentifier])
            .isSameInstanceAs(secondCallbacks)
    }

    @Test
    fun `recomposition updates only that sheet's callbacks and preserves identifiers`() = runScenario {
        val firstIdentifier = present(firstSheet).paymentElementCallbackIdentifier
        val secondIdentifier = present(secondSheet).paymentElementCallbackIdentifier
        val replacementCallbacks = createCallbacks("replacement")

        composeRule.runOnIdle {
            firstCallbacks = replacementCallbacks
        }
        composeRule.waitForIdle()

        assertThat(present(firstSheet).paymentElementCallbackIdentifier).isEqualTo(firstIdentifier)
        assertThat(present(secondSheet).paymentElementCallbackIdentifier).isEqualTo(secondIdentifier)
        assertThat(PaymentElementCallbackReferences[firstIdentifier]).isSameInstanceAs(replacementCallbacks)
        assertThat(PaymentElementCallbackReferences[secondIdentifier]).isSameInstanceAs(secondCallbacks)
    }

    @Test
    fun `saved composition state restores each sheet's identifier and registers callbacks`() = runScenario {
        val firstIdentifier = present(firstSheet).paymentElementCallbackIdentifier
        val secondIdentifier = present(secondSheet).paymentElementCallbackIdentifier

        composeRule.runOnIdle {
            PaymentElementCallbackReferences.clear()
            seedUnrelatedCallbacks()
        }
        stateRestorer.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertThat(present(firstSheet).paymentElementCallbackIdentifier).isEqualTo(firstIdentifier)
        assertThat(present(secondSheet).paymentElementCallbackIdentifier).isEqualTo(secondIdentifier)
        assertThat(PaymentElementCallbackReferences[firstIdentifier]).isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondIdentifier]).isSameInstanceAs(secondCallbacks)
    }

    private fun runScenario(block: suspend Scenario.() -> Unit) = runTest {
        seedUnrelatedCallbacks()
        val registry = FakePaymentSheetActivityResultRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
        val scenario = Scenario(registry)
        val paymentResultCallback = PaymentSheetResultCallback { error("Should not be called") }

        stateRestorer.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                val firstSheet = internalRememberPaymentSheet(scenario.firstCallbacks, paymentResultCallback)
                val secondSheet = internalRememberPaymentSheet(scenario.secondCallbacks, paymentResultCallback)

                SideEffect {
                    scenario.firstSheet = firstSheet
                    scenario.secondSheet = secondSheet
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
        private val registry: FakePaymentSheetActivityResultRegistry,
    ) {
        var firstCallbacks by mutableStateOf(createCallbacks("first"))
        val secondCallbacks = createCallbacks("second")
        lateinit var firstSheet: PaymentSheet
        lateinit var secondSheet: PaymentSheet

        suspend fun present(sheet: PaymentSheet): PaymentSheetContract.Args {
            composeRule.runOnIdle {
                sheet.presentWithPaymentIntent("pi_secret")
            }
            return registry.launchCalls.awaitItem()
        }
    }

    internal class FakePaymentSheetActivityResultRegistry : ActivityResultRegistry() {
        val launchCalls = Turbine<PaymentSheetContract.Args>()

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            launchCalls.add(input as PaymentSheetContract.Args)
        }

        fun ensureAllEventsConsumed() {
            launchCalls.ensureAllEventsConsumed()
        }
    }
}
