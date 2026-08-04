package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.ui.HapticFeedbackType

internal sealed interface NfcScanningEvent {
    data class TriggerHapticFeedback(val type: HapticFeedbackType) : NfcScanningEvent
    data class CloseWithResult(val result: NfcScanningContract.Result) : NfcScanningEvent
}
