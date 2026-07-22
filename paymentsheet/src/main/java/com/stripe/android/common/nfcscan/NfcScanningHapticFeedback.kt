package com.stripe.android.common.nfcscan

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.stripe.android.common.nfcscan.ui.HapticFeedbackType

internal object NfcScanningHapticFeedback {
    fun trigger(
        context: Context,
        type: HapticFeedbackType,
    ) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (type) {
                HapticFeedbackType.Success -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticFeedbackType.Failed -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            }

            vibrator.vibrate(effect)
        } else {
            val pattern = when (type) {
                HapticFeedbackType.Success -> LEGACY_SUCCESS_HAPTIC_VIBRATION
                HapticFeedbackType.Failed -> LEGACY_FAILED_HAPTIC_VIBRATION
            }

            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, VIBRATOR_REPEAT)
        }
    }

    private const val VIBRATOR_REPEAT = -1

    private val LEGACY_SUCCESS_HAPTIC_VIBRATION = longArrayOf(0, 50)
    private val LEGACY_FAILED_HAPTIC_VIBRATION = longArrayOf(0, 80, 100, 80)
}
