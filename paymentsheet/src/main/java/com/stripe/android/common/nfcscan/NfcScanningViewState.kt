package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus
import com.stripe.android.core.strings.ResolvableString

internal sealed interface NfcScanningViewState {
    data class Available(
        val tapZone: TapZone,
        val status: NfcScanningStatus,
    ) : NfcScanningViewState

    data class Unavailable(
        val message: ResolvableString,
    ) : NfcScanningViewState
}
