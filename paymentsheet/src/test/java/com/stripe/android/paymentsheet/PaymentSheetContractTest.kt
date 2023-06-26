package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentSheetContractTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    @Suppress("DEPRECATION")
    fun `parseResult() with missing data should return failed result`() {
        assertThat(PaymentSheetContract().parseResult(0, Intent()))
            .isInstanceOf(PaymentSheetResult.Failed::class.java)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `Converts PaymentSheetContractV2 result correctly back to PaymentSheetContract result`() {
        val contract = PaymentSheetContract()
        val args = PaymentSheetContract.Args.createPaymentIntentArgs("pi_123_secret_456")
        val intent = contract.createIntent(context, args)

        val scenario = ActivityScenario.launchActivityForResult<PaymentSheetActivity>(intent)

        scenario.onActivity { activity ->
            activity.setActivityResult(PaymentSheetResult.Canceled)
            activity.finish()
        }

        val result = contract.parseResult(scenario.result.resultCode, scenario.result.resultData)
        assertThat(result).isEqualTo(PaymentSheetResult.Canceled)
    }
}
