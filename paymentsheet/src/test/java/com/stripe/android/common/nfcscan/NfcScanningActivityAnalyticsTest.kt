package com.stripe.android.common.nfcscan

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.PaymentConfigurationTestRule
import com.stripe.android.testing.createComposeCleanupRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class NfcScanningActivityAnalyticsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val composeRule = createEmptyComposeRule()
    private val composeCleanupRule = createComposeCleanupRule()
    private val networkRule = NetworkRule(
        hostsToTrack = listOf(ApiRequest.API_HOST, AnalyticsRequest.HOST),
        validationTimeout = 5.seconds,
    )

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeCleanupRule)
        .around(composeRule)
        .around(networkRule)
        .around(AdvancedFraudSignalsTestRule())
        .around(PaymentConfigurationTestRule(context))

    @Test
    fun `launching activity fires nfc scan started`() {
        networkRule.expectNfcScanStarted()

        launchScenario {
            waitForIdle()
        }
    }

    @Test
    fun `close button fires nfc scan canceled`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanCanceled()

        launchScenario {
            composeRule.onNodeWithContentDescription("Cancel").performClick()

            waitForActivityFinish()
        }
    }

    @Test
    fun `successful card scan fires attempt and success events`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptSucceeded()
        networkRule.expectNfcScanSuccess()

        launchScenario {
            NfcScanningActivityTestHelpers.completeSuccessfulScan(
                scenario = this,
                responses = NfcScanningActivityTestFixtures.successResponses(),
            )

            waitForActivityFinish()
        }
    }

    @Test
    fun `declined card fires attempt failed with error code`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(errorCode = "cardDeclinedByNfc")

        launchScenario {
            NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
                scenario = this,
                responses = NfcScanningActivityTestFixtures.declinedCardResponses(),
                errorText = context.getString(R.string.stripe_nfc_scan_error_declined_card),
            )
        }
    }

    @Test
    fun `unsupported card fires attempt failed with error code`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(errorCode = "cardUnsupportedByNfc")

        launchScenario {
            NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
                scenario = this,
                responses = NfcScanningActivityTestFixtures.unsupportedCardResponses(),
                errorText = context.getString(R.string.stripe_nfc_scan_unsupported_card),
            )
        }
    }

    @Test
    fun `expired card fires attempt failed with error code`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(errorCode = "expiredCard")

        launchScenario {
            NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
                scenario = this,
                responses = NfcScanningActivityTestFixtures.expiredCardResponses(),
                errorText = context.getString(R.string.stripe_nfc_scan_error_expired_card),
            )
        }
    }

    private fun launchScenario(
        block: NfcScanningActivityTestHelpers.Scenario.() -> Unit,
    ) {
        NfcScanningActivityTestHelpers.launchScenario(
            context = context,
            composeRule = composeRule,
            block = block,
        )
    }
}
