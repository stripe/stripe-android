package com.stripe.android.paymentsheet.addresselement

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
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
            assertThat(result).isEqualTo(
                AddressElementActivityContract.Result(AddressLauncherResult.Canceled())
            )
        }
    }

    @Test
    fun `contract result preserves checkout response`() {
        val response = CheckoutSessionResponseFactory.create(customerEmail = "updated@example.com")
        val result = AddressElementActivityContract.Result(
            addressOptionsResult = AddressLauncherResult.Succeeded(AddressDetails()),
            checkoutSessionResponse = response,
        )

        val parsed = AddressElementActivityContract.parseResult(
            resultCode = result.addressOptionsResult.resultCode,
            intent = Intent().putExtras(result.toBundle()),
        )

        assertThat(parsed).isEqualTo(result)
    }
}
