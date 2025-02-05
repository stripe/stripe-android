package com.stripe.android.connect.webview.serialization

import com.stripe.android.core.StripeError
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Stripe.js StripeError object.
 */
@Serializable
internal data class StripeErrorJs(
    val type: String?,
    val message: String?,
    val code: String?,
    val param: String?,
    @SerialName("decline_code") val declineCode: String?,
    val charge: String?,
    @SerialName("doc_url") val docUrl: String?,
) {
    companion object {
        fun from(error: StripeError): StripeErrorJs {
            return StripeErrorJs(
                type = error.type,
                message = error.message,
                code = error.code,
                param = error.param,
                declineCode = error.declineCode,
                charge = error.charge,
                docUrl = error.docUrl
            )
        }
    }
}
