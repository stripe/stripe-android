package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KycInfo(
    @SerialName("date_of_birth")
    val dateOfBirth: String, // ISO 8601 format (YYYY-MM-DD)
    val address: PaymentSheet.Address,
    val ssn: String? = null
)
