package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.os.Looper
import android.os.Vibrator
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mockStatic
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNfcAdapter
import org.robolectric.shadows.ShadowVibrator

internal object NfcScanningActivityTestHelpers {
    private const val UI_TIMEOUT_MS = 5_000L

    fun launchScenario(
        context: Context,
        composeRule: ComposeTestRule,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        block: Scenario.() -> Unit,
    ) {
        configureNfc(context)

        val intent = NfcScanningContract.createIntent(
            context = context,
            input = NfcScanningContract.Args(
                paymentMethodMetadata = paymentMethodMetadata,
            ),
        )

        ActivityScenario.launchActivityForResult<NfcScanningActivity>(intent).use { scenario ->
            scenario.onActivity {
                block(
                    createScenario(
                        context = context,
                        composeRule = composeRule,
                        activityScenario = scenario,
                        readResult = { readActivityResult(scenario) },
                    ),
                )
            }
        }
    }

    fun assertErrorIsDisplayed(
        scenario: Scenario,
        responses: List<ByteArray>,
        errorText: String,
    ) {
        withDispatchedNfcCard(scenario, responses) {
            scenario.composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
                scenario.waitForIdle()
                scenario.composeRule.onAllNodesWithText(errorText, substring = true)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }

            scenario.composeRule.onNodeWithText(errorText, substring = true).assertExists()
        }
    }

    fun completeSuccessfulScan(
        scenario: Scenario,
        responses: List<ByteArray>,
    ) {
        waitForInitialScanningUi(scenario)

        withDispatchedNfcCard(scenario, responses) {
            waitForScannedState(scenario)
            scenario.waitForIdle()
        }
    }

    private fun createScenario(
        context: Context,
        composeRule: ComposeTestRule,
        activityScenario: ActivityScenario<NfcScanningActivity>,
        readResult: () -> NfcScanningContract.Result,
    ): Scenario {
        val waitForIdle = {
            shadowOf(Looper.getMainLooper()).idle()
            Espresso.onIdle()
            composeRule.waitForIdle()
        }

        return Scenario(
            composeRule = composeRule,
            nfcAdapter = getNfcAdapter(context),
            moveToState = activityScenario::moveToState,
            waitForIdle = waitForIdle,
            waitForActivityFinish = waitForIdle,
            isActivityDestroyed = { activityScenario.state == Lifecycle.State.DESTROYED },
            getResult = readResult,
        )
    }

    private fun readActivityResult(
        activityScenario: ActivityScenario<NfcScanningActivity>,
    ): NfcScanningContract.Result {
        return NfcScanningContract.parseResult(
            resultCode = activityScenario.result.resultCode,
            intent = activityScenario.result.resultData,
        )
    }

    fun Context.getShadowVibrator(): ShadowVibrator {
        @Suppress("DEPRECATION")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        return shadowOf(vibrator)
    }

    fun configureNfc(context: Context) {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_NFC, true)
        getNfcAdapter(context)?.setEnabled(true)
    }

    private fun getNfcAdapter(context: Context) =
        NfcAdapter.getDefaultAdapter(context)?.let { shadowOf(it) }

    private fun waitForInitialScanningUi(scenario: Scenario) {
        scenario.composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            scenario.waitForIdle()
            scenario.composeRule.onAllNodesWithContentDescription("Cancel")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }

    private fun waitForScannedState(scenario: Scenario) {
        scenario.composeRule.waitUntil(timeoutMillis = UI_TIMEOUT_MS) {
            scenario.waitForIdle()
            scenario.composeRule.onAllNodesWithContentDescription("Cancel")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
    }

    private inline fun withDispatchedNfcCard(
        scenario: Scenario,
        responses: List<ByteArray>,
        block: () -> Unit,
    ) {
        val (tag, isoDep) = NfcScanningActivityTestFixtures.createConfiguredIsoDep(responses)

        mockStatic(IsoDep::class.java).use { mockedIsoDep ->
            mockedIsoDep.`when`<IsoDep> { IsoDep.get(any()) }.thenReturn(isoDep)
            scenario.nfcAdapter?.dispatchTagDiscovered(tag)
            scenario.waitForIdle()
            block()
        }
    }

    class Scenario(
        val composeRule: ComposeTestRule,
        val nfcAdapter: ShadowNfcAdapter?,
        val moveToState: (Lifecycle.State) -> Unit,
        val waitForIdle: () -> Unit,
        val waitForActivityFinish: () -> Unit,
        val isActivityDestroyed: () -> Boolean,
        val getResult: () -> NfcScanningContract.Result,
    )
}
