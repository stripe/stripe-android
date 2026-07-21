package com.stripe.android.common.nfcscan

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.nfcscan.ui.HapticFeedbackType
import com.stripe.android.common.nfcscan.ui.NfcScanningScreen
import com.stripe.android.common.nfcscan.ui.NfcScanningTheme
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.fadeOut
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

internal class NfcScanningActivity : AppCompatActivity() {
    private lateinit var args: NfcScanningContract.Args

    private val viewModel by viewModels<NfcScanningViewModel> {
        NfcScanningViewModel.factory { args }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        args = runCatching {
            requireNotNull(NfcScanningContract.Args.fromIntent(intent)) {
                "NfcScanningActivity was started without arguments."
            }
        }.getOrElse {
            finishWithResult(NfcScanningContract.Result.Canceled)
            return
        }

        lifecycleScope.launch {
            viewModel.event.collectLatest { event ->
                when (event) {
                    is NfcScanningEvent.CloseWithResult -> finishWithResult(event.result)
                    is NfcScanningEvent.TriggerHapticFeedback -> triggerHapticFeedback(event.type)
                }
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                scrim = AndroidColor.TRANSPARENT,
                darkScrim = AndroidColor.TRANSPARENT,
            ),
        )

        setContent {
            NfcScanningTheme {
                val viewState by viewModel.viewState.collectAsState()

                LaunchedEffect(Unit) { }

                NfcScanningScreen(
                    state = viewState,
                    viewActionHandler = viewModel::handleViewAction,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.register(this)
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing && !isChangingConfigurations) {
            finishWithResult(NfcScanningContract.Result.Canceled)
        }
    }

    private fun triggerHapticFeedback(type: HapticFeedbackType) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (type) {
                HapticFeedbackType.Success -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                HapticFeedbackType.Failed -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
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

    private fun finishWithResult(result: NfcScanningContract.Result) {
        setResult(
            RESULT_OK,
            Intent().putExtras(result.toBundle()),
        )
        finish()
    }

    override fun finish() {
        super.finish()
        fadeOut()
    }

    private companion object {
        const val VIBRATOR_REPEAT = -1

        val LEGACY_SUCCESS_HAPTIC_VIBRATION = longArrayOf(0, 50)
        val LEGACY_FAILED_HAPTIC_VIBRATION = longArrayOf(0, 80, 100, 80)
    }
}
