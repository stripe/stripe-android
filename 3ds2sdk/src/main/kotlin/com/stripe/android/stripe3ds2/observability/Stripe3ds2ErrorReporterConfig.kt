package com.stripe.android.stripe3ds2.observability

import android.os.Parcelable
import com.stripe.android.stripe3ds2.transaction.SdkTransactionId
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class Stripe3ds2ErrorReporterConfig(
    private val sdkTransactionId: SdkTransactionId?
) : DefaultErrorReporter.Config, Parcelable {
    override val customTags: Map<String, String> get() =
        sdkTransactionId?.let {
            mapOf("sdk_transaction_id" to it.value)
        }.orEmpty()
}
