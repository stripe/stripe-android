package com.stripe.android.googlepaylauncher

import androidx.activity.ComponentActivity
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.stripe.android.googlepaylauncher.utils.runGooglePayPaymentMethodLauncherTest
import com.stripe.android.model.PaymentMethodFixtures.CARD_PAYMENT_METHOD
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class GooglePayPaymentMethodLauncherTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(ComponentActivity::class.java)

    @Test
    fun `present() should successfully return a result when Google Pay is available`() {
        val result = GooglePayPaymentMethodLauncher.Result.Completed(CARD_PAYMENT_METHOD)

        runGooglePayPaymentMethodLauncherTest(
            result = result,
        ) { _, launcher ->
            launcher.present(currencyCode = "usd")
        }
    }

    @Test
    fun `present() should throw IllegalStateException when Google Pay is not available`() {
        runGooglePayPaymentMethodLauncherTest(
            isReady = false,
            expectResult = false,
        ) { _, launcher ->
            assertFailsWith<IllegalStateException> {
                launcher.present(currencyCode = "usd")
            }
        }
    }

    private companion object {
        val CONFIG = GooglePayPaymentMethodLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
