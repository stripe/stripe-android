package com.stripe.android.common.nfcscan

import android.content.Intent
import android.graphics.Color as AndroidColor
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.stripe.android.uicore.StripeColors
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.delay

internal class NfcScanningActivity : AppCompatActivity() {
    private val viewModel by viewModels<NfcScanningViewModel> {
        NfcScanningViewModel.factory()
    }

    private val args: NfcScanningContract.Args? by lazy {
        NfcScanningContract.Args.fromIntent(intent)
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

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
            val state by viewModel.state.collectAsState()
            val shouldSave by viewModel.shouldSave.collectAsState()

            LaunchedEffect(state) {
                if (state is NfcScanningState.Complete) {
                    val complete = state as NfcScanningState.Complete
                    setResult(
                        NfcScanningContract.Result.Complete(
                            cardNumber = complete.cardData.cardNumber,
                            expirationMonth = complete.cardData.expirationMonth,
                            expirationYear = complete.cardData.expirationYear,
                            shouldSave = shouldSave,
                        )
                    )
                    delay(700)
                    finish()
                }
            }

            StripeTheme(
                colors = StripeTheme.getColors(isDark = false),
            ) {
                NfcScanningScreen(
                    state = state,
                    tapZone = viewModel.tapZone,
                    shouldSave = shouldSave,
                    merchantName = args?.merchantName,
                    onClose = {
                        setResult(NfcScanningContract.Result.Canceled)
                        finish()
                    },
                    onAddManually = {
                        setResult(NfcScanningContract.Result.AddManually)
                        finish()
                    },
                    onShouldSaveChanged = viewModel::onShouldSaveChanged,
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
            setResult(NfcScanningContract.Result.Canceled)
            finish()
        }
    }

    private fun setResult(result: NfcScanningContract.Result) {
        setResult(
            RESULT_OK,
            Intent().putExtras(result.toBundle()),
        )
    }
}
