package com.stripe.android.common.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentelement.callbacks.CallbacksKey
import com.stripe.android.paymentelement.callbacks.LifecyclePaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbacks
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PaymentElementComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @Test
    fun `Callback updates preserve registration ownership and observer count`() = runScenario {
        val initialObserverCount = owner.observerCount
        val updatedCallbacks = createCallbacks("updated")

        composeRule.runOnIdle { callbacks = updatedCallbacks }

        composeRule.runOnIdle {
            assertThat(PaymentElementCallbackReferences[callbackKey]).isSameInstanceAs(updatedCallbacks)
            assertThat(owner.observerCount).isEqualTo(initialObserverCount)
        }
    }

    @Test
    fun `Destroying the lifecycle removes the latest callbacks`() = runScenario {
        val updatedCallbacks = createCallbacks("updated")
        composeRule.runOnIdle { callbacks = updatedCallbacks }

        composeRule.runOnIdle {
            assertThat(PaymentElementCallbackReferences[callbackKey]).isSameInstanceAs(updatedCallbacks)
            owner.currentState = Lifecycle.State.DESTROYED
            assertThat(PaymentElementCallbackReferences[callbackKey]).isNull()
        }
    }

    @Test
    fun `Replacing the lifecycle preserves callbacks when the old lifecycle is destroyed`() = runScenario {
        val previousOwner = owner
        composeRule.runOnIdle { owner = replacementOwner }

        composeRule.runOnIdle {
            previousOwner.currentState = Lifecycle.State.DESTROYED
            assertThat(PaymentElementCallbackReferences[callbackKey]).isSameInstanceAs(callbacks)
        }
    }

    @Test
    fun `The replacement lifecycle owns cleanup for the same callback key`() = runScenario {
        val previousKey = callbackKey
        composeRule.runOnIdle { owner = replacementOwner }
        val replacementCallbacks = createCallbacks("replacement")
        composeRule.runOnIdle { callbacks = replacementCallbacks }

        composeRule.runOnIdle {
            assertThat(callbackKey).isEqualTo(previousKey)
            assertThat(PaymentElementCallbackReferences[callbackKey]).isSameInstanceAs(replacementCallbacks)
            replacementOwner.currentState = Lifecycle.State.DESTROYED
            assertThat(PaymentElementCallbackReferences[callbackKey]).isNull()
        }
    }

    @Test
    fun `Leaving composition retains callbacks until the owner is destroyed`() = runScenario {
        composeRule.runOnIdle { includeCallbacks = false }

        composeRule.runOnIdle {
            assertThat(PaymentElementCallbackReferences[callbackKey]).isSameInstanceAs(callbacks)
            owner.currentState = Lifecycle.State.DESTROYED
            assertThat(PaymentElementCallbackReferences[callbackKey]).isNull()
        }
    }

    @Test
    fun `Recomposition does not change another scope's callbacks`() = runScenario {
        val newerCallbacks = createCallbacks("newer registration")
        val otherReferences = LifecyclePaymentElementCallbackReferences(replacementOwner.lifecycle)
        composeRule.runOnIdle {
            otherReferences[IDENTIFIER] = newerCallbacks
            callbacks = createCallbacks("stale update")
        }

        composeRule.runOnIdle {
            assertThat(PaymentElementCallbackReferences[otherReferences.key(IDENTIFIER)])
                .isSameInstanceAs(newerCallbacks)
        }
    }

    @Test
    fun `Callback changes after destruction cannot resurrect the registration`() = runScenario {
        composeRule.runOnIdle {
            owner.currentState = Lifecycle.State.DESTROYED
            callbacks = createCallbacks("late update")
        }

        composeRule.runOnIdle {
            assertThat(PaymentElementCallbackReferences[callbackKey]).isNull()
        }
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val firstOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        val replacementOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        val scenario = Scenario(firstOwner, replacementOwner, createCallbacks("initial"))
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides scenario.owner) {
                if (scenario.includeCallbacks) {
                    val references = rememberCallbackReferences(IDENTIFIER)
                    UpdateCallbacks(references, IDENTIFIER, scenario.callbacks)
                    SideEffect { scenario.callbackKey = references.key(IDENTIFIER) }
                }
            }
        }
        try {
            composeRule.runOnIdle {
                assertThat(PaymentElementCallbackReferences[scenario.callbackKey]).isSameInstanceAs(scenario.callbacks)
            }
            scenario.block()
        } finally {
            composeRule.runOnIdle {
                firstOwner.currentState = Lifecycle.State.DESTROYED
                replacementOwner.currentState = Lifecycle.State.DESTROYED
            }
        }
    }

    private class Scenario(
        owner: TestLifecycleOwner,
        val replacementOwner: TestLifecycleOwner,
        callbacks: PaymentElementCallbacks,
    ) {
        var owner by mutableStateOf(owner)
        var callbacks by mutableStateOf(callbacks)
        var includeCallbacks by mutableStateOf(true)
        lateinit var callbackKey: CallbacksKey
    }

    private fun createCallbacks(name: String): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ -> error(name) }
            .build()
    }

    private companion object {
        const val IDENTIFIER = "compose_callbacks"
    }
}
