package com.stripe.android.common.nfcscan.ui

import com.stripe.android.core.strings.ResolvableString

internal sealed interface NfcScanningStatus {
    data class Idle(
        val error: ResolvableString?,
    ) : NfcScanningStatus

    data object Scanning : NfcScanningStatus
    data object Scanned : NfcScanningStatus
}
