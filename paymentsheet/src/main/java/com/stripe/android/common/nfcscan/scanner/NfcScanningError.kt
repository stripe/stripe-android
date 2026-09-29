package com.stripe.android.common.nfcscan.scanner

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.paymentsheet.R

internal abstract class NfcScanningError : Throwable() {
    abstract val errorCode: String

    abstract val userMessage: ResolvableString

    open val parameters: Map<String, Any?> = emptyMap()
}

internal data class GenericNfcScanningError(
    override val errorCode: String,
    override val userMessage: ResolvableString =
        R.string.stripe_nfc_try_again_error.resolvableString,
    override val parameters: Map<String, Any?> = emptyMap(),
) : NfcScanningError()
