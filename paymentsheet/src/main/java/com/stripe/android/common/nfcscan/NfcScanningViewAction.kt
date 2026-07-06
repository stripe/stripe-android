package com.stripe.android.common.nfcscan

internal sealed interface NfcScanningViewAction {
    data object Close : NfcScanningViewAction
}
