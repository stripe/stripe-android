package com.stripe.android.paymentelement.callbacks

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

internal class LifecyclePaymentElementCallbackReferencesTest {
    @get:Rule
    val callbackRule = PaymentElementCallbackTestRule()

    @get:Rule
    val coroutineRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `Callbacks are available by their original lookup key`() = runScenario {
        val callbacks = createCallbacks("first")

        firstReferences[KEY] = callbacks

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isSameInstanceAs(callbacks)
    }

    @Test
    fun `Setting the same key updates the scope's callbacks`() = runScenario {
        firstReferences[KEY] = createCallbacks("initial")
        val updatedCallbacks = createCallbacks("updated")

        firstReferences[KEY] = updatedCallbacks

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isSameInstanceAs(updatedCallbacks)
    }

    @Test
    fun `Different lookup keys resolve their own callbacks`() = runScenario {
        val firstCallbacks = createCallbacks("first")
        val secondCallbacks = createCallbacks("second")

        firstReferences[KEY] = firstCallbacks
        secondReferences[OTHER_KEY] = secondCallbacks

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondReferences.key(OTHER_KEY)]).isSameInstanceAs(secondCallbacks)
    }

    @Test
    fun `Destroying a lifecycle removes all its references`() = runScenario {
        firstReferences[KEY] = createCallbacks("first")
        firstReferences[OTHER_KEY] = createCallbacks("second")
        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isNotNull()
        assertThat(PaymentElementCallbackReferences[firstReferences.key(OTHER_KEY)]).isNotNull()

        firstOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isNull()
        assertThat(PaymentElementCallbackReferences[firstReferences.key(OTHER_KEY)]).isNull()
    }

    @Test
    fun `Destroying an earlier lifecycle preserves the newer scope`() = runScenario {
        firstReferences[KEY] = createCallbacks("first")
        val newerCallbacks = createCallbacks("second")
        secondReferences[KEY] = newerCallbacks

        firstOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[secondReferences.key(KEY)]).isSameInstanceAs(newerCallbacks)
    }

    @Test
    fun `Scopes using the same callback object still have independent references`() = runScenario {
        val callbacks = createCallbacks("shared")
        firstReferences[KEY] = callbacks
        secondReferences[KEY] = callbacks

        firstOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[secondReferences.key(KEY)]).isSameInstanceAs(callbacks)
    }

    @Test
    fun `Updating an older scope does not override the newer scope`() = runScenario {
        firstReferences[KEY] = createCallbacks("first")
        val newerCallbacks = createCallbacks("second")
        secondReferences[KEY] = newerCallbacks

        firstReferences[KEY] = createCallbacks("updated first")

        assertThat(PaymentElementCallbackReferences[secondReferences.key(KEY)]).isSameInstanceAs(newerCallbacks)
    }

    @Test
    fun `Destroying one scope removes only its exact key`() = runScenario {
        val firstCallbacks = createCallbacks("first")
        val secondCallbacks = createCallbacks("second")
        firstReferences[KEY] = firstCallbacks
        secondReferences[KEY] = secondCallbacks
        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondReferences.key(KEY)]).isSameInstanceAs(secondCallbacks)

        secondOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isSameInstanceAs(firstCallbacks)
        assertThat(PaymentElementCallbackReferences[secondReferences.key(KEY)]).isNull()
    }

    @Test
    fun `Multiple wrappers on one lifecycle have separate references`() = runScenario {
        firstReferences[KEY] = createCallbacks("first")
        val sameLifecycleReferences = LifecyclePaymentElementCallbackReferences(firstOwner.lifecycle)
        val newerCallbacks = createCallbacks("second")
        sameLifecycleReferences[KEY] = newerCallbacks

        firstReferences[KEY] = createCallbacks("updated first")

        assertThat(PaymentElementCallbackReferences[sameLifecycleReferences.key(KEY)]).isSameInstanceAs(newerCallbacks)
    }

    @Test
    fun `A destroyed scope cannot restore its callbacks`() = runScenario {
        firstReferences[KEY] = createCallbacks("initial")
        firstOwner.currentState = Lifecycle.State.DESTROYED

        firstReferences[KEY] = createCallbacks("late update")

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isNull()
    }

    @Test
    fun `An already destroyed lifecycle cannot install callbacks`() = runScenario {
        firstOwner.currentState = Lifecycle.State.DESTROYED
        val references = LifecyclePaymentElementCallbackReferences(firstOwner.lifecycle)

        references[KEY] = createCallbacks("late registration")

        assertThat(PaymentElementCallbackReferences[references.key(KEY)]).isNull()
    }

    @Test
    fun `Destroying a scope without references preserves other callbacks`() = runScenario {
        val callbacks = createCallbacks("second")
        secondReferences[KEY] = callbacks

        firstOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[secondReferences.key(KEY)]).isSameInstanceAs(callbacks)
    }

    @Test
    fun `Stopping the lifecycle preserves its callbacks`() = runScenario {
        val callbacks = createCallbacks("first")
        firstReferences[KEY] = callbacks

        firstOwner.currentState = Lifecycle.State.CREATED

        assertThat(PaymentElementCallbackReferences[firstReferences.key(KEY)]).isSameInstanceAs(callbacks)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val firstOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        val secondOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED)
        try {
            Scenario(
                firstOwner = firstOwner,
                secondOwner = secondOwner,
                firstReferences = LifecyclePaymentElementCallbackReferences(firstOwner.lifecycle),
                secondReferences = LifecyclePaymentElementCallbackReferences(secondOwner.lifecycle),
            ).block()
        } finally {
            firstOwner.currentState = Lifecycle.State.DESTROYED
            secondOwner.currentState = Lifecycle.State.DESTROYED
        }
    }

    private class Scenario(
        val firstOwner: TestLifecycleOwner,
        val secondOwner: TestLifecycleOwner,
        val firstReferences: LifecyclePaymentElementCallbackReferences,
        val secondReferences: LifecyclePaymentElementCallbackReferences,
    )

    private fun createCallbacks(name: String): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ -> error(name) }
            .build()
    }

    private companion object {
        const val KEY = "callbacks"
        const val OTHER_KEY = "other_callbacks"
    }
}
