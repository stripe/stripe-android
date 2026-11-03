package com.stripe.android.ui.core.cardscan

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.stripecardscan.cardscan.CardScanSheetParams
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardScanStripeLauncherComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `recomposition launches the updated server assignment`() = runTest {
        val compact = mutableStateOf(false)
        val calls = Turbine<CardScanSheetParams>()
        val reporter = FakeCardScanEventsReporter()
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    calls.add(input as CardScanSheetParams)
                }
            }
        }
        lateinit var launcher: CardScanStripeLauncher
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                launcher = CardScanStripeLauncher.rememberCardScanStripeLauncher(
                    eventsReporter = reporter,
                    apiConfiguration = ApiConfiguration.State("pk_test_123", null),
                    useCompactSsdOcrCardScan = compact.value,
                    elementsSessionId = if (compact.value) "es_compact" else "es_original",
                    onResult = {},
                )
            }
        }
        composeRule.runOnIdle { compact.value = true }
        composeRule.runOnIdle { launcher.launch(ApplicationProvider.getApplicationContext()) }

        val configuration = calls.awaitItem().cardScanConfiguration
        assertThat(configuration.useCompactSsdOcr).isTrue()
        assertThat(configuration.elementsSessionId).isEqualTo("es_compact")
        assertThat(reporter.scanStartedCalls.awaitItem().implementation).isEqualTo("stripe_card_scan")
        calls.ensureAllEventsConsumed()
        reporter.validate()
    }
}
