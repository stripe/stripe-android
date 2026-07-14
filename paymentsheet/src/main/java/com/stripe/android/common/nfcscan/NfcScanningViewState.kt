package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.tapzone.TapZone
import com.stripe.android.common.nfcscan.ui.NfcScanningStatus

internal data class NfcScanningViewState(
    val tapZone: TapZone,
    val status: NfcScanningStatus,
)
