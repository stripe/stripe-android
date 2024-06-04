package com.stripe.android.googlepaylauncher

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GooglePayPaymentMethodLauncherContractTest {

    @Test
    fun `Converts to V2 args internally`() {
        val contract = GooglePayPaymentMethodLauncherContract()

        val args = GooglePayPaymentMethodLauncherContract.Args(
            config = GooglePayPaymentMethodLauncher.Config(
                environment = GooglePayEnvironment.Test,
                merchantCountryCode = "CA",
                merchantName = "Till's Shop",
            ),
            currencyCode = "CAD",
            amount = 12345,
        )

        val intent = contract.createIntent(
            context = ApplicationProvider.getApplicationContext(),
            input = args,
        )

        val v2Args = GooglePayPaymentMethodLauncherContractV2.Args.fromIntent(intent)
        assertThat(v2Args).isNotNull()
    }
}
