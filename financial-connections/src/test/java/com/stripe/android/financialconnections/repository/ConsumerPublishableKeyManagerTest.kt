package com.stripe.android.financialconnections.repository

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ConsumerPublishableKeyManagerTest {

    private val consumerPublishableKey = "pk_this_is_not_valid_but_its_ok_for_a_test"

    @Test
    fun `Only sets consumer publishable key tentatively`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val manager = ConsumerPublishableKeyManager(
            savedStateHandle = savedStateHandle,
            isLinkWithStripe = { true },
        )

        manager.setTentativeConsumerPublishableKey(consumerPublishableKey)
        manager.assertIsNotProvidingConsumerPublishableKey()
    }

    @Test
    fun `Provides consumer publishable key after being confirmed`() = runTest {
        val savedStateHandle = SavedStateHandle()

        val manager = ConsumerPublishableKeyManager(
            savedStateHandle = savedStateHandle,
            isLinkWithStripe = { true },
        )

        manager.setTentativeConsumerPublishableKey(consumerPublishableKey)
        manager.assertIsNotProvidingConsumerPublishableKey()

        manager.confirmConsumerPublishableKey()
        manager.assertIsProvidingConsumerPublishableKey()
    }

    @Test
    fun `Does not set consumer publishable key if not in Instant Debits flow`() = runTest {
        val manager = ConsumerPublishableKeyManager(
            savedStateHandle = SavedStateHandle(),
            isLinkWithStripe = { false },
        )

        manager.setTentativeConsumerPublishableKey(consumerPublishableKey)
        manager.confirmConsumerPublishableKey()

        manager.assertIsNotProvidingConsumerPublishableKey()
    }

    @Test
    fun `Sets consumer publishable key if in Instant Debits flow`() = runTest {
        val manager = ConsumerPublishableKeyManager(
            savedStateHandle = SavedStateHandle(),
            isLinkWithStripe = { true },
        )

        manager.setTentativeConsumerPublishableKey(consumerPublishableKey)
        manager.confirmConsumerPublishableKey()

        manager.assertIsProvidingConsumerPublishableKey()
    }

    @Test
    fun `Provides consumer publishable key if SavedStateHandle includes it`() = runTest {
        val manager = ConsumerPublishableKeyManager(
            savedStateHandle = SavedStateHandle(
                initialState = mapOf(
                    KeyConsumerPublishableKey to consumerPublishableKey,
                ),
            ),
            isLinkWithStripe = { true },
        )

        manager.assertIsProvidingConsumerPublishableKey()
    }

    private fun ConsumerPublishableKeyManager.assertIsNotProvidingConsumerPublishableKey() {
        val key = provideConsumerPublishableKey()
        assertThat(key).isNull()
    }

    private fun ConsumerPublishableKeyManager.assertIsProvidingConsumerPublishableKey() {
        val key = provideConsumerPublishableKey()
        assertThat(key).isEqualTo(consumerPublishableKey)
    }
}
