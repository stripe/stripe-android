package com.stripe.android.common.nfcscan

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.stripe.android.common.nfcscan.ui.NfcScanningScreen
import com.stripe.android.common.nfcscan.ui.NfcScanningTheme
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

internal class NfcScanningActivity : AppCompatActivity() {
    private val viewModel by viewModels<NfcScanningViewModel> {
        NfcScanningViewModel.factory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            viewModel.result.collectLatest { result ->
                finishWithResult(result)
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

                NfcScanningScreen(
                    state = viewState,
                    viewActionHandler = viewModel::handleViewAction,
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing && !isChangingConfigurations) {
            finishWithResult(NfcScanningContract.Result.Canceled)
        }
    }

    private fun finishWithResult(result: NfcScanningContract.Result) {
        setResult(
            RESULT_OK,
            Intent().putExtras(result.toBundle()),
        )
        finish()
    }
}
