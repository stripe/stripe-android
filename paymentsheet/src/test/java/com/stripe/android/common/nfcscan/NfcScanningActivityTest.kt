package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
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

    private fun test(
        block: suspend Scenario.() -> Unit,
    ) {
        shadowOf(context.packageManager)
            .setSystemFeature(PackageManager.FEATURE_NFC, true)

        val intent = NfcScanningContract.createIntent(
            context = context,
            input = NfcScanningContract.Args(merchantName = "River Market"),
        )

        ActivityScenario.launchActivityForResult<NfcScanningActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                runTest {
                    block(
                        Scenario(
                            activity = activity,
                            waitForActivityFinish = {
                                shadowOf(Looper.getMainLooper()).idle()
                                Espresso.onIdle()
                                composeRule.waitForIdle()
                            },
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

    private class Scenario(
        val activity: NfcScanningActivity,
        val waitForActivityFinish: () -> Unit,
        val getResult: () -> NfcScanningContract.Result,
    )
}
