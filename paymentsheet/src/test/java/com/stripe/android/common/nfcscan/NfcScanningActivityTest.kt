package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.Intent
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
import com.stripe.android.common.nfcscan.NfcScanningActivityTestHelpers.configureNfc
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.paymentsheet.R
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.AnimationConstants
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity

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
        moveToState(Lifecycle.State.CREATED)

        waitForActivityFinish()

        assertThat(getResult()).isEqualTo(NfcScanningContract.Result.Canceled)
    }

    @Test
    fun `onResume starts NFC card scanner`() = test {
        waitForIdle()

        assertThat(nfcAdapter?.isInReaderMode).isTrue()
    }

    @Test
    fun `activity returns canceled result when started without arguments`() {
        val intent = Intent(context, NfcScanningActivity::class.java)

        ActivityScenario.launchActivityForResult<NfcScanningActivity>(intent).use { scenario ->
            shadowOf(Looper.getMainLooper()).idle()
            Espresso.onIdle()

            val result = NfcScanningContract.parseResult(
                resultCode = scenario.result.resultCode,
                intent = scenario.result.resultData,
            )

            assertThat(result).isEqualTo(NfcScanningContract.Result.Canceled)
        }
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

    @Test
    fun `successful card scan returns complete result`() = test {
        NfcScanningActivityTestHelpers.completeSuccessfulScan(
            scenario = this,
            responses = NfcScanningActivityTestFixtures.successResponses(),
        )

        waitForActivityFinish()

        assertThat(getResult()).isEqualTo(
            NfcScanningContract.Result.Complete(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )
    }

    @Test
    fun `declined card shows error and keeps activity open`() = test {
        NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
            scenario = this,
            responses = NfcScanningActivityTestFixtures.declinedCardResponses(),
            errorText = context.getString(R.string.stripe_nfc_scan_error_declined_card),
        )

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `unsupported card shows error and keeps activity open`() = test {
        NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
            scenario = this,
            responses = NfcScanningActivityTestFixtures.unsupportedCardResponses(),
            errorText = context.getString(R.string.stripe_nfc_scan_unsupported_card),
        )

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `expired card shows error and keeps activity open`() = test {
        NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
            scenario = this,
            responses = NfcScanningActivityTestFixtures.expiredCardResponses(),
            errorText = context.getString(R.string.stripe_nfc_scan_error_expired_card),
        )

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `mobile wallet shows error and keeps activity open`() = test {
        NfcScanningActivityTestHelpers.assertErrorIsDisplayed(
            scenario = this,
            responses = NfcScanningActivityTestFixtures.mobileWalletResponses(),
            errorText = context.getString(R.string.stripe_nfc_scan_error_mobile_wallet),
        )

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `finish applies fade out transition`() {
        configureNfc(context)

        val intent = NfcScanningContract.createIntent(
            context = context,
            input = NfcScanningContract.Args(
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
            ),
        )
        val controller = Robolectric.buildActivity(NfcScanningActivity::class.java, intent)
            .create()
            .start()
            .resume()

        val activity = controller.get()
        activity.finish()

        val shadowActivity = shadowOf(activity) as ShadowActivity
        assertThat(shadowActivity.pendingTransitionEnterAnimationResourceId)
            .isEqualTo(AnimationConstants.FADE_IN)
        assertThat(shadowActivity.pendingTransitionExitAnimationResourceId)
            .isEqualTo(AnimationConstants.FADE_OUT)
    }

    private fun test(
        block: NfcScanningActivityTestHelpers.Scenario.() -> Unit,
    ) {
        NfcScanningActivityTestHelpers.launchScenario(
            context = context,
            composeRule = composeRule,
            block = block,
        )
    }
}
