package com.stripe.android.common.nfcscan

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.taptoadd.TAP_TO_BUTTON_UI_TEST_TAG
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.ui.core.elements.ScannedCardDetails
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class NfcScanningActionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `when disabled clicking does not launch contract or invoke callback`() = activityResultTest(
        merchantName = "Cafe Stripe",
        enabled = false,
    ) {
        onLaunchCalls.expectNoEvents()
        onScannedCalls.expectNoEvents()
    }

    @Test
    fun `when enabled click completes forwards scanned card details`() = activityResultTest(
        merchantName = "River Books",
    ) {
        val launch = onLaunchCalls.awaitItem()

        assertThat(launch.contract).isEqualTo(NfcScanningContract)
        assertThat(launch.input).isEqualTo(
            NfcScanningContract.Args(
                merchantName = "River Books"
            )
        )

        val intent = Intent().apply {
            putExtras(
                NfcScanningContract.Result.Complete(
                    cardNumber = "4111111111111111",
                    expirationMonth = 9,
                    expirationYear = 2030,
                ).toBundle(),
            )
        }

        resultDispatcher(launch.requestCode, Activity.RESULT_OK, intent)

        assertThat(onScannedCalls.awaitItem().scannedCardDetails).isEqualTo(
            ScannedCardDetails(
                cardNumber = "4111111111111111",
                expirationYear = 2030,
                expirationMonth = 9,
            ),
        )
    }

    @Test
    fun `when enabled click returns canceled does not invoke callback`() = activityResultTest {
        val launch = onLaunchCalls.awaitItem()
        resultDispatcher(launch.requestCode, Activity.RESULT_CANCELED, null)
        onScannedCalls.expectNoEvents()
    }

    @Test
    fun `when enabled click returns canceled result in intent does not invoke callback`() = activityResultTest {
        val launch = onLaunchCalls.awaitItem()
        val intent = Intent().apply {
            putExtras(NfcScanningContract.Result.Canceled.toBundle())
        }
        resultDispatcher(launch.requestCode, Activity.RESULT_OK, intent)
        onScannedCalls.expectNoEvents()
    }

    private fun activityResultTest(
        merchantName: String = "Cafe Stripe",
        enabled: Boolean = true,
        block: suspend Scenario.() -> Unit,
    ) = runTest {
        val onScannedCardCalls = Turbine<OnScannedCall>()

        val registry = FakeActivityResultRegistry()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }

        val onLaunchCalls = registry.onLaunchCalls

        composeTestRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                NfcScanningAction(merchantName = merchantName).Content(
                    enabled = enabled,
                    onScannedCard = { details ->
                        onScannedCardCalls.add(OnScannedCall(details))
                    },
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAP_TO_BUTTON_UI_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        block(
            Scenario(
                onLaunchCalls = onLaunchCalls,
                onScannedCalls = onScannedCardCalls,
                resultDispatcher = registry::dispatchResult,
            ),
        )

        onScannedCardCalls.ensureAllEventsConsumed()
        onLaunchCalls.ensureAllEventsConsumed()
    }

    private class Scenario(
        val onLaunchCalls: ReceiveTurbine<FakeActivityResultRegistry.OnLaunchCall<*, *>>,
        val onScannedCalls: ReceiveTurbine<OnScannedCall>,
        val resultDispatcher: (requestCode: Int, resultCode: Int, data: Intent?) -> Unit,
    )

    private class OnScannedCall(
        val scannedCardDetails: ScannedCardDetails,
    )

    private class FakeActivityResultRegistry : ActivityResultRegistry() {
        val onLaunchCalls = Turbine<OnLaunchCall<*, *>>()

        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            onLaunchCalls.add(
                OnLaunchCall(
                    requestCode = requestCode,
                    contract = contract,
                    input = input,
                ),
            )
        }

        class OnLaunchCall<I, O>(
            val requestCode: Int,
            val contract: ActivityResultContract<I, O>,
            val input: I,
        )
    }
}
