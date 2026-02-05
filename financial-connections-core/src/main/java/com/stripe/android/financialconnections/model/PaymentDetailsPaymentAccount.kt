package com.stripe.android.financialconnections.model

import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a payment account type that references an existing PaymentDetails.
 * Used in IBP (Instant Bank Payments) with NME (manually entered accounts) flow.
 *
 * When this type is received, the SDK should retrieve the existing PaymentDetails
 * matching the [id] instead of creating a new one.
 */
@Serializable
@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class PaymentDetailsPaymentAccount(
    @SerialName(value = "id") @Required
    override val id: String,
) : PaymentAccount() {
    internal companion object {
        const val OBJECT = "financial_connections.payment_details"
    }
}
