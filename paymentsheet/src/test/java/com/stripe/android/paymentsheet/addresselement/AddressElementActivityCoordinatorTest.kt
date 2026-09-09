package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityCoordinatorTest {
    @Test
    fun `loading transitions to ready`() {
        val loading = AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        val ready = AddressElementActivityContract.Args.CheckoutShipping.Ready("pk_test_123", null)
        val coordinator = AddressElementActivityCoordinator(loading)

        val handled = coordinator.handleNewIntent(intentFor(ready), isFinishing = false)

        assertThat(handled).isTrue()
        assertThat(coordinator.args).isEqualTo(ready)
    }

    @Test
    fun `loading ignores another loading intent`() {
        val loading = AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        val coordinator = AddressElementActivityCoordinator(loading)

        val handled = coordinator.handleNewIntent(intentFor(loading), isFinishing = false)

        assertThat(handled).isFalse()
        assertThat(coordinator.args).isEqualTo(loading)
    }

    @Test
    fun `ready ignores another ready intent`() {
        val ready = AddressElementActivityContract.Args.CheckoutShipping.Ready("pk_test_123", null)
        val coordinator = AddressElementActivityCoordinator(ready)

        val handled = coordinator.handleNewIntent(intentFor(ready), isFinishing = false)

        assertThat(handled).isFalse()
        assertThat(coordinator.args).isEqualTo(ready)
    }

    @Test
    fun `finishing activity ignores ready intent`() {
        val loading = AddressElementActivityContract.Args.CheckoutShipping.Loading("pk_test_123")
        val ready = AddressElementActivityContract.Args.CheckoutShipping.Ready("pk_test_123", null)
        val coordinator = AddressElementActivityCoordinator(loading)

        val handled = coordinator.handleNewIntent(intentFor(ready), isFinishing = true)

        assertThat(handled).isFalse()
        assertThat(coordinator.args).isEqualTo(loading)
    }

    private fun intentFor(args: AddressElementActivityContract.Args): Intent {
        return Intent(ApplicationProvider.getApplicationContext(), AddressElementActivity::class.java)
            .putExtra(AddressElementActivityContract.EXTRA_ARGS, args)
    }
}
