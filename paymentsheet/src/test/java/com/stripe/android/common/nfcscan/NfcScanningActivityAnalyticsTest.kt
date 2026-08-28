package com.stripe.android.common.nfcscan

import android.content.Context
import android.os.Build
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.core.networking.AnalyticsRequest
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networktesting.AdvancedFraudSignalsTestRule
import com.stripe.android.networktesting.NetworkRule
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

            waitForIdle()
        }
    }

    @Test
    fun `successful card scan fires attempt and success events`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptSucceeded()
        networkRule.expectNfcScanSuccess()

        launchScenario {
            dispatchCardRead(NfcScanningActivityTestFixtures.successResponses())
            waitForCompleteUi()
            isoDep.assertSuccess()
            waitForIdle()
        }
    }

    @Test
    fun `declined card fires attempt failed with error code`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(
            errorCode = "cardDeclinedByNfc",
            errorMatchers = createApduErrorMatchers(
                executedCommands = listOf("selectPpse"),
                sw1 = "69",
                sw2 = "85",
            ),
        )

        launchScenario(autoAdvance = false) {
            dispatchCardRead(NfcScanningActivityTestFixtures.declinedCardResponses())
            assertErrorIsDisplayed(errorText = "Card declined. Try another.")
            isoDep.assertUntilPpseSelectionCommand()
        }
    }

    @Test
    fun `unsupported card fires attempt failed with error code`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(
            errorCode = "cardUnsupportedByNfc",
            errorMatchers = createApduErrorMatchers(
                executedCommands = listOf("selectPpse"),
                sw1 = "6A",
                sw2 = "82",
            ),
        )

        launchScenario(autoAdvance = false) {
            dispatchCardRead(NfcScanningActivityTestFixtures.unsupportedCardResponses())
            assertErrorIsDisplayed(errorText = "Card not supported. Try another.")
            isoDep.assertUntilPpseSelectionCommand()
        }
    }

    @Test
    fun `select application failure includes executed commands in analytics`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(
            errorCode = "cardUnsupportedByNfc",
            errorMatchers = createApduErrorMatchers(
                executedCommands = listOf(
                    "selectPpse",
                    "selectApplication(aid=A0000000031010)",
                ),
                sw1 = "6A",
                sw2 = "82",
            ),
        )

        launchScenario(autoAdvance = false) {
            dispatchCardRead(NfcScanningActivityTestFixtures.selectApplicationFailureResponses())
            assertErrorIsDisplayed(errorText = "Card not supported. Try another.")
            isoDep.assertConnect()
            isoDep.assertCommand(NfcScanningActivityTestFixtures.ApduCommands.SELECT_PPSE)
            isoDep.assertCommand(NfcScanningActivityTestFixtures.ApduCommands.SELECT_VISA_APPLICATION)
            isoDep.assertClose()
        }
    }

    @Test
    fun `expired card fires attempt failed with error code`() {
        networkRule.expectNfcScanStarted()
        networkRule.expectNfcScanAttemptStarted()
        networkRule.expectNfcScanAttemptFailed(
            errorCode = "expiredCard",
        )

        launchScenario(autoAdvance = false) {
            dispatchCardRead(NfcScanningActivityTestFixtures.expiredCardResponses())
            assertErrorIsDisplayed(errorText = "Card expired. Try another.")
            isoDep.assertSuccess()
        }
    }

    private fun launchScenario(
        autoAdvance: Boolean = true,
        block: suspend NfcScanningActivityScenario.() -> Unit,
    ) {
        NfcScanningActivityTestHelpers.launchScenario(
            context = context,
            composeRule = composeRule,
            autoAdvance = autoAdvance,
            block = block,
        )
    }
}
