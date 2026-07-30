package com.stripe.android.common.nfcscan

import android.content.Context
import android.os.Build
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.nfcscan.ui.HapticFeedbackType
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class NfcScanningHapticFeedbackTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `success uses legacy vibration pattern on API 28`() {
        val spiedContext = spy(context)

        triggerHapticFeedback(HapticFeedbackType.Success, spiedContext)

        spiedContext.verifyVibratorServiceUsed()

        assertThat(shadowVibrator.pattern).isEqualTo(LEGACY_SUCCESS_HAPTIC_VIBRATION)
        assertThat(shadowVibrator.repeat).isEqualTo(VIBRATOR_REPEAT)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `failure uses legacy vibration pattern on API 28`() {
        val spiedContext = spy(context)

        triggerHapticFeedback(HapticFeedbackType.Failed, spiedContext)

        spiedContext.verifyVibratorServiceUsed()

        assertThat(shadowVibrator.pattern).isEqualTo(LEGACY_FAILED_HAPTIC_VIBRATION)
        assertThat(shadowVibrator.repeat).isEqualTo(VIBRATOR_REPEAT)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `success uses vibrator service click effect on API 29`() {
        val spiedContext = spy(context)

        triggerHapticFeedback(HapticFeedbackType.Success, spiedContext)

        spiedContext.verifyVibratorServiceUsed()

        assertThat(shadowVibrator.effectId).isEqualTo(VibrationEffect.EFFECT_CLICK)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `failure uses vibrator service heavy click effect on API 29`() {
        val spiedContext = spy(context)

        triggerHapticFeedback(HapticFeedbackType.Failed, spiedContext)

        spiedContext.verifyVibratorServiceUsed()

        assertThat(shadowVibrator.effectId).isEqualTo(VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `success uses vibrator manager click effect on API 31`() {
        val vibrator = mock<Vibrator>()
        val spiedContext = contextWithVibratorManager(vibrator)

        triggerHapticFeedback(HapticFeedbackType.Success, spiedContext)

        spiedContext.verifyVibratorManagerServiceUsed()

        verifyVibrationEffect(vibrator, VibrationEffect.EFFECT_CLICK)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `failure uses vibrator manager heavy click effect on API 31`() {
        val vibrator = mock<Vibrator>()
        val spiedContext = contextWithVibratorManager(vibrator)

        triggerHapticFeedback(HapticFeedbackType.Failed, spiedContext)

        spiedContext.verifyVibratorManagerServiceUsed()

        verifyVibrationEffect(vibrator, VibrationEffect.EFFECT_HEAVY_CLICK)
    }

    private fun triggerHapticFeedback(
        type: HapticFeedbackType,
        triggerContext: Context = context,
    ) {
        NfcScanningHapticFeedback.trigger(triggerContext, type)
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun Context.verifyVibratorServiceUsed() {
        @Suppress("DEPRECATION")
        verify(this).getSystemService(Context.VIBRATOR_SERVICE)
        verify(this, never()).getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
    }

    private fun Context.verifyVibratorManagerServiceUsed() {
        @Suppress("DEPRECATION")
        verify(this, never()).getSystemService(Context.VIBRATOR_SERVICE)
        verify(this).getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
    }

    private fun verifyVibrationEffect(
        vibrator: Vibrator,
        expectedEffectId: Int,
    ) {
        val vibrationEffectCaptor = argumentCaptor<VibrationEffect>()

        verify(vibrator).vibrate(vibrationEffectCaptor.capture())

        assertThat(vibrationEffectCaptor.firstValue)
            .isEqualTo(VibrationEffect.createPredefined(expectedEffectId))
    }

    private fun contextWithVibratorManager(vibrator: Vibrator): Context {
        val vibratorManager = mock<VibratorManager> {
            on { defaultVibrator } doReturn vibrator
        }

        return spy(context) {
            on { getSystemService(Context.VIBRATOR_MANAGER_SERVICE) } doReturn vibratorManager
        }
    }

    @Suppress("DEPRECATION")
    private val realVibrator: Vibrator
        get() = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    @Suppress("DEPRECATION")
    private val shadowVibrator
        get() = shadowOf(realVibrator)

    private companion object {
        const val VIBRATOR_REPEAT = -1

        val LEGACY_SUCCESS_HAPTIC_VIBRATION = longArrayOf(0, 50)
        val LEGACY_FAILED_HAPTIC_VIBRATION = longArrayOf(0, 80, 100, 80)
    }
}
