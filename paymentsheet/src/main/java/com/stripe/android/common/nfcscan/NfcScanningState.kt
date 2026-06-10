package com.stripe.android.common.nfcscan

import com.stripe.android.common.nfcscan.apdu.NfcCardData

internal sealed interface NfcScanningState {
    /** Waiting for the shopper to present a card to the NFC coil. */
    data object Scanning : NfcScanningState

    /** A tag was detected and the card is being read. */
    data object Reading : NfcScanningState

    data class Complete(val cardData: NfcCardData) : NfcScanningState
    data class Failed(val error: Throwable) : NfcScanningState
}
