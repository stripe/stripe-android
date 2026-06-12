package com.stripe.android.common.nfcscan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

internal class NfcScanningActivity : AppCompatActivity() {
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
