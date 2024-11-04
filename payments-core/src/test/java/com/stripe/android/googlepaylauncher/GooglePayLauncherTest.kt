package com.stripe.android.googlepaylauncher

import androidx.activity.ComponentActivity
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.googlepaylauncher.utils.runGooglePayLauncherTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
internal class GooglePayLauncherTest {

    @get:Rule
    val intentsTestRule = IntentsTestRule(ComponentActivity::class.java)

    @Test
    fun `presentForPaymentIntent() should successfully return a result when Google Pay is available`() {
        runGooglePayLauncherTest { _, launcher ->
            launcher.presentForPaymentIntent("pi_123_secret_456")
        }
    }

    @Test
    fun `presentForPaymentIntent() should throw IllegalStateException when Google Pay is not available`() {
        runGooglePayLauncherTest(
            isReady = false,
            expectResult = false,
        ) { _, launcher ->
            assertFailsWith<IllegalStateException> {
                launcher.presentForPaymentIntent("pi_123")
            }
        }
    }

    @Test
    fun `isJcbEnabled should return expected value for different merchantCountryCode`() {
        assertThat(
            CONFIG.copy(merchantCountryCode = "US").isJcbEnabled
        ).isFalse()

        assertThat(
            CONFIG.copy(merchantCountryCode = "JP").isJcbEnabled
        ).isTrue()

        assertThat(
            CONFIG.copy(merchantCountryCode = "jp").isJcbEnabled
        ).isTrue()
    }

    private companion object {
        val CONFIG = GooglePayLauncher.Config(
            GooglePayEnvironment.Test,
            merchantCountryCode = "US",
            merchantName = "Widget Store"
        )
    }
}
