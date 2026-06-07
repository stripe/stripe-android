package com.stripe.android.common.nfcscan

import android.content.Intent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

internal class NfcScanningActivity : AppCompatActivity() {
    val viewModel by viewModels<NfcScanningViewModel> {
        NfcScanningViewModel.factory()
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
            Intent().putExtras(result.toBundle())
        )
    }
}
