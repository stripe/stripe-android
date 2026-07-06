package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNfcAdapter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class NfcScanningActivityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `close button returns canceled result`() = test {
        composeRule.onNodeWithContentDescription("Cancel").performClick()

        waitForActivityFinish()

        assertThat(getResult()).isEqualTo(NfcScanningContract.Result.Canceled)
    }

    @Test
    fun `activity returns canceled result when moved to background`() = test {
        activity.finish()

        waitForActivityFinish()

        assertThat(getResult()).isEqualTo(NfcScanningContract.Result.Canceled)
    }

    @Test
    fun `onResume starts NFC card scanner`() = test {
        waitForIdle()

        assertThat(nfcAdapter?.isInReaderMode).isTrue()
    }

    @Test
    fun `onResume re-registers NFC card scanner when returning from background`() = test {
        waitForIdle()
        assertThat(nfcAdapter?.isInReaderMode).isTrue()

        moveToState(Lifecycle.State.STARTED)
        assertThat(nfcAdapter?.isInReaderMode).isFalse()

        moveToState(Lifecycle.State.RESUMED)
        assertThat(nfcAdapter?.isInReaderMode).isTrue()
    }

    private fun test(
        block: suspend Scenario.() -> Unit,
    ) {
        shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_NFC, true)
        getNfcAdapter()?.setEnabled(true)

        val intent = NfcScanningContract.createIntent(
            context = context,
            input = NfcScanningContract.Args(merchantName = "River Market"),
        )

        ActivityScenario.launchActivityForResult<NfcScanningActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                runTest {
                    val waitForIdle = {
                        shadowOf(Looper.getMainLooper()).idle()
                        Espresso.onIdle()
                        composeRule.waitForIdle()
                    }

                    block(
                        Scenario(
                            activity = activity,
                            nfcAdapter = getNfcAdapter(),
                            moveToState = scenario::moveToState,
                            waitForIdle = waitForIdle,
                            waitForActivityFinish = waitForIdle,
                            getResult = {
                                NfcScanningContract.parseResult(
                                    resultCode = scenario.result.resultCode,
                                    intent = scenario.result.resultData,
                                )
                            },
                        )
                    )
                }
            }
        }
    }

    private fun getNfcAdapter() = NfcAdapter.getDefaultAdapter(context)?.let { shadowOf(it) }

    private class Scenario(
        val activity: NfcScanningActivity,
        val nfcAdapter: ShadowNfcAdapter?,
        val moveToState: (Lifecycle.State) -> Unit,
        val waitForIdle: () -> Unit,
        val waitForActivityFinish: () -> Unit,
        val getResult: () -> NfcScanningContract.Result,
    )
}
