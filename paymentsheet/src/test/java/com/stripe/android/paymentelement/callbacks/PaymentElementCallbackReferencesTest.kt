package com.stripe.android.paymentelement.callbacks

import com.google.common.truth.Truth.assertThat
import com.stripe.android.utils.PaymentElementCallbackTestRule
import org.junit.Rule
import org.junit.Test

class PaymentElementCallbackReferencesTest {
    @get:Rule
    val testRule = PaymentElementCallbackTestRule()

    @Test
    fun `On get with no callbacks available, should return null`() {
        assertThat(PaymentElementCallbackReferences["Key1"]).isNull()
    }

    @Test
    fun `On get with callbacks assigned to the given key, should return callbacks`() {
        val callbacks = createCallbacks()

        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = callbacks

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(callbacks)
    }

    @Test
    fun `On get with callbacks not assigned to a given key but has callbacks set, should return the first set`() {
        val initialRegisteredCallbacks = createCallbacks()

        PaymentElementCallbackReferences["Key2"] = initialRegisteredCallbacks
        PaymentElementCallbackReferences["Key3"] = createCallbacks()
        PaymentElementCallbackReferences["Key4"] = createCallbacks()

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(initialRegisteredCallbacks)
    }

    @Test
    fun `On get after remove for a given key, should return null`() {
        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = createCallbacks()

        PaymentElementCallbackReferences.remove(DEFAULT_TEST_KEY)

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `On get after clear for a given key, should return null`() {
        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = createCallbacks()

        PaymentElementCallbackReferences.clear()

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `registering shipping updater preserves callbacks under the same key`() {
        val callbacks = createCallbacks()
        val updater: ShippingAddressUpdater = { Result.success(Unit) }
        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = callbacks

        PaymentElementCallbackReferences.registerShippingAddressUpdater(
            key = DEFAULT_TEST_KEY,
            updater = updater,
        )

        val registeredCallbacks = requireNotNull(
            PaymentElementCallbackReferences[DEFAULT_TEST_KEY]
        )
        assertThat(registeredCallbacks.createIntentCallback)
            .isSameInstanceAs(callbacks.createIntentCallback)
        assertThat(registeredCallbacks.confirmCustomPaymentMethodCallback)
            .isSameInstanceAs(callbacks.confirmCustomPaymentMethodCallback)
        assertThat(registeredCallbacks.externalPaymentMethodConfirmHandler)
            .isSameInstanceAs(callbacks.externalPaymentMethodConfirmHandler)
        assertThat(registeredCallbacks.shippingAddressUpdater).isSameInstanceAs(updater)

        PaymentElementCallbackReferences.unregisterShippingAddressUpdater(
            key = DEFAULT_TEST_KEY,
            updater = updater,
        )

        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isEqualTo(callbacks)
    }

    @Test
    fun `setting and removing callbacks preserves registered shipping updater`() {
        val updater: ShippingAddressUpdater = { Result.success(Unit) }
        PaymentElementCallbackReferences.registerShippingAddressUpdater(DEFAULT_TEST_KEY, updater)

        PaymentElementCallbackReferences[DEFAULT_TEST_KEY] = createCallbacks()
        PaymentElementCallbackReferences.remove(DEFAULT_TEST_KEY)

        assertThat(PaymentElementCallbackReferences.getShippingAddressUpdater(DEFAULT_TEST_KEY))
            .isSameInstanceAs(updater)
        PaymentElementCallbackReferences.unregisterShippingAddressUpdater(DEFAULT_TEST_KEY, updater)
        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isNull()
    }

    @Test
    fun `shipping updater lookup does not use unrelated callback fallback`() {
        val updater: ShippingAddressUpdater = { Result.success(Unit) }
        val callbacks = createCallbacks()
        PaymentElementCallbackReferences.registerShippingAddressUpdater(
            key = "Key2",
            updater = updater,
        )
        PaymentElementCallbackReferences["Key3"] = callbacks

        assertThat(PaymentElementCallbackReferences["Key2"]).isSameInstanceAs(callbacks)
        assertThat(PaymentElementCallbackReferences[DEFAULT_TEST_KEY]).isSameInstanceAs(callbacks)
        assertThat(PaymentElementCallbackReferences.getShippingAddressUpdater(DEFAULT_TEST_KEY))
            .isNull()
    }

    @Test
    fun `stale owner cannot unregister rebound shipping updater`() {
        val originalUpdater: ShippingAddressUpdater = { Result.success(Unit) }
        val reboundUpdater: ShippingAddressUpdater = { Result.success(Unit) }
        PaymentElementCallbackReferences.registerShippingAddressUpdater(DEFAULT_TEST_KEY, originalUpdater)
        PaymentElementCallbackReferences.registerShippingAddressUpdater(DEFAULT_TEST_KEY, reboundUpdater)

        PaymentElementCallbackReferences.unregisterShippingAddressUpdater(DEFAULT_TEST_KEY, originalUpdater)

        assertThat(PaymentElementCallbackReferences.getShippingAddressUpdater(DEFAULT_TEST_KEY))
            .isSameInstanceAs(reboundUpdater)
    }

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
