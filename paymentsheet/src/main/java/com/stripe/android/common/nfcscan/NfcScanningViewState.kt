package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus

internal sealed interface NfcScanningViewState {
    data object NotSecure : NfcScanningViewState

    data class Ready(
        val tapZone: TapZone,
        val status: NfcScanningStatus,
    ) : NfcScanningViewState
}
