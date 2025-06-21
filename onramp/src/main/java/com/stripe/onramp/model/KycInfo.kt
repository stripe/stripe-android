package com.stripe.onramp.model

import android.os.Parcelable
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.parcelize.Parcelize

/**
 * Represents KYC information required for crypto operations.
 */
@Parcelize
data class KycInfo(
    val firstName: String,
    val lastName: String,
    val dateOfBirth: String, // ISO 8601 format (YYYY-MM-DD)
    val address: PaymentSheet.Address,
    val ssn: String? = null, // Optional, for US residents
    val governmentId: GovernmentId? = null
) : Parcelable {

    /**
     * Represents government ID information.
     */
    @Parcelize
    data class GovernmentId(
        val type: Type,
        val number: String,
        val expirationDate: String? = null // ISO 8601 format (YYYY-MM-DD)
    ) : Parcelable {
        enum class Type {
            DRIVERS_LICENSE,
            PASSPORT,
            STATE_ID
        }
    }
}