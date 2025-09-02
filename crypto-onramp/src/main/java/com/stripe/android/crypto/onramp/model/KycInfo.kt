package com.stripe.android.crypto.onramp.model

import androidx.annotation.RestrictTo
import com.stripe.android.paymentsheet.PaymentSheet
import kotlinx.serialization.Serializable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class KycInfo(
    val firstName: String,
    val lastName: String,
    val idNumber: String?,
    val dateOfBirth: DateOfBirth,
    val address: PaymentSheet.Address
)

@Serializable
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class DateOfBirth(
    val day: Int,
    val month: Int,
    val year: Int
)
