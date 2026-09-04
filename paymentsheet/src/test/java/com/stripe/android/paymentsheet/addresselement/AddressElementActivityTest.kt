package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AddressElementActivityTest {
    @Test
    fun `when launched without args should finish with canceled result`() {
        ActivityScenario.launchActivityForResult(
            AddressElementActivity::class.java,
            Bundle.EMPTY
        ).use { activityScenario ->
            assertThat(activityScenario.state).isEqualTo(Lifecycle.State.DESTROYED)
            val result = AddressElementActivityContract.parseResult(0, activityScenario.result.resultData)
            assertThat(result).isEqualTo(AddressElementActivityContract.Result.Canceled)
        }
    }

    @Test
    fun `contract preserves standalone success`() {
        val result = AddressElementActivityContract.Result.StandaloneSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }

    @Test
    fun `contract preserves checkout shipping success`() {
        val result = AddressElementActivityContract.Result.CheckoutShippingSucceeded(AddressDetails())

        val parsed = AddressElementActivityContract.parseResult(
            resultCode = result.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }
}
