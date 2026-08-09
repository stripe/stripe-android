package com.stripe.android.common.nfcscan.ui

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

internal sealed interface NfcScanningStatus {
    val title: ResolvableString
    val subtitle: ResolvableString?

    data object Idle : NfcScanningStatus {
        override val title: ResolvableString =
            R.string.stripe_nfc_scan_hold_card_behind_phone.resolvableString

        override val subtitle: ResolvableString? = null
    }

    data class Error(private val message: ResolvableString) : NfcScanningStatus {
        override val title: ResolvableString =
            R.string.stripe_something_went_wrong.resolvableString

        override val subtitle: ResolvableString = message
    }

    data object Scanning : NfcScanningStatus {
        override val title: ResolvableString =
            R.string.stripe_nfc_scan_reading_card_details.resolvableString

        override val subtitle: ResolvableString? = null
    }

    data object Scanned : NfcScanningStatus {
        override val title: ResolvableString =
            R.string.stripe_nfc_scan_card_details_added.resolvableString

        override val subtitle: ResolvableString? = null
    }
}
