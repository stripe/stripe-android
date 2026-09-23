package com.stripe.android.paymentelement.callbacks

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.utils.PaymentElementCallbackTestRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test

class PaymentElementCallbackReferencesTest {
    @get:Rule
    val testRule = PaymentElementCallbackTestRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `On get with no callbacks available, should return null`() {
        assertThat(PaymentElementCallbackReferences["Key1"]).isNull()
    }

    @Test
    fun `On get with callbacks assigned to the given key, should return callbacks`() = runScenario {
        val callbacks = createCallbacks()

        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, callbacks)

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(callbacks)
    }

    @Test
    fun `On get with callbacks not assigned to a given key but has callbacks set, should return the first set`() =
        runScenario {
            val initialRegisteredCallbacks = createCallbacks()

            PaymentElementCallbackReferences.register("Key2", owner, initialRegisteredCallbacks)
            PaymentElementCallbackReferences.register("Key3", owner, createCallbacks())
            PaymentElementCallbackReferences.register("Key4", owner, createCallbacks())

            assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(initialRegisteredCallbacks)
        }

    @Test
    fun `Destroying the registering owner removes its callbacks`() = runScenario {
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNotNull()

        owner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `On get after clear for a given key, should return null`() = runScenario {
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())

        PaymentElementCallbackReferences.clear()

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `Destroying a previous owner preserves the replacement callbacks`() = runScenario {
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        val replacementCallbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, otherOwner, replacementCallbacks)

        owner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(replacementCallbacks)
    }

    @Test
    fun `Destroying the replacement owner does not restore a previous registration`() = runScenario {
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        val replacementCallbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, otherOwner, replacementCallbacks)
        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(replacementCallbacks)

        otherOwner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `Registrations with the same callbacks have independent ownership`() = runScenario {
        val callbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, callbacks)
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, otherOwner, callbacks)

        owner.currentState = Lifecycle.State.DESTROYED

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(callbacks)
    }

    @Test
    fun `An active registration can update its callbacks`() = runScenario {
        val registration = PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        val updatedCallbacks = createCallbacks()

        registration.update(updatedCallbacks)

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(updatedCallbacks)
    }

    @Test
    fun `A superseded registration cannot overwrite its replacement`() = runScenario {
        val registration = PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        val replacementCallbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, otherOwner, replacementCallbacks)

        registration.update(createCallbacks())

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(replacementCallbacks)
    }

    @Test
    fun `Registrations on the same owner have independent ownership`() = runScenario {
        val registration = PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        val replacementCallbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, replacementCallbacks)

        registration.update(createCallbacks())

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(replacementCallbacks)
    }

    @Test
    fun `A destroyed registration cannot resurrect its callbacks`() = runScenario {
        val registration = PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        owner.currentState = Lifecycle.State.DESTROYED

        registration.update(createCallbacks())

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `A superseded registration cannot resurrect callbacks after its replacement is destroyed`() = runScenario {
        val registration = PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, otherOwner, createCallbacks())
        otherOwner.currentState = Lifecycle.State.DESTROYED

        registration.update(createCallbacks())

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `An already destroyed owner cannot install callbacks`() = runScenario {
        owner.currentState = Lifecycle.State.DESTROYED

        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, createCallbacks())

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `An already destroyed owner cannot replace an existing registration`() = runScenario {
        val callbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, callbacks)
        otherOwner.currentState = Lifecycle.State.DESTROYED

        val inactiveRegistration = PaymentElementCallbackReferences.register(
            DEFAULT_TEST_KEY,
            otherOwner,
            createCallbacks(),
        )
        inactiveRegistration.update(createCallbacks())

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(callbacks)
    }

    @Test
    fun `Pausing and stopping the owner preserves callbacks`() = runScenario {
        val callbacks = createCallbacks()
        PaymentElementCallbackReferences.register(DEFAULT_TEST_KEY, owner, callbacks)

        owner.currentState = Lifecycle.State.CREATED

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(callbacks)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        val scenario = Scenario(
            owner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED),
            otherOwner = TestLifecycleOwner(initialState = Lifecycle.State.RESUMED),
        )
        try {
            scenario.block()
        } finally {
            scenario.owner.currentState = Lifecycle.State.DESTROYED
            scenario.otherOwner.currentState = Lifecycle.State.DESTROYED
        }
    }

    private class Scenario(
        val owner: TestLifecycleOwner,
        val otherOwner: TestLifecycleOwner,
    )

    private fun createCallbacks(): PaymentElementCallbacks {
        return PaymentElementCallbacks.Builder()
            .createIntentCallback { _, _ ->
                error("Should not be called!")
            }
            .confirmCustomPaymentMethodCallback { _, _ ->
                error("Should not be called!")
            }
            .externalPaymentMethodConfirmHandler { _, _ ->
                error("Should not be called!")
            }
            .build()
    }

    private companion object {
        const val DEFAULT_TEST_KEY = "Key1"
    }
}
