package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

internal class CheckoutShippingAddressUpdaterRegistryTest {
    @Test
    fun `register stores updater under UUID key`() = runTest {
        val updater = FakeUpdater()
        val key = CheckoutShippingAddressUpdaterRegistry.register(updater)

        try {
            assertThat(UUID.fromString(key).toString()).isEqualTo(key)
            assertThat(CheckoutShippingAddressUpdaterRegistry.get(key)).isSameInstanceAs(updater)
        } finally {
            CheckoutShippingAddressUpdaterRegistry.remove(key)
        }
    }

    @Test
    fun `register with existing key rebinds updater`() = runTest {
        val originalUpdater = FakeUpdater()
        val reboundUpdater = FakeUpdater()
        val key = CheckoutShippingAddressUpdaterRegistry.register(originalUpdater)

        try {
            CheckoutShippingAddressUpdaterRegistry.register(key, reboundUpdater)

            assertThat(CheckoutShippingAddressUpdaterRegistry.get(key)).isSameInstanceAs(reboundUpdater)
        } finally {
            CheckoutShippingAddressUpdaterRegistry.remove(key)
        }
    }

    @Test
    fun `busy state is keyed and cleared when registration is removed`() = runTest {
        val key = CheckoutShippingAddressUpdaterRegistry.register(FakeUpdater())

        CheckoutShippingAddressUpdaterRegistry.setBusy(key, true)
        assertThat(CheckoutShippingAddressUpdaterRegistry.isBusy(key)).isTrue()
        assertThat(CheckoutShippingAddressUpdaterRegistry.isBusy(null)).isFalse()

        CheckoutShippingAddressUpdaterRegistry.remove(key)

        assertThat(CheckoutShippingAddressUpdaterRegistry.get(key)).isNull()
        assertThat(CheckoutShippingAddressUpdaterRegistry.isBusy(key)).isFalse()
    }

    private class FakeUpdater : CheckoutShippingAddressUpdaterRegistry.Updater {
        override suspend fun update(address: AddressDetails): Result<Unit> {
            return Result.success(Unit)
        }
    }
}
