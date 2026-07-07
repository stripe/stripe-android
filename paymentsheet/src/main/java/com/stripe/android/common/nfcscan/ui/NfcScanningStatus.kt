package com.stripe.android.common.nfcscan.ui

internal sealed interface NfcScanningStatus {
    data object Idle : NfcScanningStatus
    data object Scanning : NfcScanningStatus
    data object Scanned : NfcScanningStatus
}
