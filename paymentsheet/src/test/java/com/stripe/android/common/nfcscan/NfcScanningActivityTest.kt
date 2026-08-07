package com.stripe.android.common.nfcscan

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.testing.LocaleTestRule
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.AnimationConstants
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.shadows.ShadowVibrator
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.use

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class NfcScanningActivityTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val composeRule = createEmptyComposeRule()
    private val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain
        .outerRule(composeCleanupRule)
        .around(composeRule)
        .around(LocaleTestRule(Locale.US))

    @Test
    fun `close button returns canceled result`() = test {
        composeRule.onNodeWithContentDescription("Cancel").performClick()

        waitForIdle()

        assertThat(getResult()).isEqualTo(NfcScanningContract.Result.Canceled)
    }

    @Test
    fun `activity returns canceled result when moved to background`() = test {
        moveToState(Lifecycle.State.CREATED)

        waitForIdle()

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
    fun `successful card scan perform haptic feedback & returns complete result`() = test {
        dispatchCardRead(NfcScanningActivityTestFixtures.successResponses())
        waitForCompleteUi()

        isoDep.assertSuccess()

        assertThat(getShadowVibrator(context).effectId).isEqualTo(VibrationEffect.EFFECT_CLICK)

        waitForIdle()

        assertThat(getResult()).isEqualTo(
            NfcScanningContract.Result.Complete(
                cardNumber = "4242424242424242",
                expirationMonth = 12,
                expirationYear = 2030,
            ),
        )
    }

    @Test
    fun `successful card scan with full PDOL template returns proper values in GPO command`() {
        test(
            paymentMethodMetadata = NfcScanningActivityTestFixtures.paymentMethodMetadataWithPdolData(),
        ) {
            dispatchCardRead(NfcScanningActivityTestFixtures.fullPdolSuccessResponses())
            waitForCompleteUi()

            isoDep.assertSuccess(
                gpoCommand = NfcScanningActivityTestFixtures.ApduCommands.GPO_FULL_PDOL,
            )

            waitForIdle()

            assertThat(getResult()).isEqualTo(
                NfcScanningContract.Result.Complete(
                    cardNumber = "4242424242424242",
                    expirationMonth = 12,
                    expirationYear = 2030,
                ),
            )
        }
    }

    @Test
    fun `declined card shows error, performs haptic feedback, and keeps activity open`() = test(
        autoAdvance = false,
    ) {
        dispatchCardRead(NfcScanningActivityTestFixtures.declinedCardResponses())
        assertErrorIsDisplayed(errorText = "Card declined. Try another card.")
        assertErrorDisappears()

        isoDep.assertUntilPpseSelectionCommand()

        assertThat(getShadowVibrator(context).effectId).isEqualTo(VibrationEffect.EFFECT_HEAVY_CLICK)

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `unsupported card shows error and keeps activity open`() = test(autoAdvance = false) {
        dispatchCardRead(NfcScanningActivityTestFixtures.unsupportedCardResponses())
        assertErrorIsDisplayed(errorText = "Card not supported. Try another card.")
        assertErrorDisappears()

        isoDep.assertUntilPpseSelectionCommand()

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `merchant card brand filter rejects visa and keeps activity open`() {
        test(
            autoAdvance = false,
            paymentMethodMetadata = NfcScanningActivityTestFixtures.paymentMethodMetadataWithVisaDisallowed(),
        ) {
            dispatchCardRead(NfcScanningActivityTestFixtures.successResponses())
            assertErrorIsDisplayed(errorText = "Card not supported. Try another card.")
            assertErrorDisappears()

            isoDep.assertSuccess()

            assertThat(isActivityDestroyed()).isFalse()
        }
    }

    @Test
    fun `expired card shows error and keeps activity open`() = test(autoAdvance = false) {
        dispatchCardRead(NfcScanningActivityTestFixtures.expiredCardResponses())
        assertErrorIsDisplayed(errorText = "Card expired. Try another card.")
        assertErrorDisappears()

        isoDep.assertSuccess()

        assertThat(isActivityDestroyed()).isFalse()
    }

    @Test
    fun `inactivity timeout returns canceled result`() = test {
        ShadowSystemClock.advanceBy(20.seconds.inWholeSeconds, TimeUnit.SECONDS)
        waitForIdle()

        assertThat(getResult()).isEqualTo(NfcScanningContract.Result.Canceled)
    }

    @Test
    fun `finish applies fade out transition`() {
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
        autoAdvance: Boolean = true,
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        block: suspend NfcScanningActivityScenario.() -> Unit,
    ) {
        NfcScanningActivityTestHelpers.launchScenario(
            context = context,
            composeRule = composeRule,
            autoAdvance = autoAdvance,
            paymentMethodMetadata = paymentMethodMetadata,
            block = block,
        )
    }

    private fun getShadowVibrator(context: Context): ShadowVibrator {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        return shadowOf(vibrator)
    }
}
