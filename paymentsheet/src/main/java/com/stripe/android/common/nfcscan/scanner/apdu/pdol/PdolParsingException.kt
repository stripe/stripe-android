package com.stripe.android.common.nfcscan.scanner.apdu.pdol

import com.stripe.android.common.nfcscan.scanner.NfcScanningError
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

internal class PdolParsingException(
    override val cause: Throwable,
) : NfcScanningError() {
    override val errorCode: String = "pdolParsingError"
    override val userMessage = R.string.stripe_something_went_wrong.resolvableString
    override val message = "Failed to parse PDOL template"
}
