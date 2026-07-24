package com.stripe.android.common.nfcscan

import android.util.Log

internal object NfcScanLogger {
    fun debug(message: String) {
        runCatching {
            Log.d(TAG, message)
        }
    }

    private const val TAG = "NFC-Scan-Log"
}
