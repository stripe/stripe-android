package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycInfo(
    val firstName: String,
    val lastName: String,
    @SerialName("date_of_birth")
    val dateOfBirth: String, // ISO 8601 format (YYYY-MM-DD)

    @Serializable(with = PaymentSheetAddressSerializer::class)
    val address: PaymentSheet.Address,
    val ssn: String? = null
)
