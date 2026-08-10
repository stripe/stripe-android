package com.stripe.android.common.nfcscan.ui

import com.stripe.android.core.strings.ResolvableString

internal sealed interface NfcScanningStatus {
    data object Idle : NfcScanningStatus
    data class Error(val message: ResolvableString) : NfcScanningStatus
    data object Scanning : NfcScanningStatus
    data object Scanned : NfcScanningStatus
}
